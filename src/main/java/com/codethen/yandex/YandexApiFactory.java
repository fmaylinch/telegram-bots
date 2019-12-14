package com.codethen.yandex;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class YandexApiFactory {

    public static YandexApi build() {

        return new Retrofit.Builder()
                .baseUrl("https://translate.yandex.net/api/v1.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YandexApi.class);
    }
}
