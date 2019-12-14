package com.codethen.yandex;

import com.codethen.yandex.model.YandexLangsResponse;
import com.codethen.yandex.model.YandexResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YandexApi {

  @GET("tr.json/translate")
  Call<YandexResponse> translate(
          @Query("key") String key,
          @Query("text") String text,
          @Query("lang") String lang);

  @GET("tr.json/getLangs")
  Call<YandexLangsResponse> getLangs(
          @Query("key") String key);
}