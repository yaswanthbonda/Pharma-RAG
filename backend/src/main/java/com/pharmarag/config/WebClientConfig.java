package com.pharmarag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    @Value("${anthropic.api.base-url}")
    private String anthropicBaseUrl;

    @Value("${anthropic.api.version}")
    private String anthropicVersion;

    @Value("${chroma.host}")
    private String chromaHost;

    @Value("${chroma.port}")
    private int chromaPort;

    @Bean("anthropicWebClient")
    public WebClient anthropicWebClient() {
        return WebClient.builder()
                .baseUrl(anthropicBaseUrl)
                .defaultHeader("x-api-key", anthropicApiKey)
                .defaultHeader("anthropic-version", anthropicVersion)
                .defaultHeader("content-type", "application/json")
                .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Bean("chromaWebClient")
    public WebClient chromaWebClient() {
        return WebClient.builder()
                .baseUrl("http://" + chromaHost + ":" + chromaPort)
                .defaultHeader("content-type", "application/json")
                .build();
    }

    @Bean("openFdaWebClient")
    public WebClient openFdaWebClient(@Value("${openfda.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
