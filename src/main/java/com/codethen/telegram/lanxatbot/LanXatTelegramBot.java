package com.codethen.telegram.lanxatbot;

import com.codethen.telegram.lanxatbot.exception.InlineQueryException;
import com.codethen.telegram.lanxatbot.exception.ProfileNotExistsException;
import com.codethen.telegram.lanxatbot.exception.YandexException;
import com.codethen.telegram.lanxatbot.profile.LangConfig;
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
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Bot for translating messages.
 * You can send messages to the bot, but the main feature is to use inline queries.
 * See: https://core.telegram.org/bots/inline
 *
 * In an inline query you type `@lanxat_bot Hello, how are you?`
 * and it will suggest the translation depending on the translation configuration you have currently.
 * You may also explicitly indicate the languages to use for translation in a single message
 * (overriding the default configuration).
 *
 * Note that the bot is currently named lanxat_bot, but this may change. See {@link #getBotUsername()}.
 */
public class LanXatTelegramBot extends TelegramLongPollingBot {

    private enum Command {

        start, langconfig;

        String getText() {
            return "/" + name();
        }
    }

    private enum SpecialLangConfig {

        /** {@link LangConfig} for inline messages */
        inline,
        /** {@link LangConfig} for messages sent to the bot */
        bot
    }

    private static final Map<Command, String> commandText =
            Arrays.stream(Command.values()).collect(Collectors.toMap(x -> x, Command::getText));

    /** {@link Command#start} parameter to display information about how inline mode works */
    private static final String HELP_INLINE_START_PARAM = "help-inline";
    /** If this dot comes after the languages, it means the user wants to change the defaults */

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

        final UserProfile profile = userProfileRepo.getProfileById(inlineQuery.getFrom().getId());

        if (!endsInPunctuationMark(query)) {
            final LangConfig langConfig = profile.getLangConfigs().get(SpecialLangConfig.inline.name());
            displayInlineHelpButton(inlineQuery, "Translating " + langConfig.shortDescription() + ". Click to know more.");
            return;
        }

        final TranslationRequest request = buildTranslationRequest(query, profile, SpecialLangConfig.inline);

        if (request.text == null) return; // No text yet

        try {
            final String translation = requestYandexTranslation(request);

            System.out.println("Translation: '" + translation + "'");

            execute(new AnswerInlineQuery()
                    .setInlineQueryId(inlineQuery.getId())
                    .setCacheTime(0) // TODO: Maybe adjust later as needed
                    .setResults(
                            buildResult(request.langConfig.getFrom(), request.text, "1"),
                            buildResult(request.langConfig.getTo(), translation, "2"),
                            buildResult(request.getLangs(), "- " + translation + "\n" + "- " + request.text, "3")
                    ));

        } catch (YandexException e) {
            throw new InlineQueryException(inlineQuery, e.getMessage(), e);
        }
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
     * Parses query, witch may be {@link #langsAndMessagePattern}, {@link #langConfigAndMessagePattern},
     * or otherwise it's just a plain text with no pattern.
     *
     * If a pattern matches, we decide the {@link LangConfig} from there
     * (either explicit languages or one of the existing {@link UserProfile#getLangConfigs()}).
     *
     * If the query is a plain text, the {@link LangConfig}) is also taken from
     * {@link UserProfile#getLangConfigs()}, according to the given {@link SpecialLangConfig}.
     *
     * Note that {@link TranslationRequest#text} might be null if not present in the query.
     */
    private TranslationRequest buildTranslationRequest(String query, UserProfile profile, SpecialLangConfig config) {

        final TranslationRequest request = new TranslationRequest();
        request.apiKey = profile.getYandexApiKey();

        final Matcher langsAndMessage = langsAndMessagePattern.matcher(query);
        if (langsAndMessage.matches()) {
            request.langConfig = new LangConfig(langsAndMessage.group(1), langsAndMessage.group(2));
            request.text = langsAndMessage.group(3);
        } else {
            final Matcher langConfigAndMessage = langConfigAndMessagePattern.matcher(query);
            if (langConfigAndMessage.matches()) {
                final String langConfigName = langConfigAndMessage.group(1);
                request.langConfig = profile.getLangConfigs().get(langConfigName);
                request.text = langConfigAndMessage.group(2);
            } else {
                request.langConfig = profile.getLangConfigs().get(config.name());
                request.text = query;
            }
        }

        return request;
    }

    /** In ".en.ru" matcher would capture groups: "en", "ru". */
    private static final String langsPatternStr = "\\.([a-z][a-z])\\.([a-z][a-z])";
    /** In ".name" matcher would capture group: "name". */
    private static final String langConfigPatternStr = "\\.(\\w+)";
    /** In "   message" matcher would capture group: "message". */
    private static final String messagePatternStr = "\\s+(.+)";
    /** In ".en.ru message" matcher would capture groups: "en", "ru", "message". */
    private static final Pattern langsAndMessagePattern = Pattern.compile(langsPatternStr + messagePatternStr);
    /** In ".name message" matcher would capture groups: "name", "message". */
    private static final Pattern langConfigAndMessagePattern = Pattern.compile(langConfigPatternStr + messagePatternStr);

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

        final UserProfile profile = userProfileRepo.getProfileById(message.getFrom().getId());

        final TranslationRequest request = buildTranslationRequest(message.getText(), profile, SpecialLangConfig.bot);

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

        final String commandStr = message.getText();

        if (isCommand(Command.start, commandStr)) {
            processStartCommand(message, commandStr);
        } else {
            sendMessage(message.getChatId(),
                    "Sorry, the commandStr `" + commandStr + "` is not implemented yet");
        }
    }

    /**
     * The {@link Command#start} displays general instructions to the user.
     * It may be used with parameter {@link #HELP_INLINE_START_PARAM} for instructions about inline mode.
     */
    private void processStartCommand(Message message, String commandStr) throws TelegramApiException {

        final String[] commandParts = commandStr.split("\\s+");

        if (commandParts.length == 1) {

            final UserProfile profile = userProfileRepo.getProfileById(message.getFrom().getId());

            final LangConfig langConfigBot = profile.getLangConfigs().get(SpecialLangConfig.bot.name());
            final LangConfig langConfigInline = profile.getLangConfigs().get(SpecialLangConfig.inline.name());

            sendMessage(message.getChatId(),
                    "You can write (or forward) messages here and I'll translate them" +
                            " from language `" + langConfigBot.getFrom() + "` to language `" + langConfigBot.getTo() + "`." +
                            " Soon you will be able to change those default settings." +
                            "\n\n" +
                            "But the cool thing is using me in inline mode when talking to other people," +
                            " by typing `@" + getBotUsername() + "` and then the message to translate." +
                            // TODO: Use this to let the user try the inline mode: https://core.telegram.org/bots/api#inlinekeyboardbutton
                            "\n" +
                            "\nCurrent translation here: " + langConfigBot.shortDescription() +
                            "\nCurrent translation inline: " + langConfigInline.shortDescription()
            );

        } else  {

            String parameter = commandParts[1];

            if (parameter.equals(HELP_INLINE_START_PARAM)) {

                final UserProfile profile = userProfileRepo.getProfileById(message.getFrom().getId());

                final LangConfig langConfigInline = profile.getLangConfigs().get(SpecialLangConfig.inline.name());

                sendMessage(message.getChatId(),
                        "You can send translated messages while talking to other people." +
                                " In any chat, type `@" + getBotUsername() + "` followed by your message," +
                                " and finish it with punctuation mark `" + PUNCTUATION_CHARS + "` to see the results." +
                                " You can then choose to send the original message, the translated message or both." +
                                " The results will be translated" +
                                " from language `" + langConfigInline.getFrom() + "` to language `" + langConfigInline.getTo() + "`." +
                                " Soon you will be able to change those default settings." +
                                " You can also choose the languages for a single message (without affecting the default setting)," +
                                " by typing something like `@" + getBotUsername() + " .en.es Translate this to Spanish.`");

            } else {

                sendMessage(message.getChatId(),
                        "Sorry, I don't understand the " + Command.start + " parameter `" + parameter + "`.");
            }
        }
    }

    private boolean isCommand(Command command, String text) {
        return text.startsWith(commandText.get(command));
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
    public static class TranslationRequest {

        public String text;
        public LangConfig langConfig;
        public String apiKey;

        public String getLangs() {
            return langConfig.shortDescription();
        }
    }
}
