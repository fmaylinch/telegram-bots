# Telegram bots

Telegram bots using the [Telegram Bot Java Library][bot lib].

## Project

The project mixes Java and Kotlin,
and is launched via [Spring Boot][LanXatBotSpringBootApp].
For now there's just a simple [StatusController]
to check that the application started correctly.

## LanXat bot

The [LanXatTelegramBot] is an [inline bot]
that translates messages using the [Yandex Translate API].  

## Building and running the server with Docker

Build the jar with Maven. This generates `target/lanxat-bot-1.0-SNAPSHOT.jar`.
```shell script
mvn clean package
```

You could then start the app if you define or pass the environment variables
`MONGO_URL`, `MONGO_DATABASE`, `LANXAT_BOT_NAME` and `LANXAT_BOT_TOKEN`.
```shell script
java -jar target/lanxat-bot-1.0-SNAPSHOT.jar
```

Build the Docker image
```shell script
docker build -t fmaylinch/lanxatbot .
```

Run the Docker image binding port 8080 and passing environment variables with the flag `-e`:
```shell script
docker run -p 8080:8080 \
-e MONGO_URL='mongodb+srv://user:pass@domain.net/database_name' \
-e MONGO_DATABASE='database_name' \
-e LANXAT_BOT_NAME='name_of_bot' \
-e LANXAT_BOT_TOKEN='1234567890' \
fmaylinch/lanxatbot
```

If you want to push the docker image to docker hub:
```shell script
docker push fmaylinch/lanxatbot
```

[LanXatBotSpringBootApp]: src/main/java/com/codethen/LanXatBotSpringBootApp.java
[bot lib]: https://github.com/rubenlagus/TelegramBots
[StatusController]: src/main/kotlin/com/codethen/api/StatusController.kt
[LanXatTelegramBot]: src/main/java/com/codethen/telegram/lanxatbot/LanXatTelegramBot.java
[inline bot]: https://core.telegram.org/bots/inline
[Yandex Translate API]: https://tech.yandex.com/translate/
