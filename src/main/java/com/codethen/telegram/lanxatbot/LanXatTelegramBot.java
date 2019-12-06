package com.codethen.telegram.lanxatbot;

import com.codethen.ApiKeys;
import com.codethen.yandex.YandexService;
import com.codethen.yandex.model.YandexResponse;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * Bot for translating messages
 */
public class LanXatTelegramBot extends TelegramLongPollingBot {

    private YandexService yandex;

    public LanXatTelegramBot(YandexService yandex) {
        this.yandex = yandex;
    }

    public void onUpdateReceived(Update update) {

        try {
            if (update.hasInlineQuery()) {
                processInlineQuery(update);
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
        System.out.println("Received inline query: " + query);

        if (query.length() < 7) {
            // TODO: Send message explaining that format is "L1-L2 message"
            //  Doesn't work: execute(new SendChatAction(update.getChannelPost().getChatId(), "msg wrong"));
            return;
        }

        final String lang = query.substring(0, 5); // Format like "en-ru"
        final String text = query.substring(6);

        System.out.println("Translating (" + lang + ") : '" + text + "'");
        final Call<YandexResponse> call = yandex.translate(ApiKeys.YANDEX_API_KEY, text, lang);

        final Response<YandexResponse> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            sendResult(inlineQuery, "Error", "Error translating (" + lang + ") '" + query + "'");
            e.printStackTrace();
            return;
        }

        if (response.code() != 200) {
            sendResult(inlineQuery, "Error", "Bad response translating (" + lang + ") '" + query + "'");
            System.out.println("Response status is not 200");
            return;
        }

        final String translation = response.body().text.get(0);
        System.out.println("Translation: '" + translation + "'");
        sendResult(inlineQuery, lang, "- " + translation + "\n" + "- " + text);
    }

    private void processMessageOrCommand(Update update) {

        if (!update.hasMessage()) {
            System.out.println("Received update without message");
            return;
        }

        final Message message = update.getMessage();

        if (!message.hasText()) {
            System.out.println("Received message without text");
            return;
        }

        // TODO: May be a command like "/start" or "/yandexkey"
        System.out.println("Message text: " + message.getText());
    }

    private void sendResult(InlineQuery inlineQuery, String title, String text) throws TelegramApiException {

        final String id = "1"; // Each result should have a different id

        execute(new AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.getId())
                .setResults(buildResult(title, text, id)));
    }

    private InlineQueryResultArticle buildResult(String title, String translation, String id) {
        return new InlineQueryResultArticle()
                .setId(id)
                .setTitle(title)
                .setDescription(translation)
                .setInputMessageContent(new InputTextMessageContent()
                        .setParseMode(ParseMode.MARKDOWN) // Optional
                        .setMessageText(translation));
    }

    public String getBotUsername() {
        return "lanxat_bot";
    }

    public String getBotToken() {
        return ApiKeys.TELEGRAM_BOT_TOKEN;
    }
}
