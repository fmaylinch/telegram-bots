package com.codethen.telegram.lanxatbot;

import com.codethen.telegram.lanxatbot.exception.*;
import com.codethen.telegram.lanxatbot.profile.LangConfig;
import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.codethen.yandex.YandexService;
import com.codethen.yandex.model.YandexResponse;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import retrofit2.Call;
import retrofit2.Response;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * Bot for translating messages.
 * You can send messages to the bot, but the main feature is to use inline queries.
 * See: https://core.telegram.org/bots/inline
 *
 * In an inline query you type `@lanxat_bot Hello, how are you?`
 * and it will suggest the translation depending on the translation configuration you have currently.
 * You may also explicitly indicate the languages to use for translation in a single message
 * (overriding the default configuration).
 *
 * Note that the bot is currently named lanxat_bot, but this may change. See {@link #getBotUsername()}.
 */
public class LanXatTelegramBot extends TelegramLongPollingBot {

    private enum Command {

        start, langconfig;

        String getText() {
            return "/" + name();
        }
    }

    private enum SpecialLangConfig {

        /** {@link LangConfig} for inline messages */
        inline,
        /** {@link LangConfig} for messages sent to the bot */
        bot
    }

    private static final Map<Command, String> commandText =
            Arrays.stream(Command.values()).collect(Collectors.toMap(x -> x, Command::getText));

    /** {@link Command#start} parameter to display information about how inline mode works */
    private static final String HELP_INLINE_START_PARAM = "help-inline";
    /** If this dot comes after the languages, it means the user wants to change the defaults */

    private final String botName;
    private final String apiToken;

    /**
     * TODO: Simplify behind an interface that this bot defines,
     *       like we do with {@link UserProfileRepository}
     */
    private final YandexService yandex;

    private final UserProfileRepository userProfileRepo;

    public LanXatTelegramBot(String botName, String apiToken, YandexService yandex, UserProfileRepository userProfileRepo) {
        this.botName = botName;
        this.apiToken = apiToken;
        this.yandex = yandex;
        this.userProfileRepo = userProfileRepo;
    }

    public void onUpdateReceived(Update update) {

        try {
            try {
                if (update.hasInlineQuery()) {
                    try {
                        processInlineQuery(update);
                    } catch (InlineQueryException e) {
                        sendErrorResult(e.getQuery(), e.getMessage());
                    }
                } else {
                    processMessageOrCommand(update);
                }

            } catch (ProfileNotExistsException e) {

                System.out.println("User does not have a profile: " + e.getUserId());
                sendError(update, "You don't have a profile yet. This bot is in development. Ask the bot creator. Your userId is " + e.getUserId() +".");

            } catch (LanXatException e) {

                sendError(update, e.getMessage()); // TODO: This is not markdown
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendError(Update update, String markdown) throws TelegramApiException {

        if (update.hasMessage()) {
            sendMessage(update.getMessage(), markdown);
        } else if (update.hasInlineQuery()) {
            displayInlineHelpButton(update.getInlineQuery(), markdown); // TODO: Markdown not allowed here
        } else {
            System.out.println("Update without message nor inline query produced an error but I don't know where to send it: " + markdown);
        }
    }

    private void processInlineQuery(Update update) throws TelegramApiException {

        final InlineQuery inlineQuery = update.getInlineQuery();
        final String query = inlineQuery.getQuery();
        System.out.println("Received inline query from " + getUserInfo(inlineQuery.getFrom()) + ": `" + query + "`");

        final UserProfile profile = getProfile(inlineQuery.getFrom());

        final TranslationRequest request = buildTranslationRequest(query, profile, SpecialLangConfig.inline);

        if (!sentenceIsFinished(query)) {
            displayInlineHelpButton(inlineQuery, "Translating " + request.langConfig.shortDescription() + ". Click to know more.");
            return;
        }

        if (request.text == null || request.text.length() == 1) return; // No text or just punctuation

        try {
            final String translation = requestYandexTranslation(request);
            System.out.println("Translation: '" + translation + "'");

            final String reverseTranslation = requestYandexTranslation(request.reverse());
            System.out.println("Reverse translation: '" + translation + "'");

            execute(new AnswerInlineQuery()
                    .setInlineQueryId(inlineQuery.getId())
                    .setCacheTime(0) // TODO: Maybe adjust later as needed
                    .setResults(
                            buildResult(request.langConfig.getTo(), translation, "1"),
                            buildResult(request.langConfig.getFrom(), request.text, "2"),
                            buildResult(request.getLangs() + LangConfig.ARROW + request.langConfig.getFrom(), reverseTranslation, "3"),
                            buildResult(request.getLangs(), "- " + translation + "\n" + "- " + request.text, "4")
                    ));

        } catch (YandexException e) {
            throw new InlineQueryException(inlineQuery, e.getMessage(), e);
        }
    }

    private void displayInlineHelpButton(InlineQuery inlineQuery, String text) throws TelegramApiException {

        execute(new AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.getId())
                .setCacheTime(0)
                .setSwitchPmText(text)
                .setSwitchPmParameter(HELP_INLINE_START_PARAM)
                .setResults());
    }

    private boolean sentenceIsFinished(String text) {

        if (text.length() == 0) return false;

        final char lastChar = text.charAt(text.length() - 1);
        return lastChar != ',' && !Character.isLetterOrDigit(lastChar);
    }

    private String requestYandexTranslation(TranslationRequest request) throws YandexException {

        System.out.println("Translating " + request.getLangs() + " : '" + request.text + "'");

        final Call<YandexResponse> call = yandex.translate(request.apiKey, request.text, request.yandexLangs());

        final Response<YandexResponse> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new YandexException("Unexpected error calling Yandex API", e);
        }

        if (response.code() != 200) {
            throw new YandexException("Unexpected bad response from Yandex API");
        }

        return response.body().text.get(0);
    }

    /** In ".en.ru" matcher would capture groups: "en", "ru". */
    private static final String langsPatternStr = "\\.([a-z][a-z])\\.([a-z][a-z])";
    /** In ".name" matcher would capture group: "name". */
    private static final String langConfigPatternStr = "\\.(\\w+)";
    /** In "   message" matcher would capture group: "message". */
    private static final String messagePatternStr = "\\s+(.+)";
    /** In ".en.ru message" matcher would capture groups: "en", "ru", "message". */
    private static final Pattern langsAndMessagePattern = Pattern.compile(langsPatternStr + messagePatternStr);
    /** In ".name message" matcher would capture groups: "name", "message". */
    private static final Pattern langConfigAndMessagePattern = Pattern.compile(langConfigPatternStr + messagePatternStr);
    /**
     * In ".name = .en.ru" matcher would capture groups: "name", "en", "ru".
     * In ".name =" matcher would capture groups: "name", null, null.
     */
    private static final Pattern langConfigSetupPattern = Pattern.compile(langConfigPatternStr + "\\s*=\\s*" + "(?:" + langsPatternStr + ")?");

    private String getUserInfo(User user) {
        return user.getUserName() + " (" + user.getId() + ")";
    }

    /**
     * Parses query, witch may be {@link #langsAndMessagePattern}, {@link #langConfigAndMessagePattern},
     * or otherwise it's just a plain text with no pattern.
     *
     * If a pattern matches, we decide the {@link LangConfig} from there
     * (either explicit languages or one of the existing {@link UserProfile#getLangConfigs()}).
     *
     * If the query is a plain text, the {@link LangConfig}) is also taken from
     * {@link UserProfile#getLangConfigs()}, according to the given {@link SpecialLangConfig}.
     *
     * Note that {@link TranslationRequest#text} might be null if not present in the query.
     */
    private TranslationRequest buildTranslationRequest(String query, UserProfile profile, SpecialLangConfig config) {

        final TranslationRequest request = new TranslationRequest();
        request.apiKey = profile.getYandexApiKey();

        final Matcher langsAndMessage = langsAndMessagePattern.matcher(query);
        if (langsAndMessage.matches()) {
            request.langConfig = new LangConfig(langsAndMessage.group(1), langsAndMessage.group(2));
            request.text = langsAndMessage.group(3);
        } else {
            final Matcher langConfigAndMessage = langConfigAndMessagePattern.matcher(query);
            if (langConfigAndMessage.matches()) {
                final String langConfigName = langConfigAndMessage.group(1);
                request.langConfig = profile.getLangConfigs().get(langConfigName);
                if (request.langConfig == null) {
                    throw new LangConfigNotExistsException(langConfigName);
                }
                request.text = langConfigAndMessage.group(2);
            } else {
                request.langConfig = profile.getLangConfigs().get(config.name());
                request.text = query;
            }
        }

        return request;
    }

    private void processMessageOrCommand(Update update) throws TelegramApiException {

        if (!update.hasMessage()) {
            System.out.println("Received update without message");
            return;
        }

        final Message message = update.getMessage();
        final String userInfo = getUserInfo(message.getFrom());

        if (!message.hasText()) {
            System.out.println("Received message without text from " + userInfo);
            return;
        }

        System.out.println("Message text from " + userInfo + ": " + message.getText());

        if (message.isCommand()) {
            processCommand(update.getMessage());
        } else {
            processMessage(update.getMessage());
        }
    }

    private void processMessage(Message message) throws TelegramApiException {

        // Maybe it's a lang config setup?
        final Matcher matcher = langConfigSetupPattern.matcher(message.getText());
        if (matcher.matches()) {
            final String langConfigName = matcher.group(1);
            final LangConfig langConfig = matcher.group(2) != null ?
                    new LangConfig(matcher.group(2), matcher.group(3)) : null;
            setupLangConfig(langConfigName, langConfig, message);
            return;
        }

        processMessageAsTranslation(message);
    }

    private void processMessageAsTranslation(Message message) throws TelegramApiException {

        final UserProfile profile = getProfile(message.getFrom());

        final TranslationRequest request = buildTranslationRequest(message.getText(), profile, SpecialLangConfig.bot);

        try {
            final String translation = requestYandexTranslation(request);
            final String msg = "Translated " + request.getLangs() + "\n" + translation;
            System.out.println("Sent translation " + request.getLangs() + ": '" + translation + "'");
            sendMessage(message, msg);
        } catch (YandexException e) {
            sendMessage(message, "There was an error with Yandex API: " + e.getMessage());
        } catch (TelegramApiException e) {
            sendMessage(message, "There was an error with Telegram API: " + e.getMessage());
        }
    }

    private void sendMessage(Message originalMessage, String markdown) throws TelegramApiException {
        execute(new SendMessage()
                .setChatId(originalMessage.getChatId())
                .setText(markdown)
                .setParseMode(ParseMode.MARKDOWN));
    }

    private void processCommand(Message message) throws TelegramApiException {

        final String commandStr = message.getText();

        if (isCommand(Command.start, commandStr)) {
            processStartCommand(message, commandStr);
        } else if (isCommand(Command.langconfig, commandStr)) {
            processLangConfigCommand(message, commandStr);
        } else {
            sendMessage(message, "Sorry, the commandStr `" + commandStr + "` is not implemented yet");
        }
    }

    /**
     * Sets or removes a language configuration and sends a message about the change.
     * If the langConfigName is a {@link SpecialLangConfig}, it won't be removed (it's not allowed).
     */
    private void setupLangConfig(String langConfigName, @Nullable LangConfig langConfig, Message message) throws TelegramApiException {

        final UserProfile profile = getProfile(message.getFrom());
        final String langConfigAsMarkdown = "`" + langConfigNameToPattern(langConfigName) + "`";
        final String moreInfo = "\nSee " + commandText.get(Command.langconfig) + " for more information.";

        if (langConfig != null) {
            profile.getLangConfigs().put(langConfigName, langConfig);
            userProfileRepo.saveOrUpdate(profile);
            sendMessage(message, "Now " + langConfigAsMarkdown + " is set to `" + toLangsPattern(langConfig) + "`." + moreInfo);
        } else {
            if (isSpecialLangConfigName(langConfigName)) {
                sendMessage(message, "The configuration " + langConfigAsMarkdown + " is special and cannot be removed." + moreInfo);
            } else if (!profile.getLangConfigs().containsKey(langConfigName)) {
                sendMessage(message, "The configuration " + langConfigAsMarkdown + " doesn't exist." + moreInfo);
            } else {
                profile.getLangConfigs().remove(langConfigName);
                userProfileRepo.saveOrUpdate(profile);
                sendMessage(message, "The configuration " + langConfigAsMarkdown + " was removed." + moreInfo);
            }
        }
    }

    private boolean isSpecialLangConfigName(String langConfigName) {
        return Arrays.stream(SpecialLangConfig.values()).anyMatch(c -> c.name().equals(langConfigName));
    }

    /**
     * The {@link Command#langconfig} is used to display or setup language configurations.
     */
    private void processLangConfigCommand(Message message, String commandStr) throws TelegramApiException {

        final String[] commandParts = commandStr.split("\\s+");

        if (commandParts.length == 1) { // No parameters

            final UserProfile profile = getProfile(message.getFrom());

            final String langConfigsMarkdown = profile.getLangConfigs().entrySet().stream()
                    .map(e -> "- `" + langConfigNameToPattern(e.getKey()) + " = " + toLangsPattern(e.getValue()) + "`\n")
                    .collect(Collectors.joining());

            sendMessage(message,
                    "**Language configurations**" +
                            "\n\n" +
                            "You can write something like `.en.es Translate this!` to translate from English to Spanish." +
                            "\n\n" +
                            "But you can setup shortcuts to select the languages to translate more easily." +
                            " For example, you may decide that `.e` is equivalent to `.es.en` and then just write `.e Translate this!`." +
                            "\n\n" +
                            "There are two special language configurations: `.bot` and `.inline`. By default," +
                            " I will use the `.bot` configuration to translate messages you write/forward to me, and" +
                            " I will use the `.inline` configuration to translate messages you send inline to other users." +
                            "\n\n" +
                            "You can modify or create language configurations by sending me a message like:" +
                            "\n\n" +
                            "`.e = .es.en`" +
                            "\n\n" +
                            " That would configure `.e` to be equivalent to `.es.en`." +
                            "\n\n" +
                            " To remove a configuration, write a message like:" +
                            "\n\n" +
                            "`.e =`" +
                            "\n\n" +
                            "Note that there is no value after the equal sign." +
                            "\n\n" +
                            "Current language profiles:\n" + langConfigsMarkdown);

        } else {

            //final String parameter = commandParts[1];

            sendMessage(message,
                    "Sorry, the " + Command.langconfig + " command doesn't allow parameters.");
        }
    }

    /** Returns the {@link LangConfig} name with the {@link #langConfigPatternStr} pattern. */
    private String langConfigNameToPattern(String langConfigName) {
        return "." + langConfigName;
    }

    /** Returns the {@link LangConfig} with the {@link #langsPatternStr} pattern. */
    private String toLangsPattern(LangConfig langConfig) {
        return "." + langConfig.getFrom() + "." + langConfig.getTo();
    }

    /**
     * The {@link Command#start} displays general instructions to the user.
     * It may be used with parameter {@link #HELP_INLINE_START_PARAM} for instructions about inline mode.
     */
    private void processStartCommand(Message message, String commandStr) throws TelegramApiException {

        final String[] commandParts = commandStr.split("\\s+");

        if (commandParts.length == 1) { // No parameters

            final UserProfile profile = getProfile(message.getFrom());

            final LangConfig langConfigBot = profile.getLangConfigs().get(SpecialLangConfig.bot.name());
            final LangConfig langConfigInline = profile.getLangConfigs().get(SpecialLangConfig.inline.name());

            final String markdown =
                    "**How this bot works**" +
                    "\n\n" +
                    "You can write (or forward) messages to me and I'll translate them" +
                    " from language `" + langConfigBot.getFrom() + "` to language `" + langConfigBot.getTo() + "`." +
                    " You can change that setting, see " + commandText.get(Command.langconfig) + "."+
                    "\n\n" +
                    "But the cool thing is using me in inline mode when talking to other people." +
                    " In chat, type `@" + getBotUsername() + "` and then the message to translate." +
                    " Try the inline mode by clicking the button below." +
                    "\n" +
                    "\nCurrent translation here: " + langConfigBot.shortDescription() +
                    "\nCurrent translation inline: " + langConfigInline.shortDescription();

            //sendMessage(message, markdown);

            execute(new SendMessage()
                    .setChatId(message.getChatId())
                    .setText(markdown)
                    .setParseMode(ParseMode.MARKDOWN)
                    .setReplyMarkup(new InlineKeyboardMarkup()
                            .setKeyboard(singletonList(singletonList(
                                    new InlineKeyboardButton()
                                            .setText("Try the inline mode!")
                                            .setSwitchInlineQuery(".en.ru Have you tried @" + getBotUsername() + "?")
                            )))));

        } else  {

            String parameter = commandParts[1];

            if (parameter.equals(HELP_INLINE_START_PARAM)) {

                final UserProfile profile = getProfile(message.getFrom());

                final LangConfig langConfigInline = profile.getLangConfigs().get(SpecialLangConfig.inline.name());

                sendMessage(message,
                        "**Inline mode**" +
                                "\n\n" +
                                "While talking to other people, type `@" + getBotUsername() + "` followed by your message," +
                                " and finish it with punctuation mark or emoji to see the results." +
                                "\n\n" +
                                "The results let you to send the original message, the translated message or both." +
                                "\n\n" +
                                "The default inline translation is now " + langConfigInline.shortDescription() +
                                " but you can change that setting (see " + commandText.get(Command.langconfig) + ")." +
                                "\n\n" +
                                "You also choose the languages for a single message (without affecting the default setting)," +
                                " by typing something like `@" + getBotUsername() + " .en.es Translate this to Spanish.`");

            } else {

                sendMessage(message,
                        "Sorry, I don't understand the " + Command.start + " parameter `" + parameter + "`.");
            }
        }
    }

    private UserProfile getProfile(User user) {
        return userProfileRepo.getProfileById(user.getId());
    }

    private boolean isCommand(Command command, String text) {
        return text.startsWith(commandText.get(command));
    }

    private void sendInfoResult(InlineQuery inlineQuery, String markdown) throws TelegramApiException {
        sendSingleResult(inlineQuery, "Information", markdown);
    }

    private void sendErrorResult(InlineQuery inlineQuery, String markdown) throws TelegramApiException {
        sendSingleResult(inlineQuery, "Error", markdown);
    }

    private void sendSingleResult(InlineQuery inlineQuery, String title, String markdown) throws TelegramApiException {

        final String id = "1"; // Each result should have a different id

        execute(new AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.getId())
                .setResults(buildResult(title, markdown, id)));
    }

    private InlineQueryResultArticle buildResult(String title, String markdownText, String resultId) {
        return new InlineQueryResultArticle()
                .setId(resultId)
                .setTitle(title)
                .setDescription(markdownText)
                .setInputMessageContent(new InputTextMessageContent()
                        .setParseMode(ParseMode.MARKDOWN)
                        .setMessageText(markdownText));
    }

    public String getBotUsername() {
        return botName;
    }

    public String getBotToken() {
        return apiToken;
    }

    /** TODO: This could be part of {@link YandexService} */
    public static class TranslationRequest {

        public String text;
        public LangConfig langConfig;
        public String apiKey;

        public String getLangs() {
            return langConfig.shortDescription();
        }

        /** Languages as Yandex expects */
        public String yandexLangs() {
            return langConfig.getFrom() + "-" + langConfig.getTo();
        }

        /** Returns the a similar request but with the languages in {@link LangConfig} reversed */
        public TranslationRequest reverse() {

            final TranslationRequest result = new TranslationRequest();
            result.text = this.text;
            result.langConfig = new LangConfig(this.langConfig.getTo(), this.langConfig.getFrom());
            result.apiKey = this.apiKey;
            return result;
        }
    }
}
