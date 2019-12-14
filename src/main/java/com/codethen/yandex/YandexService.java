package com.codethen.yandex;

import com.codethen.telegram.lanxatbot.translate.TranslationException;
import com.codethen.telegram.lanxatbot.translate.TranslationService;
import com.codethen.telegram.lanxatbot.translate.TranslationData;
import com.codethen.yandex.model.TranslateResponse;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public class YandexService implements TranslationService {

    private YandexApi yandexApi;

    public YandexService(YandexApi yandexApi) {
        this.yandexApi = yandexApi;
    }

    @Override
    public TranslationData translate(TranslationData request) throws TranslationException {

        System.out.println("Translating " + request.getLangs() + " : '" + request.text + "'");

        final String langs = request.langConfig.getFrom() + "-" + request.langConfig.getTo(); // Format required by Yandex
        final Call<TranslateResponse> call = yandexApi.translate(request.apiKey, request.text, langs);

        final Response<TranslateResponse> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new TranslationException("Unexpected error calling Yandex API", e);
        }

        if (response.code() != 200) {
            throw new TranslationException("Unexpected bad response from Yandex API");
        }

        final TranslationData result = new TranslationData();
        result.text = response.body().text.get(0);
        result.langConfig = request.langConfig;
        result.apiKey = request.apiKey;
        return result;
    }

}
