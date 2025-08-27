package com.owmetagg.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import com.owmetagg.dtos.PlayerDTO;
import com.owmetagg.configurations.RabbitMQConfig;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OverFastService implements InitializingBean {

    private static final int DEFAULT_RATE_LIMIT_DELAY_MS = 200; // 5 requests per second
    private static final int API_TIMEOUT_MS = 30000; // 30 seconds
    private static final String TIMESTAMP_HEADER = "fetch-timestamp";

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;
    private final RestTemplate restTemplate;

    @Value("${overfast.api.base-url:https://overfast-api.tekrop.fr}")
    private String overfastApiUrl;

    @Value("${overfast.api.rate-limit.requests-per-second:5}")
    private int rateLimitRequestsPerSecond;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private long lastApiCallTimestamp = 0;

    public OverFastService(
            RabbitTemplate rabbitTemplate,
            RabbitMQConfig rabbitMQConfig,
            RestTemplate restTemplate
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMQConfig = rabbitMQConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        initializeService();
    }

    private void initializeService() {
        try {
            log.info("Initializing OverFastService");
            log.info("OverFast API URL: {}", overfastApiUrl);
            log.info("Rate limit: {} requests/second", rateLimitRequestsPerSecond);
            log.info("Active profile: {}", activeProfile);

            // Test API connectivity
            boolean isHealthy = checkOverFastAPIHealth();
            if (isHealthy) {
                log.info("OverFast API connection test successful");
            } else {
                log.warn("OverFast API connection test failed - service will continue but may have issues");
            }

        } catch (Exception e) {
            log.error("Error initializing OverFastService: {}", e.getMessage(), e);
        }
    }

    /**
     * Fetch single player data and send to RabbitMQ
     */
    public void fetchAndSendPlayerData(String battletag) {
        fetchAndSendPlayerData(battletag, "pc");
    }

    public void fetchAndSendPlayerData(String battletag, String platform) {
        log.info("🎮 Fetching player data for: {} ({})", battletag, platform);

        try {
            // Apply rate limiting
            enforceRateLimit();

            // Fetch player data from API
            String playerData = fetchPlayerFromApi(battletag);

            if (playerData != null && !playerData.isEmpty()) {
                // Process and send to RabbitMQ
                processApiResponse(battletag, platform, playerData);
                log.info("Successfully processed player: {}", battletag);
            } else {
                log.warn("No data received for player: {}", battletag);
            }

        } catch (Exception e) {
            log.error("Failed to process player data for: {}", battletag, e);
        }
    }

    /**
     * Fetch multiple players in batch (like your WavuService batch processing)
     */
    public void fetchAndSendMultiplePlayers(List<String> battletags) {
        log.info("Processing batch of {} players", battletags.size());

        for (int i = 0; i < battletags.size(); i++) {
            String battletag = battletags.get(i);
            try {
                fetchAndSendPlayerData(battletag);

                // Progress logging (like your WavuService)
                if ((i + 1) % 10 == 0 || i == battletags.size() - 1) {
                    log.info("Batch progress: {}/{} players processed", i + 1, battletags.size());
                }

            } catch (Exception e) {
                log.error("Failed to process player in batch: {}", battletag, e);
                // Continue with next player (resilient like your service)
            }
        }

        log.info("Completed batch processing of {} players", battletags.size());
    }

    private String fetchPlayerFromApi(String battletag) {
        try {
            String url = UriComponentsBuilder.fromUriString(overfastApiUrl)
                    .path("/players/{battletag}")
                    .build(battletag)
                    .toString();

            log.debug("Calling OverFast API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<String>() {}
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API request failed with status: " + response.getStatusCode());
            }

            return response.getBody();

        } catch (RestClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("Player not found: {} (404)", battletag);
            } else {
                log.error("OverFast API error for {}: {}", battletag, e.getMessage());
            }
            return null;
        }
    }

    private void processApiResponse(String battletag, String platform, String playerData) {
        log.debug("📊 Processing API response for: {}", battletag);
        long startTime = System.currentTimeMillis();

        // Create message (similar to your Battle processing)
        PlayerDTO message = PlayerDTO.builder()
                .battletag(battletag)
                .platform(platform)
                .rawPlayerData(playerData)
                .build();

        // Send to RabbitMQ (same pattern as your WavuService)
        sendToRabbitMQ(message, getCurrentTimestamp());

        log.debug("📨 Sending data to RabbitMQ took {} ms", (System.currentTimeMillis() - startTime));
    }

    void sendToRabbitMQ(PlayerDTO message, String timestamp) {
        try {
            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getPlayerDataQueue(), // Direct queue name
                    message,
                    msg -> {
                        msg.getMessageProperties()
                                .setHeader(TIMESTAMP_HEADER, timestamp);
                        return msg;
                    }
            );
            log.debug("📨 Sent message to queue: {}", message.getBattletag());
        } catch (Exception e) {
            log.error("📨 Failed to send message to queue: {}", message.getBattletag(), e);
            throw e;
        }
    }

    /**
     * Rate limiting (similar to your backpressure handling)
     */
    private void enforceRateLimit() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCall = currentTime - lastApiCallTimestamp;
        long minInterval = 1000 / rateLimitRequestsPerSecond; // ms between calls

        if (timeSinceLastCall < minInterval) {
            long sleepTime = minInterval - timeSinceLastCall;
            try {
                log.debug("⏱️ Rate limiting: sleeping for {} ms", sleepTime);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ Rate limiting interrupted", e);
            }
        }

        lastApiCallTimestamp = System.currentTimeMillis();
    }

    /**
     * Health check (similar to your API connectivity checks)
     */
    public boolean checkOverFastAPIHealth() {
        try {
            String url = UriComponentsBuilder.fromUriString(overfastApiUrl)
                    .path("/heroes")
                    .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<String>() {}
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("❌ OverFast API health check failed", e);
            return false;
        }
    }

    private String getCurrentTimestamp() {
        return ZonedDateTime.now(ZoneId.of("UTC")).toString();
    }
}
