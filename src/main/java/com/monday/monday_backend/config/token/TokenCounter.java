package com.monday.monday_backend.config.token;

import com.knuddels.jtokkit.api.Encoding;
import com.monday.shared.llm.LlmMessage;

import java.util.List;

public class TokenCounter {

    private final Encoding encoding;

    public TokenCounter(Encoding encoding) {
        this.encoding = encoding;
    }

    public long estimateContentTokens(String content) {
        return encoding.countTokens(content);
    }

    /**
     * Approximate chat token counting using the standard "tokens-per-message" overhead
     * pattern for OpenAI-style chat. Provider usage remains canonical post-call.
     */
    public long estimateChatTokens(List<LlmMessage> messages) {
        final int TOKENS_PER_MESSAGE = 3;
        final int TOKENS_REPLY_PRIMER = 3;

        long tokens = 0;
        for (LlmMessage msg : messages) {
            tokens += TOKENS_PER_MESSAGE;
            tokens += encoding.countTokens(msg.role().name().toLowerCase());
            tokens += encoding.countTokens(msg.content());
        }
        tokens += TOKENS_REPLY_PRIMER;
        return tokens;
    }
}
