package com.codethen.telegram.lanxatbot.profile

// TODO: Group language pairs

class UserProfile (
        val userId: Int,
        val yandexApiKey: String,
        /** Language that you usually want to use to write your messages */
        val langFrom: String,
        /** Language to which you usually want to translate your messages */
        val langTo: String,
        /** Language of the messages from other people that you usually want to translate */
        val langOtherFrom: String,
        /** Language to which you usually want to translate messages from other people */
        val langOtherTo: String
)