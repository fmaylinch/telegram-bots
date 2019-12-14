package com.codethen.telegram.lanxatbot.translate;

import com.codethen.telegram.lanxatbot.profile.LangConfig;

public class TranslationRequest {

    public String text;
    public LangConfig langConfig;
    public String apiKey;

    public String getLangs() {
        return langConfig.shortDescription();
    }
}