package com.github.sun.foundation.ai.qwen;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import com.github.sun.foundation.ai.Assistant;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QwenAssistant extends Assistant.Basic {
    private final Generation gen;

    public QwenAssistant() {
        PooledDashScopeObjectFactory pooledDashScopeObjectFactory =
                new PooledDashScopeObjectFactory();
        GenericObjectPoolConfig<Generation> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(32);
        config.setMinIdle(32);
        try (GenericObjectPool<Generation> generationPool =
                     new GenericObjectPool<>(pooledDashScopeObjectFactory, config);) {
            this.gen = generationPool.borrowObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String fetch() {
        Constants.apiKey = apiKey;
        List<Message> messages = new ArrayList<>();

        systemMessages.forEach(m -> {
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(m)
                    .build());
        });

        userMessages.forEach(m -> {
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(m)
                    .build());
        });

        GenerationParam param = GenerationParam.builder().model(model)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE).topP(0.8).enableSearch(false)
                .build();
        try {
            GenerationResult result = gen.call(param);
            List<String> resp = result.getOutput().getChoices().stream()
                    .map(v -> v.getMessage().getContent())
                    .collect(Collectors.toList());
            return resp.isEmpty() ? null : resp.get(0);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class PooledDashScopeObjectFactory extends BasePooledObjectFactory<Generation> {
        @Override
        public Generation create() {
            return new Generation();
        }

        @Override
        public PooledObject<Generation> wrap(Generation obj) {
            return new DefaultPooledObject<>(obj);
        }
    }
}