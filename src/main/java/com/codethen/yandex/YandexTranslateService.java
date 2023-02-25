package com.codethen.yandex;

import java.io.IOException;

import com.codethen.telegram.lanxatbot.profile.LangConfig;
import com.codethen.telegram.lanxatbot.translate.DetectRequest;
import com.codethen.telegram.lanxatbot.translate.DetectResponse;
import com.codethen.telegram.lanxatbot.translate.TranslationData;
import com.codethen.telegram.lanxatbot.translate.TranslationException;
import com.codethen.telegram.lanxatbot.translate.TranslationService;
import com.codethen.yandex.model.TranslateResponse;
import retrofit2.Call;
import retrofit2.Response;

/**
 * TODO (ferran) 25.02.2023 - currently doesn't work because the API has changed
 * https://cloud.yandex.com/en-ru/docs/translate/
 */
public class YandexTranslateService implements TranslationService {

    private YandexApi yandexApi;

    public YandexTranslateService(YandexApi yandexApi) {
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

        // TODO (ferran) 25.02.2023 - set up API key
        final Call<TranslateResponse> call = yandexApi.translate("API_KEY_HERE", request.text, langs);
        final TranslateResponse response = executeCall(call);

        final TranslationData result = new TranslationData();
        result.text = response.text.get(0);
        result.langConfig = langConfigToUse;
        return result;
    }

    @Override
    public DetectResponse detect(DetectRequest request) throws TranslationException {

        final String hint = request.possibleLangs == null ? "" :
                String.join(",", request.possibleLangs);

        final Call<DetectResponse> call = yandexApi.detect(request.apiKey, request.text, hint);

        return executeCall(call);
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
