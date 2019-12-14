package com.codethen.telegram.lanxatbot.translate;

public interface TranslationService {

    TranslationData translate(TranslationData request) throws TranslationException;
}
