package com.codethen.telegram.lanxatbot;

import com.codethen.telegram.lanxatbot.exception.InlineQueryException;
import com.codethen.telegram.lanxatbot.exception.LanXatException;
import com.codethen.telegram.lanxatbot.exception.LangConfigNotExistsException;
import com.codethen.telegram.lanxatbot.exception.ProfileNotExistsException;
import com.codethen.telegram.lanxatbot.profile.LangConfig;
import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.codethen.telegram.lanxatbot.translate.TranslationData;
import com.codethen.telegram.lanxatbot.translate.TranslationException;
import com.codethen.telegram.lanxatbot.translate.TranslationService;
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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.codethen.telegram.lanxatbot.profile.LangConfig.ARROW;
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

    private final TranslationService translationService;

    private final UserProfileRepository userProfileRepo;

    public LanXatTelegramBot(String botName, String apiToken, TranslationService translationService, UserProfileRepository userProfileRepo) {
        this.botName = botName;
        this.apiToken = apiToken;
        this.translationService = translationService;
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

        final TranslationData request = buildTranslationRequest(query, profile, SpecialLangConfig.inline);

        if (!sentenceIsFinished(query)) {
            final String langConfigDesc = request.langConfig.shortDescription();
            displayInlineHelpButton(inlineQuery, "Translating " + langConfigDesc + ". Click to know more.");
            return;
        }

        if (request.text == null || request.text.length() == 1) return; // No text or just punctuation

        final TranslationData translation = translationService.translate(request);
        System.out.println("Translation: '" + translation.text + "'");

        final TranslationData revReq = new TranslationData();
        revReq.text = translation.text;
        revReq.langConfig = translation.langConfig.reverse();
        revReq.apiKey = request.apiKey;

        TranslationData revTranslation = translationService.translate(revReq);
        System.out.println("Reverse translation: '" + revTranslation.text + "'");

        final String langTo = translation.langConfig.getTo();
        final String langFrom = translation.langConfig.getFrom();
        final String langToRev = revTranslation.langConfig.getTo();

        execute(new AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.getId())
                .setCacheTime(0) // TODO: Maybe adjust later as needed
                .setResults(
                        buildResult(getThumbnail(langTo), langTo, translation.text, "1"),
                        buildResult(getThumbnail(langFrom), langFrom + " (original)", request.text, "2"),
                        buildResult(getThumbnail(revTranslation.langConfig.getTo()), translation.getLangs() + ARROW + langToRev + " (reverse translation)", revTranslation.text, "3"),
                        buildResult(null, translation.langConfig.getFrom() + " / " + translation.langConfig.getTo(), "- " + request.text + "\n" + "- " + translation.text, "4")
                ));
    }

    private String getThumbnail(String lang) {
        final String countryCode = lang.equals("en") ? "gb" : lang;
        return "https://www.countryflags.io/" + countryCode + "/flat/64.png";
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

    /**
     * In ".en.ru" matcher would capture groups: null, "en", "ru".
     * In "(es,en,ru) .en.ru" matcher would capture groups: "es,en,fr", "en", "ru".
     * In "() .en.ru" matcher would capture groups: "", "en", "ru".
     */
    private static final String hintsAndLangsPatternStr = "(?:\\(([a-z,]*)\\)\\s+)?\\.([a-z][a-z])\\.([a-z][a-z])";
    /** In ".name" matcher would capture group: "name". */
    private static final String langConfigPatternStr = "\\.(\\w+)";
    /** In "   message" matcher would capture group: "message". */
    private static final String messagePatternStr = "\\s+(.+)";
    /** In ".en.ru message" matcher would capture groups: "en", "ru", "message". */
    private static final Pattern hintsAndLangsAndMessagePattern = Pattern.compile(hintsAndLangsPatternStr + messagePatternStr);
    /** In ".name message" matcher would capture groups: "name", "message". */
    private static final Pattern langConfigAndMessagePattern = Pattern.compile(langConfigPatternStr + messagePatternStr);
    /**
     * In ".name = .en.ru" matcher would capture groups: "name", null, "en", "ru".
     * In ".name = (es,en) .en.ru" matcher would capture groups: "name", "es,en", "en", "ru".
     * In ".name = () .en.ru" matcher would capture groups: "name", "", "en", "ru".
     * In ".name =" matcher would capture groups: "name", null, null.
     */
    private static final Pattern hintsAndLangConfigSetupPattern = Pattern.compile(langConfigPatternStr + "\\s*=\\s*" + "(?:" + hintsAndLangsPatternStr + ")?");

    private String getUserInfo(User user) {
        return user.getUserName() + " (" + user.getId() + ")";
    }

    /**
     * Parses query, witch may be {@link #hintsAndLangsAndMessagePattern}, {@link #langConfigAndMessagePattern},
     * or otherwise it's just a plain text with no pattern.
     *
     * If a pattern matches, we decide the {@link LangConfig} from there
     * (either explicit languages or one of the existing {@link UserProfile#getLangConfigs()}).
     *
     * If the query is a plain text, the {@link LangConfig}) is also taken from
     * {@link UserProfile#getLangConfigs()}, according to the given {@link SpecialLangConfig}.
     *
     * Note that {@link TranslationData#text} might be null if not present in the query.
     */
    private TranslationData buildTranslationRequest(String query, UserProfile profile, SpecialLangConfig defaultConfig) {

        final TranslationData request = new TranslationData();
        request.apiKey = profile.getYandexApiKey();

        final Matcher hintsAndLangsAndMessage = hintsAndLangsAndMessagePattern.matcher(query);
        if (hintsAndLangsAndMessage.matches()) {
            request.langConfig = buildLangConfigFromMatcher(hintsAndLangsAndMessage, 1);
            request.text = hintsAndLangsAndMessage.group(4);
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
                request.langConfig = profile.getLangConfigs().get(defaultConfig.name());
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

        final Matcher matcher = hintsAndLangConfigSetupPattern.matcher(message.getText());
        if (matcher.matches()) {
            final String langConfigName = matcher.group(1);
            final LangConfig langConfig = buildLangConfigFromMatcher(matcher, 2);
            setupLangConfig(langConfigName, langConfig, message);
            return;
        }

        processMessageAsTranslation(message);
    }

    /** Builds a {@link LangConfig} from the data taken from the {@link Matcher} */
    private LangConfig buildLangConfigFromMatcher(Matcher matcher, int firstGroupIndex) {

        final String hintsStr = matcher.group(firstGroupIndex);
        final String langFrom = matcher.group(firstGroupIndex + 1);
        final String langTo = matcher.group(firstGroupIndex + 2);

        if (langFrom == null) return null;

        // If hintsStr part is not present there is no auto-detection
        final List<String> hints = hintsStr == null ? null :
                (hintsStr.isEmpty() ? Collections.emptyList() :
                        Arrays.asList(hintsStr.split(",")));

        return new LangConfig(hints, langFrom, langTo);
    }

    private void processMessageAsTranslation(Message message) throws TelegramApiException {

        final UserProfile profile = getProfile(message.getFrom());

        final TranslationData request = buildTranslationRequest(message.getText(), profile, SpecialLangConfig.bot);

        try {
            final TranslationData translation = translationService.translate(request);
            final String msg = "Translated " + translation.getLangs() + "\n" + translation.text;
            System.out.println("Sent translation " + translation.getLangs() + ": '" + translation.text + "'");
            sendMessage(message, msg);
        } catch (TranslationException e) {
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
                    .map(e -> "`" + langConfigNameToPattern(e.getKey()) + " = " + toLangsPattern(e.getValue()) + "`\n")
                    .collect(Collectors.joining());

            final String markdown =
                "**Language configurations**" +
                "\n\n" +
                "To translate from English to Spanish you can, for example, write:\n" +
                "`.en.es Translate this!`" +
                "\n\n" +
                "You can also setup configurations to select the languages to translate. " +
                "For example, you can set `.e` as an alias for `.es.en` and then just write:\n" +
                "`.e Translate this!`." +
                "\n\n" +
                "There are two special language configurations: " +
                "`." + SpecialLangConfig.bot + "` and `." + SpecialLangConfig.inline + "`. " + "By default, " +
                "the `." + SpecialLangConfig.bot + "` configuration will be used to translate messages " +
                "you write/forward here to the bot, and " +
                "the `." + SpecialLangConfig.inline + "` configuration will be used to translate messages " +
                "you send inline to other users." +
                "\n\n" +
                "To modify or create language configurations send here a message like:\n" +
                "`.e = .es.en`" +
                "\n\n" +
                "To remove a configuration, send a message like:\n" +
                "`.e =`" +
                "\n\n" +
                "Advanced: when creating a language configuration, you can also specify that you want " +
                "language auto-detection, typing parenthesis with optional hints inside " +
                "(hints are not recommended). Examples:\n" +
                "`.e = () .es.en`\n" +
                "`.e = (es,fr,it) .es.en`" +
                "\n\n" +
                "Current language profiles:\n" + langConfigsMarkdown +
                "";

            sendMessage(message, markdown);

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

    /** Returns the {@link LangConfig} looking like {@link #hintsAndLangsPatternStr}. */
    private String toLangsPattern(LangConfig langConfig) {

        final String detect = langConfig.getHints() != null ?
                "(" + String.join(",", langConfig.getHints()) + ") " : "";

        return detect + "." + langConfig.getFrom() + "." + langConfig.getTo();
    }

    /**
     * The {@link Command#start} displays general instructions to the user.
     * It may be used with parameter {@link #HELP_INLINE_START_PARAM} for instructions about inline mode.
     */
    private void processStartCommand(Message message, String commandStr) throws TelegramApiException {

        final String[] commandParts = commandStr.split("\\s+");

        if (commandParts.length == 1) { // No parameters

            final String markdown =
                            "**How this bot works**" +
                            "\n\n" +
                            "You can write (or forward) messages to this bot and it will translate them, " +
                            "according to your `." + SpecialLangConfig.bot + "` profile " +
                            "in " + commandText.get(Command.langconfig) + "." +
                            "\n\n" +
                            "This bot can also be used inline, i.e. you can use it while " +
                            "chatting with other people. In a chat, type `@" + getBotUsername() + "` and then the " +
                            "message to translate. It will be translated according to your `." + SpecialLangConfig.inline + "` profile " +
                            "\n\n" +
                            "Try the inline mode by clicking the button below!" +
                            "";

            execute(new SendMessage()
                    .setChatId(message.getChatId())
                    .setText(markdown)
                    .setParseMode(ParseMode.MARKDOWN)
                    .setReplyMarkup(new InlineKeyboardMarkup()
                            .setKeyboard(singletonList(singletonList(
                                    new InlineKeyboardButton()
                                            .setText("Try the inline mode!")
                                            .setSwitchInlineQuery("Have you tried @" + getBotUsername() + "?")
                            )))));

        } else  {

            String parameter = commandParts[1];

            if (parameter.equals(HELP_INLINE_START_PARAM)) {

                final UserProfile profile = getProfile(message.getFrom());

                final LangConfig langConfigInline = profile.getLangConfigs().get(SpecialLangConfig.inline.name());

                final String markdown =
                        "**Inline mode**" +
                        "\n\n" +
                        "While talking to other people, type `@" + getBotUsername() + "` followed by your message, " +
                        "and finish it with punctuation mark or emoji to see the results." +
                        "\n\n" +
                        "The results let you to send the original message, the translated message or both." +
                        "\n\n" +
                        "See the `." + SpecialLangConfig.inline + "` configuration in " + commandText.get(Command.langconfig) + "." +
                        "\n\n" +
                        "You also choose the languages for a single message (without affecting the default setting), " +
                        "by typing, for example, `@" + getBotUsername() + " .en.es Translate this to Spanish.`" +
                        "";

                sendMessage(message, markdown);

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
                .setResults(buildResult(null, title, markdown, id)));
    }

    private InlineQueryResultArticle buildResult(String thumbUrl, String title, String markdownText, String resultId) {
        return new InlineQueryResultArticle()
                .setId(resultId)
                .setTitle(title)
                .setDescription(markdownText)
                .setThumbUrl(thumbUrl)
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
}
