package com.github.sun.foundation.ai.doubao;

import com.github.sun.foundation.ai.Assistant;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;

import java.util.ArrayList;
import java.util.List;

public class DouBaoAssistant extends Assistant.Basic {
    @Override
    public String fetch() {
        ArkService service = ArkService.builder()
                .apiKey(apiKey)
                .build();
        List<ChatMessage> messages = new ArrayList<>();

        systemMessages.forEach(m -> {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(m)
                    .build());
        });

        userMessages.forEach(m -> {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(m)
                    .build());
        });

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .build();

        List<ChatCompletionChoice> choices = service.createChatCompletion(chatCompletionRequest).getChoices();
        service.shutdownExecutor();
        return choices.isEmpty() ? null : (String) choices.get(0).getMessage().getContent();
    }
}
