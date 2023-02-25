package com.codethen.telegram.lanxatbot.profile;

import javax.annotation.Nullable;
import java.util.List;

public class LangConfig {

    public static final String ARROW = " -> ";

    /**
     * If not null, language will be detected, using these languages as hint (might be an empty list).
     * Hints are not recommended for Yandex, but you may still specify an empty list for auto-detection.
     * Note that you may still write in another language not included in {@link #getHints()}.
     * If null, {@link #getFrom()} will be used, without detection.
     */
    @Nullable
    private List<String> hints;
    private String from;
    private String to;

    /** @deprecated TODO: Necessary by Spring data? */
    public LangConfig() {}

    public LangConfig(@Nullable List<String> hints, String from, String to) {
        this.hints = hints;
        this.from = from;
        this.to = to;
    }

    public boolean shouldDetectLang() {
        return hints != null;
    }

    @Nullable
    public List<String> getHints() {
        return hints;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String shortDescription() {

        if (shouldDetectLang()) {
            return "(" + (hints.isEmpty() ? "any" : String.join(",", hints)) + ")" + ARROW + to;
        } else {
            return from + ARROW + to;
        }
    }

    public LangConfig reverse() {
        return new LangConfig(null, to, this.from);
    }
}
