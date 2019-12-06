package com.codethen.telegram.lanxatbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Bot for translating messages
 */
public class LanXatTelegramBot extends TelegramLongPollingBot {

    public void onUpdateReceived(Update update) {

        if (update.hasInlineQuery()) {
            final InlineQuery inlineQuery = update.getInlineQuery();
            final String query = inlineQuery.getQuery();
            System.out.println("Received inline query: " + query);

            try {

                final String translation = "Here you would see the translation of: " + query;

                final String id = "1"; // Every result should have a different id

                execute(new AnswerInlineQuery()
                        .setInlineQueryId(inlineQuery.getId())
                        .setResults(
                                buildResult(translation, id)));

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
        return "token";
    }
}
