package com.codethen.telegram.lanxatbot.translate;

import java.util.List;

public class DetectResponse {

    public List<String> langs;

    public DetectResponse(List<String> langs) {
        this.langs = langs;
    }
}
