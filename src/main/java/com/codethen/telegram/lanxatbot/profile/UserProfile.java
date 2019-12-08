package com.codethen.telegram.lanxatbot.profile;

import com.codethen.telegram.lanxatbot.LanXatTelegramBot;

/**
 * Profile settings for a user, like languages to use by default and so on.
 *
 * I think that this class must be Java to work with Spring MongoDB ORM.
 *
 * TODO: Group from/to languages in one object.
 * TODO: langFrom/To should be named inlineLangFrom/To or sth similar
 */
public class UserProfile {

    /** Telegram userId */
    private Integer userId;
    /** Source lang for inline translations */
    private String langFrom;
    /** Target lang for inline translations */
    private String langTo;
    /** Source lang for bot chat translations */
    private String langOtherFrom;
    /** Target lang for bot chat translations */
    private String langOtherTo;
    /** Yandex API key that will be used to translate */
    private String yandexApiKey;

    public UserProfile(Integer userId, String langFrom, String langTo, String langOtherFrom, String langOtherTo, String yandexApiKey) {
        this.userId = userId;
        this.langFrom = langFrom;
        this.langTo = langTo;
        this.langOtherFrom = langOtherFrom;
        this.langOtherTo = langOtherTo;
        this.yandexApiKey = yandexApiKey;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getLangFrom() {
        return langFrom;
    }

    public void setLangFrom(String langFrom) {
        this.langFrom = langFrom;
    }

    public String getLangTo() {
        return langTo;
    }

    public void setLangTo(String langTo) {
        this.langTo = langTo;
    }

    public String getLangOtherFrom() {
        return langOtherFrom;
    }

    public void setLangOtherFrom(String langOtherFrom) {
        this.langOtherFrom = langOtherFrom;
    }

    public String getLangOtherTo() {
        return langOtherTo;
    }

    public void setLangOtherTo(String langOtherTo) {
        this.langOtherTo = langOtherTo;
    }

    public String getYandexApiKey() {
        return yandexApiKey;
    }

    public void setYandexApiKey(String yandexApiKey) {
        this.yandexApiKey = yandexApiKey;
    }

    /** TODO: this is also done in {@link LanXatTelegramBot.TranslationRequest#getLangs()} */
    public String langs() {
        return langFrom + "-" + langTo;
    }

    /** TODO: this is also done in {@link LanXatTelegramBot.TranslationRequest#getLangs()} */
    public String langsOther() {
        return langOtherFrom + "-" + langOtherTo;
    }

    @Override
    public String toString() {
        return "UserProfileJava{" +
                "userId=" + userId +
                ", langFrom='" + langFrom + '\'' +
                ", langTo='" + langTo + '\'' +
                ", langOtherFrom='" + langOtherFrom + '\'' +
                ", langOtherTo='" + langOtherTo + '\'' +
                '}';
    }
}
