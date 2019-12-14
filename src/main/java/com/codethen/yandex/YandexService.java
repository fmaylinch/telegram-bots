package com.codethen.yandex;

import com.codethen.telegram.lanxatbot.profile.LangConfig;
import com.codethen.telegram.lanxatbot.translate.*;
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

        final LangConfig langConfigToUse;

        if (request.langConfig.shouldDetectLang()) {
            final DetectResponse detectResponse = detect(buildDetectRequest(request));
            final String langTo = decideLangTo(detectResponse, request.langConfig);
            langConfigToUse = new LangConfig(null, detectResponse.lang, langTo);
        } else {
            langConfigToUse = request.langConfig;
        }

        final String langs = langConfigToUse.getFrom() + "-" + langConfigToUse.getTo(); // Format required by Yandex

        final Call<TranslateResponse> call = yandexApi.translate(request.apiKey, request.text, langs);
        final TranslateResponse response = executeCall(call);

        final TranslationData result = new TranslationData();
        result.text = response.text.get(0);
        result.langConfig = langConfigToUse;
        result.apiKey = request.apiKey;
        return result;
    }

    @Override
    public DetectResponse detect(DetectRequest request) throws TranslationException {

        final String hint = request.possibleLangs == null ? "" :
                String.join(",", request.possibleLangs);

        final Call<DetectResponse> call = yandexApi.detect(request.apiKey, request.text, hint);

        return executeCall(call);
    }

    private DetectRequest buildDetectRequest(TranslationData translationData) {

        final DetectRequest result = new DetectRequest();
        result.text = translationData.text;
        // TODO: Note taht Yandex is actually worse with hints.
        //       For example, in the first case correctly guesses "en", but in the second case it guesses "en":
        //         https://translate.yandex.net/api/v1.5/tr.json/detect?text=hola&hint=&key=API_KEY
        //         https://translate.yandex.net/api/v1.5/tr.json/detect?text=hola&hint=en,es&key=API_KEY
        result.possibleLangs = translationData.langConfig.getHints();
        result.apiKey = translationData.apiKey;
        return result;
    }

    /**
     * Usually would return langConfig.to, which is the desired target language, but if it's the same as
     * detected.lang, then returns langConfig.from.
     *
     * For example, suppose source language (langConfig.from) is "en" and the desired target language
     * is "ru" (langConfig.to). When the message is written in "ru" (detected.lang), then this method would
     * decide that the desired target language is "en".
     */
    private String decideLangTo(DetectResponse detected, LangConfig langConfig) {

        return detected.lang.equals(langConfig.getTo()) ? langConfig.getFrom() : langConfig.getTo();
    }

    private <T> T executeCall(Call<T> call) {

        final Response<T> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new TranslationException("Unexpected error calling Yandex API", e);
        }

        if (response.code() != 200) {
            throw new TranslationException("Unexpected bad response from Yandex API");
        }

        return response.body();
    }
}
