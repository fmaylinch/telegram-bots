package com.codethen.telegram.lanxatbot.translate;

public interface TranslationService {

    String translate(TranslationRequest request) throws TranslationException;
}
