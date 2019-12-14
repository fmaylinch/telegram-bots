package com.codethen.yandex;

import com.codethen.telegram.lanxatbot.translate.DetectResponse;
import com.codethen.yandex.model.GetLangsResponse;
import com.codethen.yandex.model.TranslateResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YandexApi {

  /** https://tech.yandex.com/translate/doc/dg/reference/translate-docpage */
  @GET("tr.json/translate")
  Call<TranslateResponse> translate(
          @Query("key") String key,
          @Query("text") String text,
          @Query("lang") String lang);

  /** https://tech.yandex.com/translate/doc/dg/reference/detect-docpage/ */
  @GET("tr.json/detect")
  Call<DetectResponse> detect(
          @Query("key") String key,
          @Query("text") String text,
          @Query("hint") String hint);

  /** https://tech.yandex.com/translate/doc/dg/reference/getLangs-docpage/ */
  @GET("tr.json/getLangs")
  Call<GetLangsResponse> getLangs(
          @Query("key") String key);
}