package com.codethen.yandex;

import com.codethen.telegram.lanxatbot.translate.TranslationException;
import com.codethen.telegram.lanxatbot.translate.TranslationService;
import com.codethen.telegram.lanxatbot.translate.TranslationRequest;
import com.codethen.yandex.model.YandexResponse;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public class YandexService implements TranslationService {

    private YandexApi yandexApi;

    public YandexService(YandexApi yandexApi) {
        this.yandexApi = yandexApi;
    }

    @Override
    public String translate(TranslationRequest request) throws TranslationException {

        System.out.println("Translating " + request.getLangs() + " : '" + request.text + "'");

        final String langs = request.langConfig.getFrom() + "-" + request.langConfig.getTo(); // Format required by Yandex
        final Call<YandexResponse> call = yandexApi.translate(request.apiKey, request.text, langs);

        final Response<YandexResponse> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new TranslationException("Unexpected error calling Yandex API", e);
        }

        if (response.code() != 200) {
            throw new TranslationException("Unexpected bad response from Yandex API");
        }

        return response.body().text.get(0);
    }

}
