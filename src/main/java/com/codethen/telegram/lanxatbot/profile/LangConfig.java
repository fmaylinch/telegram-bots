package com.codethen.telegram.lanxatbot.profile;

public class LangConfig {

    public static final String ARROW = " -> ";

    private String from;
    private String to;

    public LangConfig(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String shortDescription() {
        return from + ARROW + to;
    }

    public LangConfig reverse() {
        return new LangConfig(this.to, this.from);
    }
}
