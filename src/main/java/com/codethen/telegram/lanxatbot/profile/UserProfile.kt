package com.codethen.telegram.lanxatbot.profile

// TODO: Group language pairs

data class UserProfile (
        val userId: Int,
        /** Language that you usually want to use to write your messages */
        var langFrom: String,
        /** Language to which you usually want to translate your messages */
        var langTo: String,
        /** Language of the messages from other people that you usually want to translate */
        var langOtherFrom: String,
        /** Language to which you usually want to translate messages from other people */
        var langOtherTo: String,
        var yandexApiKey: String
)