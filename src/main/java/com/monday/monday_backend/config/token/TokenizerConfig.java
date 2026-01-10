package com.monday.monday_backend.config.token;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenizerConfig {

    @Bean
    public Encoding gptEncoding() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        return registry.getEncoding(EncodingType.CL100K_BASE);
    }

}
