package com.github.sun.foundation.ai;

import com.github.sun.foundation.ai.qwen.QwenAssistant;
import com.github.sun.foundation.boot.InjectionProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface Assistant {
  String chat(String apiKey, String model, List<String> q);

  default String chat(String apiKey, String model, String... q) {
    return chat(apiKey, model, Arrays.asList(q));
  }

  default String chat(String apiKey, String model, String q) {
    return chat(apiKey, model, Collections.singletonList(q));
  }

  class Provider implements InjectionProvider {
    @Override
    public void config(Binder binder) {
      binder.named("qwen").bind(new QwenAssistant());
    }
  }
}