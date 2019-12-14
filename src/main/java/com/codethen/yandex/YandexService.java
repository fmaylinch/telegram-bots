package com.codethen.yandex;

import com.codethen.telegram.lanxatbot.profile.LangConfig;
import com.codethen.telegram.lanxatbot.translate.*;
import com.codethen.yandex.model.TranslateResponse;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class YandexService implements TranslationService {

    private YandexApi yandexApi;

    public YandexService(YandexApi yandexApi) {
        this.yandexApi = yandexApi;
    }

    @Override
    public TranslationData translate(TranslationData request) throws TranslationException {

        System.out.println("Translating " + request.getLangs() + " : '" + request.text + "'");

        final LangConfig langConfigToUse;

        if (request.langConfig.isDetect()) {
            final DetectResponse detectResponse = detect(buildDetectRequest(request));
            final String langTo = decideLangTo(detectResponse, request.langConfig);
            langConfigToUse = new LangConfig(Collections.singletonList(detectResponse.lang), langTo);
        } else {
            langConfigToUse = request.langConfig;
        }

        final String langs = langConfigToUse.getFrom().get(0) + "-" + langConfigToUse.getTo(); // Format required by Yandex

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

        // TODO: I'm not using the hints, because Yandex is actually worse with those
        //       For example, in the first case correctly guesses "en", but in the second case it guesses "en":
        //         https://translate.yandex.net/api/v1.5/tr.json/detect?text=hola&hint=&key=API_KEY
        //         https://translate.yandex.net/api/v1.5/tr.json/detect?text=hola&hint=en,es&key=API_KEY
        final String hint = request.possibleLangs == null ? "" :
                String.join(",", request.possibleLangs);

        final Call<DetectResponse> call = yandexApi.detect(request.apiKey, request.text, "");

        return executeCall(call);
    }

    private DetectRequest buildDetectRequest(TranslationData translationData) {

        final DetectRequest result = new DetectRequest();
        result.text = translationData.text;
        result.possibleLangs = translationData.langConfig.getFrom();
        result.apiKey = translationData.apiKey;
        return result;
    }

    /**
     * Usually would return langConfig.to, which is the desired target language, but if it's the same as
     * detected.lang, then returns the first language in langConfig.from that is not detected.lang.
     *
     * For example, suppose source languages (langConfig.from) are ["ru", "en"] and the desired target language
     * is ru (langConfig.to). When the message is written in "ru" (detected.lang), then this method would
     * decide that the desired target language is "en".
     *
     * TODO: Is it possible that sometimes we would like to translate to another language,
     *       not the first in the list of langConfig.from? That list should be sorted by
     *       preference of writing, so usually it would be good to have it translated to the first
     *       of those, but maybe sometimes not...
     *
     * @throws TranslationException if there is no alternative language in langConfig.from when is needed
     */
    private String decideLangTo(DetectResponse detected, LangConfig langConfig) throws TranslationException {

        if (!detected.lang.equals(langConfig.getTo())) {
            return langConfig.getTo();
        } else {
            final Optional<String> result = langConfig.getFrom().stream().filter(x -> !x.equals(detected.lang)).findFirst();

            if (!result.isPresent()) {
                throw new TranslationException("Language detected is " + detected.lang + " and no other language is available in 'from' languages of the lang config: " + langConfig.getFrom());
            }

            return result.get();
        }
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
