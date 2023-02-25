package com.codethen.telegram.lanxatbot.translate;

import com.codethen.telegram.lanxatbot.profile.LangConfig;

public interface TranslationService {

    /**
     * The result will only contain one language in {@link LangConfig#getFrom()},
     * which is the language used for the translation.
     * That's important if the request has {@link LangConfig#shouldDetectLang()}. In that case,
     * the language contained in the resulting {@link LangConfig#getFrom()}, will be the detected one.
     *
     * TODO: Maybe we should use another DTO for the result, instead of having to explain this.
     */
    TranslationData translate(TranslationData request) throws TranslationException;

    DetectResponse detect(DetectRequest request) throws TranslationException;

    default DetectRequest buildDetectRequest(TranslationData translationData) {
        final DetectRequest result = new DetectRequest();
        result.text = translationData.text;
        result.possibleLangs = translationData.langConfig.getHints();
        return result;
    }


    /**
     * Usually would return langConfig.to, which is the desired target language, but if it's the same as
     * detected.lang, then returns langConfig.from.
     * For example, suppose source language (langConfig.from) is "en" and the desired target language
     * is "ru" (langConfig.to). When the message is written in "ru" (detected.lang), then this method would
     * decide that the desired target language is "en".
     */
    default String decideLangTo(DetectResponse detected, LangConfig langConfig) {
        return detected.lang.equals(langConfig.getTo()) ? langConfig.getFrom() : langConfig.getTo();
    }

}
