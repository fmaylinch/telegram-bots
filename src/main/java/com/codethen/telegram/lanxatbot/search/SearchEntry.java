package com.codethen.telegram.lanxatbot.search;

public class SearchEntry {

    private final Integer userId;
    private final String source;
    private final String target;
    private final String langSource;
    private final String langTarget;

    public SearchEntry(Integer userId, String source, String target, String langSource, String langTarget) {
        this.userId = userId;
        this.source = source;
        this.target = target;
        this.langSource = langSource;
        this.langTarget = langTarget;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getLangSource() {
        return langSource;
    }

    public String getLangTarget() {
        return langTarget;
    }
}
