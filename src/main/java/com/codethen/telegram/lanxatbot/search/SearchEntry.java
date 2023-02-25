package com.codethen.telegram.lanxatbot.search;

import java.util.Date;

public class SearchEntry {

    private final Long userId;
    private Date date;
    private final String source;
    private final String target;
    private final String from;
    private final String to;

    public SearchEntry(Long userId, Date date, String source, String target, String from, String to) {
        this.userId = userId;
        this.date = date;
        this.source = source;
        this.target = target;
        this.from = from;
        this.to = to;
    }

    public Long getUserId() {
        return userId;
    }

    public Date getDate() {
        return date;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
