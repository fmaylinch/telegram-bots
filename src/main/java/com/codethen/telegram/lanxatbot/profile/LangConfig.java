package com.codethen.telegram.lanxatbot.profile;

import com.codethen.telegram.lanxatbot.LanXatTelegramBot;

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

    /** TODO: This is actually defined in {@link LanXatTelegramBot#langsPatternStr} */
    public String queryPattern() {
        return "." + from + "." + to;
    }
}
