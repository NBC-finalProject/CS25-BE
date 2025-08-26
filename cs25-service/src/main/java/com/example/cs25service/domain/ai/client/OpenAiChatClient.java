package com.example.cs25service.domain.ai.client;

import com.example.cs25service.domain.ai.exception.AiException;
import com.example.cs25service.domain.ai.exception.AiExceptionCode;
import com.example.cs25service.domain.ai.resilience.AiResilience;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class OpenAiChatClient implements AiChatClient {

    private final ChatClient openAiChatClient;
    private final AiResilience resilience;

    public OpenAiChatClient(
        @Qualifier("openAiChatModelClient") ChatClient openAiChatClient,
        AiResilience resilience) {
        this.openAiChatClient = openAiChatClient;
        this.resilience = resilience;
    }

    @Override
    public String call(String systemPrompt, String userPrompt) {
        return resilience.executeSync("openai", () ->
            openAiChatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content()
                .trim()
        );
    }

    @Override
    public ChatClient raw() {
        return openAiChatClient;
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userPrompt) {
        return resilience.executeStream("openai", () ->
            openAiChatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
        ).onErrorMap(e -> new AiException(AiExceptionCode.INTERNAL_SERVER_ERROR));
    }
}
