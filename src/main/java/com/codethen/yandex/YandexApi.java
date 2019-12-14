package com.codethen.yandex;

import com.codethen.yandex.model.GetLangsResponse;
import com.codethen.yandex.model.TranslateResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YandexApi {

  @GET("tr.json/translate")
  Call<TranslateResponse> translate(
          @Query("key") String key,
          @Query("text") String text,
          @Query("lang") String lang);

  @GET("tr.json/getLangs")
  Call<GetLangsResponse> getLangs(
          @Query("key") String key);
}