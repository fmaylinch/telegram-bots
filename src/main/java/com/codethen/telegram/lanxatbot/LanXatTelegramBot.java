package com.codethen.telegram.lanxatbot;

import com.codethen.telegram.lanxatbot.exception.InlineQueryException;
import com.codethen.telegram.lanxatbot.exception.ProfileNotExistsException;
import com.codethen.telegram.lanxatbot.exception.YandexException;
import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.codethen.yandex.YandexService;
import com.codethen.yandex.model.YandexResponse;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bot for translating messages.
 *
 * It's used via inline queries. You type, for example, `@lanxat_bot en.ru Hello, how are you? .`
 * and it will suggest the translation, that you can click to send. Note that the message
 * ends with a space and a dot ` .`. That's a signal that you finished the message and you want
 * to see the translation (I did it this way to avoid many API requests to Yandex while typing).
 */
public class LanXatTelegramBot extends TelegramLongPollingBot {

    private static final String START_COMMAND = "/start";
    private static final String START_COMMAND_PLUS_SPACE = START_COMMAND + " ";
    /** /start parameter to display information about how inline mode works */
    private static final String HELP_INLINE_START_PARAM = "help-inline";

    private final String botName;
    private final String apiToken;

    /**
     * TODO: Simplify behind an interface that this bot defines,
     *       like we do with {@link UserProfileRepository}
     */
    private final YandexService yandex;

    private final UserProfileRepository userProfileRepo;

    public LanXatTelegramBot(String botName, String apiToken, YandexService yandex, UserProfileRepository userProfileRepo) {
        this.botName = botName;
        this.apiToken = apiToken;
        this.yandex = yandex;
        this.userProfileRepo = userProfileRepo;
    }

    public void onUpdateReceived(Update update) {

        try {
            try {
                if (update.hasInlineQuery()) {
                    try {
                        processInlineQuery(update);
                    } catch (InlineQueryException e) {
                        sendErrorResult(e.getQuery(), e.getMessage());
                    }
                } else {
                    processMessageOrCommand(update);
                }

            } catch (ProfileNotExistsException e) {

                System.out.println("User does not have a profile: " + e.getUserId());
                if (update.hasMessage()) {
                    sendMessage(update.getMessage().getChatId(), "You don't have a profile yet. This bot is in development. Ask the bot creator. Your userId is " + e.getUserId() +".");
                }
            }

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void processInlineQuery(Update update) throws TelegramApiException {

        final InlineQuery inlineQuery = update.getInlineQuery();
        final String query = inlineQuery.getQuery();
        System.out.println("Received inline query from " + getUserInfo(inlineQuery.getFrom()) + ": `" + query + "`");

        if (!endsInPunctuationMark(query)) {
            displayInlineHelpButton(inlineQuery, "How this works?");
            return;
        }

        final UserProfile profile = getCheckedProfileById(inlineQuery.getFrom().getId());

        final Matcher matcher = langsChangePattern.matcher(query);
        if (matcher.matches()) {
            profile.setLangFrom(matcher.group(1));
            profile.setLangTo(matcher.group(2));
            userProfileRepo.saveOrUpdate(profile);
            final String langs = profile.getLangFrom() + "-" + profile.getLangTo(); /** TODO: This is done in {@link TranslationRequest#getLangs()} too. */
            displayInlineHelpButton(inlineQuery, "Changed translation to " + langs + ". Click to know more.");
            return;
        }

        final TranslationRequest request = buildTranslationRequest(query, profile, false);

        try {
            final String translation = requestYandexTranslation(request);

            System.out.println("Translation: '" + translation + "'");

            execute(new AnswerInlineQuery()
                    .setInlineQueryId(inlineQuery.getId())
                    .setCacheTime(0) // TODO: Maybe adjust later as needed
                    .setResults(
                            buildResult(request.langFrom, request.text, "1"),
                            buildResult(request.langTo, translation, "2"),
                            buildResult(request.getLangs(), "- " + translation + "\n" + "- " + request.text, "3")
                    ));

        } catch (YandexException e) {
            throw new InlineQueryException(inlineQuery, e.getMessage(), e);
        }
    }

    private UserProfile getCheckedProfileById(Integer userId) {
        final UserProfile result = userProfileRepo.getProfileById(userId);
        if (result == null) {
            throw new ProfileNotExistsException(userId);
        }
        return result;
    }

    private void displayInlineHelpButton(InlineQuery inlineQuery, String text) throws TelegramApiException {

        execute(new AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.getId())
                .setCacheTime(0)
                .setSwitchPmText(text)
                .setSwitchPmParameter(HELP_INLINE_START_PARAM)
                .setResults());
    }

    private static final String PUNCTUATION_CHARS = ".?!:)";

    private boolean endsInPunctuationMark(String text) {

        if (text.length() == 0) return false;

        final char lastChar = text.charAt(text.length() - 1);
        return PUNCTUATION_CHARS.indexOf(lastChar) >= 0;
    }

    private String requestYandexTranslation(TranslationRequest request) throws YandexException {

        System.out.println("Translating " + request.getLangs() + " : '" + request.text + "'");

        final Call<YandexResponse> call = yandex.translate(request.apiKey, request.text, request.getLangs());

        final Response<YandexResponse> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new YandexException("Unexpected error calling Yandex API", e);
        }

        if (response.code() != 200) {
            throw new YandexException("Unexpected bad response from Yandex API");
        }

        return response.body().text.get(0);
    }

    private String getUserInfo(User user) {
        return user.getUserName() + " (" + user.getId() + ")";
    }

    /**
     * Parses query, with may fit {@link #queryWithLangsPattern} or be just a plain message.
     */
    private TranslationRequest buildTranslationRequest(String query, UserProfile profile, boolean userOtherLangs) {

        final TranslationRequest request = new TranslationRequest();
        request.apiKey = profile.getYandexApiKey();

        final Matcher matcher = queryWithLangsPattern.matcher(query);
        if (matcher.matches()) {
            request.langFrom = matcher.group(1);
            request.langTo = matcher.group(2);
            request.text = matcher.group(3);
        } else {
            request.langFrom = userOtherLangs ? profile.getLangOtherFrom() : profile.getLangFrom();
            request.langTo = userOtherLangs ? profile.getLangOtherTo() : profile.getLangTo();
            request.text = query;
        }

        return request;
    }

    /** In "en-ru" matcher would capture groups: "en", "ru". */
    private static final String langsPatternStr = "([a-z][a-z])-([a-z][a-z])";
    /** In "en-ru message" matcher would capture groups: "en", "ru", "message". */
    private static final Pattern queryWithLangsPattern = Pattern.compile(langsPatternStr + "\\s+(.+)");
    /** In "en-ru." matcher would capture groups: "en", "ru". */
    private static final Pattern langsChangePattern = Pattern.compile(langsPatternStr + "\\.");

    private void processMessageOrCommand(Update update) throws TelegramApiException {

        if (!update.hasMessage()) {
            System.out.println("Received update without message");
            return;
        }

        final Message message = update.getMessage();
        final String userInfo = getUserInfo(message.getFrom());

        if (!message.hasText()) {
            System.out.println("Received message without text from " + userInfo);
            return;
        }

        System.out.println("Message text from " + userInfo + ": " + message.getText());

        if (message.isCommand()) {
            processCommand(update.getMessage());
        } else {
            processMessageAsTranslation(update.getMessage());
        }
    }

    private void processMessageAsTranslation(Message message) throws TelegramApiException {

        final UserProfile profile = getCheckedProfileById(message.getFrom().getId());

        final TranslationRequest request = buildTranslationRequest(message.getText(), profile, true);

        if (request.text == null) {
            profile.setLangOtherFrom(request.langFrom);
            profile.setLangOtherTo(request.langTo);
            userProfileRepo.saveOrUpdate(profile);
            sendMessage(message.getChatId(), "Changed default translation to " + request.getLangs());
            return;
        }

        try {
            final String translation = requestYandexTranslation(request);
            final String msg = "Translated " + request.getLangs() + ":\n" + translation;
            System.out.println("Sent translation " + request.getLangs() + ": '" + translation + "'");
            sendMessage(message.getChatId(), msg);
        } catch (YandexException e) {
            sendMessage(message.getChatId(), "There was an error with Yandex API: " + e.getMessage());
        } catch (TelegramApiException e) {
            sendMessage(message.getChatId(), "There was an error with Telegram API: " + e.getMessage());
        }
    }

    private void sendMessage(Long chatId, String markdownText) throws TelegramApiException {
        execute(new SendMessage()
                .setChatId(chatId)
                .setText(markdownText)
                .setParseMode(ParseMode.MARKDOWN));
    }

    private void processCommand(Message message) throws TelegramApiException {

        final String command = message.getText();

        if (command.equals(START_COMMAND) || command.startsWith(START_COMMAND_PLUS_SPACE)) {

            if (command.equals(START_COMMAND)) {

                final UserProfile profile = getCheckedProfileById(message.getFrom().getId());

                sendMessage(message.getChatId(),
                        "You can write messages here and I'll translate them" +
                                " from language `" + profile.getLangOtherFrom() + "` to language `" + profile.getLangOtherTo() + "`." +
                                " You can change the languages to translate by writing here something like `en-ru`." +
                                " You can also use me in inline mode when talking to other people," +
                                " by typing `@" + getBotUsername() + "` and then the message to translate.");

            } else  {

                String parameter = command.substring((START_COMMAND_PLUS_SPACE).length());

                if (parameter.equals(HELP_INLINE_START_PARAM)) {

                    final UserProfile profile = getCheckedProfileById(message.getFrom().getId());

                    sendMessage(message.getChatId(),
                            "You can send translated messages while talking to other people." +
                                    " In any chat, type `@" + getBotUsername() + "` followed by your message," +
                                    " and finish it with punctuation mark `" + PUNCTUATION_CHARS + "` to see the results." +
                                    " You can then choose to send the original message, the translated message or both." +
                                    " The results will be translated" +
                                    " from language `" + profile.getLangFrom() + "` to language `" + profile.getLangTo() + "`." +
                                    " You can change the languages to translate by entering something like `en-ru.` in the inline message (note the ending dot). " +
                                    " You can also choose the languages for a single message (without affecting the default setting)," +
                                    " by typing something like `@" + getBotUsername() + " en-es Translate this to Spanish.`");

                } else {

                    sendMessage(message.getChatId(),
                            "Sorry, I don't understand the " + START_COMMAND + " parameter `" + parameter + "`.");
                }
            }
        } else {

            sendMessage(message.getChatId(),
                    "Sorry, the command `" + command + "` is not implemented yet");
        }
    }

    private void sendInfoResult(InlineQuery inlineQuery, String message) throws TelegramApiException {
        sendSingleResult(inlineQuery, "Information", message);
    }

    private void sendErrorResult(InlineQuery inlineQuery, String message) throws TelegramApiException {
        sendSingleResult(inlineQuery, "Error", message);
    }

    private void sendSingleResult(InlineQuery inlineQuery, String title, String text) throws TelegramApiException {

        final String id = "1"; // Each result should have a different id

        execute(new AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.getId())
                .setResults(buildResult(title, text, id)));
    }

    private InlineQueryResultArticle buildResult(String title, String text, String id) {
        return new InlineQueryResultArticle()
                .setId(id)
                .setTitle(title)
                .setDescription(text)
                .setInputMessageContent(new InputTextMessageContent()
                        .setParseMode(ParseMode.MARKDOWN)
                        .setMessageText(text));
    }

    public String getBotUsername() {
        return botName;
    }

    public String getBotToken() {
        return apiToken;
    }

    /** TODO: This could be part of {@link YandexService} */
    private static class TranslationRequest {

        public String text;
        public String langFrom;
        public String langTo;
        public String apiKey;

        public String getLangs() {
            return langFrom + "-" + langTo;
        }
    }
}
