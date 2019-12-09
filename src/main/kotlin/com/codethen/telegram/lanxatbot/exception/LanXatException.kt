package com.codethen.telegram.lanxatbot.exception

open class LanXatException : RuntimeException {

    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}