package com.codethen.telegram.lanxatbot.profile;

import java.util.Collections;
import java.util.List;

public class LangConfig {

    public static final String ARROW = " -> ";

    /**
     * If true, language will be detected, using {@link #getFrom()} languages as hint
     * (note that you may still write in another language not included in {@link #getFrom()}).
     * If false, first language of {@link #getFrom()} will be used.
     */
    private boolean detect;
    private List<String> from;
    private String to;

    /** @deprecated Necessary by Spring data */
    public LangConfig() {}

    public LangConfig(List<String> from, String to) {
        this(false, from, to);
    }

    public LangConfig(boolean detect, List<String> from, String to) {
        this.detect = detect;
        this.from = from;
        this.to = to;
    }

    public boolean isDetect() {
        return detect;
    }

    public List<String> getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String shortDescription() {

        if (isDetect()) {
            return "(" + String.join(",", from) + ")" + ARROW + to;
        } else {
            return from.get(0) + ARROW + to;
        }
    }

    public LangConfig reverse() {
        return new LangConfig(Collections.singletonList(to), this.from.get(0));
    }
}
