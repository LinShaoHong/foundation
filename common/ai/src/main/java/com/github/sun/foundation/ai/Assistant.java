package com.github.sun.foundation.ai;

import com.github.sun.foundation.ai.qwen.QwenAssistant;
import com.github.sun.foundation.boot.InjectionProvider;

import java.util.List;

public interface Assistant {
  List<String> chat(String apiKey, String model, List<String> q);

  class Provider implements InjectionProvider {
    @Override
    public void config(Binder binder) {
      binder.named("qwen").bind(new QwenAssistant());
    }
  }
}