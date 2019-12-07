package com.codethen.telegram.lanxatbot.exception

class YandexException : RuntimeException {

    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}