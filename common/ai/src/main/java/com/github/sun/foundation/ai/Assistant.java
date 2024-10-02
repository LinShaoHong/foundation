package com.github.sun.foundation.ai;

import com.github.sun.foundation.ai.doubao.DouBaoAssistant;
import com.github.sun.foundation.ai.qwen.QwenAssistant;
import com.github.sun.foundation.boot.InjectionProvider;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

public interface Assistant {

    Assistant apiKey(String apiKey);

    Assistant model(String model);

    Assistant systemMessage(String message);

    Assistant userMessage(String message);

    String fetch();

    abstract class Basic implements Assistant {
        protected String apiKey;
        protected String model;
        protected Set<String> systemMessages;
        protected Set<String> userMessages;

        @Override
        public Basic apiKey(String apiKey) {
            this.apiKey = apiKey;
            this.systemMessages = new LinkedHashSet<>();
            this.userMessages = new LinkedHashSet<>();
            return this;
        }

        @Override
        public Basic model(String model) {
            this.model = model;
            this.systemMessages = new LinkedHashSet<>();
            this.userMessages = new LinkedHashSet<>();
            return this;
        }

        @Override
        public Basic systemMessage(String message) {
            if (StringUtils.hasText(message)) {
                this.systemMessages.add(message);
            }
            return this;
        }

        @Override
        public Basic userMessage(String message) {
            if (StringUtils.hasText(message)) {
                this.userMessages.add(message);
            }
            return this;
        }
    }

    class Provider implements InjectionProvider {
        @Override
        public void config(Binder binder) {
            binder.named("qwen").bind(new QwenAssistant());
            binder.named("doubao").bind(new DouBaoAssistant());
        }
    }
}