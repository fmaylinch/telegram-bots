package com.codethen.google;

import java.io.IOException;

import com.codethen.telegram.lanxatbot.exception.LanXatException;
import com.codethen.telegram.lanxatbot.translate.DetectRequest;
import com.codethen.telegram.lanxatbot.translate.DetectResponse;
import com.codethen.telegram.lanxatbot.translate.TranslationData;
import com.codethen.telegram.lanxatbot.translate.TranslationException;
import com.codethen.telegram.lanxatbot.translate.TranslationService;
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

                TranslateTextRequest req =
                        TranslateTextRequest.newBuilder()
                                .setParent(parent.toString())
                                .setMimeType("text/plain")
                                .setSourceLanguageCode(request.langConfig.getFrom())
                                .setTargetLanguageCode(request.langConfig.getTo())
                                .addContents(request.text)
                                .build();

                TranslateTextResponse response = client.translateText(req);

                // Display the translation for each input text provided
                for (Translation translation : response.getTranslationsList()) {
                    final TranslationData result = new TranslationData();
                    result.text = translation.getTranslatedText();
                    result.langConfig = request.langConfig;
                    return result;
                }

                throw new LanXatException("Could not find translation for text: " + request.text);
            }
        } catch (IOException e) {
            throw new LanXatException("General error while translating", e);
        }
    }

    @Override
    public DetectResponse detect(DetectRequest request) throws TranslationException {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
