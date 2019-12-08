package com.codethen.telegram.lanxatbot.profile;

import java.util.Date;
import java.util.Map;

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
    private Integer id;
    /** When the profile was created */
    private Date created;
    /** Yandex API key that will be used to translate */
    private String yandexApiKey;
    /** Language configurations that the user can use */
    private Map<String, LangConfig> langConfigs;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Map<String, LangConfig> getLangConfigs() {
        return langConfigs;
    }

    public void setLangConfigs(Map<String, LangConfig> langConfigs) {
        this.langConfigs = langConfigs;
    }

    public String getYandexApiKey() {
        return yandexApiKey;
    }

    public void setYandexApiKey(String yandexApiKey) {
        this.yandexApiKey = yandexApiKey;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", created=" + created +
                ", langConfigs=" + langConfigs +
                '}';
    }
}
