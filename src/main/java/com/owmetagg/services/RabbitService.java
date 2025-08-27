package com.owmetagg.services;

import com.owmetagg.dtos.PlayerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.owmetagg.utils.Constants.TIMESTAMP_HEADER;

@Slf4j
@Service
public class RabbitService {

    private final PlayerProcessingService playerProcessingService;

    public RabbitService(PlayerProcessingService playerProcessingService) {
        this.playerProcessingService = playerProcessingService;
    }

    @RabbitListener(
            queues = "#{rabbitMQConfig.playerDataQueue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receivePlayerData(
            PlayerDTO playerMessage,
            @Header(TIMESTAMP_HEADER) String timestamp) {

        log.info("Received player data from RabbitMQ: {}, timestamp: {}",
                playerMessage.getBattletag(), timestamp);

        long start = System.currentTimeMillis();

        try {
            playerProcessingService.processPlayerDataAsync(playerMessage);
        } catch (Exception e) {
            log.error("Failed to process player data for: {}", playerMessage.getBattletag(), e);
            throw new RuntimeException("Failed to process player data", e);
        }

        log.info("Total operation time: {} ms", System.currentTimeMillis() - start);
    }

    // For future batch processing (like your List<Battle> pattern)
    /*
    @RabbitListener(
            queues = "#{rabbitMQConfig.playerBatchQueue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receivePlayerBatch(
            List<PlayerDTO> players,
            @Header(TIMESTAMP_HEADER) String timestamp) {

        log.info("Received {} players from RabbitMQ, timestamp: {}", players.size(), timestamp);

        long start = System.currentTimeMillis();

        try {
            playerProcessingService.processPlayerBatchAsync(players);
        } catch (Exception e) {
            log.error("Failed to process player batch", e);
            throw new RuntimeException("Failed to process player batch", e);
        }

        log.info("Total operation time: {} ms", System.currentTimeMillis() - start);
    }
    */
}