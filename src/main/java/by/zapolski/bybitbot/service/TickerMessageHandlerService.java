package by.zapolski.bybitbot.service;

import static jdk.dynalink.linker.support.Guards.isNotNull;

import com.bybit.api.client.domain.websocket_message.public_channel.PublicTickerData;
import com.bybit.api.client.domain.websocket_message.public_channel.WebSocketTickerMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
//@Service
public class TickerMessageHandlerService {

    @Value("${trade.mark-price.pulling.delay}")
    private Duration pullingDelay;

    private LocalDateTime lastTickTs;

    @SneakyThrows
    public void tickerMessageHandle(String message) {
        WebSocketTickerMessage tickerData = (new ObjectMapper()).readValue(message, WebSocketTickerMessage.class);
        String markPrice = Optional.ofNullable(tickerData)
                .map(WebSocketTickerMessage::getData)
                .map(PublicTickerData::getMarkPrice)
                .orElse(null);
        BigDecimal currentMarkPrice = StringUtils.hasText(markPrice) ? new BigDecimal(markPrice) : null;
        if (currentMarkPrice == null) {
            return;
        }
        
        
        
        
        LocalDateTime now = LocalDateTime.now();
        if (lastTickTs.plus(pullingDelay).isBefore(now)) {
            log.info("Mark price: {}", currentMarkPrice);
            lastTickTs = now;
        }
    }

    @PostConstruct
    private void setUp() {
        this.lastTickTs = LocalDateTime.now();
    }
}