package com.codethen.telegram.lanxatbot.translate;

import javax.annotation.Nullable;
import java.util.List;

public class DetectRequest {

    public String text;
    @Nullable
    public List<String> possibleLangs;
    public String apiKey;
}