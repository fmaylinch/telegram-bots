package com.codethen.telegram.lanxatbot.translate;

import com.codethen.telegram.lanxatbot.profile.LangConfig;

public class TranslationData {

    public String text;
    public LangConfig langConfig;

    public String getLangs() {
        return langConfig.shortDescription();
    }
}
