package com.codethen.telegram.lanxatbot.exception

import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery

class InlineQueryException : RuntimeException {

    val query: InlineQuery

    constructor(query: InlineQuery, message: String): super(message) {
        this.query = query
    }

    constructor(query: InlineQuery, message: String, cause: Throwable): super(message, cause) {
        this.query = query
    }
}