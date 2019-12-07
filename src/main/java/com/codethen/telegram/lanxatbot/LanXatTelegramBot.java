package com.codethen.telegram.lanxatbot;

import com.codethen.ApiKeys;
import com.codethen.telegram.lanxatbot.exception.InlineQueryException;
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

    private static final String END_OF_QUERY = " .";

    /**
     * TODO: Simplify behind an interface that this bot defines,
     *       like we do with {@link UserProfileRepository}
     */
    private YandexService yandex;

    private UserProfileRepository userProfileRepo;

    public LanXatTelegramBot(YandexService yandex, UserProfileRepository userProfileRepo) {
        this.yandex = yandex;
        this.userProfileRepo = userProfileRepo;
    }

    public void onUpdateReceived(Update update) {

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

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void processInlineQuery(Update update) throws TelegramApiException {

        final InlineQuery inlineQuery = update.getInlineQuery();
        final String query = inlineQuery.getQuery();
        System.out.println("Received inline query from " + getUserInfo(inlineQuery.getFrom()) + ": " + query);

        if (!query.endsWith(END_OF_QUERY)) {
            // I could throw an exception here, but this is going to be executed many times
            sendInfoResult(inlineQuery,
                    "- End message with '" + END_OF_QUERY + "' to translate.\n"
                            + "- Num chars: " + query.length() + ", last char: '" + query.charAt(query.length()-1) + "'");
            return;
        }

        final UserProfile profile = userProfileRepo.getProfileById(inlineQuery.getFrom().getId());
        if (profile == null) {
            throw new InlineQueryException(inlineQuery, "Write '/yandexkey YOUR_API' to the bot to set Yandex API Key");
        }

        // Remove end signal
        final String cleanQuery = query.substring(0, query.length() - END_OF_QUERY.length()).trim();

        final TranslationRequest request = buildTranslationRequest(cleanQuery, profile, false);

        if (request.text == null) {
            profile.setLangFrom(request.langFrom);
            profile.setLangTo(request.langTo);
            userProfileRepo.saveOrUpdate(profile);
            sendInfoResult(inlineQuery, "Changed default translation to " + request.getLangs());
            return;
        }

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
     * Note that {@link TranslationRequest#text} might be null, if query doesn't contain the message.
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

    /** In "en.ru" matcher would capture groups: "en", "ru" */
    private static final String langsPatternStr = "([a-z][a-z])\\.([a-z][a-z])";
    /**
     * In "en.ru message" matcher would capture groups: "en", "ru", "message".
     * In "en.ru" matcher would capture groups: "en", "ru", null.
     */
    private static final Pattern queryWithLangsPattern = Pattern.compile(langsPatternStr + "(?:\\s+(.+))?");

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
        sendMessage(message.getChatId(),
                "Sorry, the command `" + message.getText() + "` is not implemented yet");
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
        return "lanxat_bot";
    }

    public String getBotToken() {
        return ApiKeys.TELEGRAM_BOT_TOKEN;
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
