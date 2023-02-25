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
}
