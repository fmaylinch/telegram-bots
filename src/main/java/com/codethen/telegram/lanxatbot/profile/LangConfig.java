package com.codethen.telegram.lanxatbot.profile;

public class LangConfig {

    private String from;
    private String to;

    public LangConfig(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String shortDescription() {
        return from + "-" + to;
    }
}
