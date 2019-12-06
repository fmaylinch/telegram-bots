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

        if (update.hasInlineQuery()) {
            final InlineQuery inlineQuery = update.getInlineQuery();
            final String query = inlineQuery.getQuery();
            System.out.println("Received inline query: " + query);

            try {

                if (query == null || query.equals("")) return;

                final String lang = "en-ru"; // TODO: Decide in query (or configuration?)

                System.out.println("Translating (" + lang + ") : " + query);
                final Call<YandexResponse> call = yandex.translate(ApiKeys.YANDEX_API_KEY, query, lang);

                final Response<YandexResponse> response;
                try {
                    response = call.execute();
                } catch (IOException e) {
                    sendResult(inlineQuery, "Error translating: " + query);
                    e.printStackTrace();
                    return;
                }

                if (response.code() != 200) {
                    sendResult(inlineQuery, "Could not translate: " + query);
                    System.out.println("Response status is not 200");
                    return;
                }

                final String translation = response.body().text.get(0);
                System.out.println("Translation: " + translation);
                sendResult(inlineQuery, translation);

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        if (!update.hasMessage()) {
            System.out.println("Received update without message");
            return;
        }

        final Message message = update.getMessage();

        if (!message.hasText()) {
            System.out.println("Received message without text");
            return;
        }

        System.out.println("Message text: " + message.getText());
    }

    private void sendResult(InlineQuery inlineQuery, String text) throws TelegramApiException {

        final String id = "1"; // Every result should have a different id

        execute(new AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.getId())
                .setResults(buildResult(text, id)));
    }

    private InlineQueryResultArticle buildResult(String translation, String id) {
        return new InlineQueryResultArticle()
                .setId(id)
                .setTitle("Sample title")
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
