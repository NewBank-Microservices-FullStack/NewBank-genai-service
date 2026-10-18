package com.newbank.microservices.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class NewBankChatClient {

    private static final Logger LOG = LoggerFactory.getLogger(NewBankChatClient.class);

    private final ChatClient chatClient;

    public NewBankChatClient(ChatClient.Builder builder, ChatMemory chatMemory, BankTools bankTools) {
        this.chatClient = builder
            .defaultSystem("""
                You are a helpful NewBank AI assistant for a digital banking platform.
                Your job is to help with customer onboarding, account management, advisory support,
                and transaction-related queries. Respond professionally, clearly, and concisely.
                If you do not know the answer, state that you do not know and ask one clarifying question.
                If you do know the answer, provide it without extra follow-up questions.
                For advisor-related requests, explain that results may be limited by search criteria.
                For customer, account, and transaction questions, answer based on the available data.
                """)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .order(10)
                    .build(),
                new SimpleLoggerAdvisor())
            .defaultTools(bankTools)
            .build();
    }

    @PostMapping("/chatclient")
    public String exchange(@RequestBody String query) {
        try {
            return this.chatClient
                .prompt()
                .user(query)
                .call()
                .content();
        }
        catch (Exception exception) {
            LOG.error("Error processing chat message", exception);
            return "Chat is currently unavailable. Please try again later.";
        }
    }
}
