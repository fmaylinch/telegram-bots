package com.codethen.google;

import java.io.IOException;

import com.codethen.telegram.lanxatbot.exception.LanXatException;
import com.codethen.telegram.lanxatbot.profile.LangConfig;
import com.codethen.telegram.lanxatbot.translate.DetectRequest;
import com.codethen.telegram.lanxatbot.translate.DetectResponse;
import com.codethen.telegram.lanxatbot.translate.TranslationData;
import com.codethen.telegram.lanxatbot.translate.TranslationException;
import com.codethen.telegram.lanxatbot.translate.TranslationService;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.DetectedLanguage;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;

public class GoogleTranslateService implements TranslationService {

    private static final String PROJECT_ID = "yandex-terraform";

    @Override
    public TranslationData translate(TranslationData request) throws TranslationException {
        try {
            try (TranslationServiceClient client = TranslationServiceClient.create()) {
                LocationName parent = LocationName.of(PROJECT_ID, "global");

                System.out.println("Translating " + request.getLangs() + " : '" + request.text + "'");

                final LangConfig langConfigToUse;

                if (request.langConfig.shouldDetectLang()) {
                    final DetectResponse detectResponse = detect(buildDetectRequest(request));
                    final String langTo = decideLangTo(detectResponse, request.langConfig);
                    langConfigToUse = new LangConfig(null, detectResponse.lang, langTo);
                } else {
                    langConfigToUse = request.langConfig;
                }

                TranslateTextRequest req =
                        TranslateTextRequest.newBuilder()
                                .setParent(parent.toString())
                                .setMimeType("text/plain")
                                .setSourceLanguageCode(langConfigToUse.getFrom())
                                .setTargetLanguageCode(langConfigToUse.getTo())
                                .addContents(request.text)
                                .build();

                TranslateTextResponse response = client.translateText(req);

                // Display the translation for each input text provided
                for (Translation translation : response.getTranslationsList()) {
                    final TranslationData result = new TranslationData();
                    result.text = translation.getTranslatedText();
                    result.langConfig = langConfigToUse;
                    return result;
                }

                throw new LanXatException("Could not find translation for text: " + request.text);
            }
        } catch (IOException e) {
            throw new LanXatException("IOException while translating", e);
        }
    }

    @Override
    public DetectResponse detect(DetectRequest request) throws TranslationException {
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            LocationName parent = LocationName.of(PROJECT_ID, "global");

            var req = DetectLanguageRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setContent(request.text)
                    .build();

            var response = client.detectLanguage(req);

            var langCodes = response.getLanguagesList().stream().map(DetectedLanguage::getLanguageCode).toList();
            System.out.println("Detected languages: " + langCodes);
            var result = new DetectResponse();
            result.lang = langCodes.get(0);
            return result;

        } catch (IOException e) {
            throw new LanXatException("IOException while detecting language", e);
        }
    }
}
