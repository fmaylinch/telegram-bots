# Telegram bots

Telegram bots using the [Telegram Bot Java Library][bot lib].

## Project

The project mixes Java and Kotlin, and uses Spring Boot.
For now there's just a simple [StatusController]
to check that the application started correctly.

## LanXat bot

The [LanXatTelegramBot] is an [inline bot]
that translates messages using the [Yandex Translate API].  



[bot lib]: https://github.com/rubenlagus/TelegramBots
[StatusController]: src/main/java/com/codethen/api/StatusController.kt
[LanXatTelegramBot]: src/main/java/com/codethen/telegram/lanxatbot/LanXatTelegramBot.java
[inline bot]: https://core.telegram.org/bots/inline
[Yandex Translate API]: https://tech.yandex.com/translate/