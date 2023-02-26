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

/**
 * This service needs a key to work.
 * The GOOGLE_APPLICATION_CREDENTIALS env var should point to the key file.
 * Look for the Translation API tutorial in Java.
 */
public class GoogleTranslateService implements TranslationService {

    private static final String PROJECT_ID = "yandex-terraform";

    private final TranslationServiceClient client;

    public GoogleTranslateService() {
        try {
            this.client = TranslationServiceClient.create();
        } catch (IOException e) {
            throw new RuntimeException("Could not create Google Translate client", e);
        }
    }

    @Override
    public TranslationData translate(TranslationData request) throws TranslationException {
        LocationName parent = LocationName.of(PROJECT_ID, "global");

        LangConfig langConfigToUse = langConfigToUse(request);
        System.out.println("Translating " + langConfigToUse.shortDescription() + " : '" + request.text + "'");

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

    @Override
    public DetectResponse detect(DetectRequest request) throws TranslationException {
        LocationName parent = LocationName.of(PROJECT_ID, "global");

        var req = DetectLanguageRequest.newBuilder()
                .setParent(parent.toString())
                .setMimeType("text/plain")
                .setContent(request.text)
                .build();

        var response = client.detectLanguage(req);

        var langs = response.getLanguagesList().stream().map(DetectedLanguage::getLanguageCode).toList();
        System.out.println("Detected languages: " + langs);
        return new DetectResponse(langs);
    }
}
