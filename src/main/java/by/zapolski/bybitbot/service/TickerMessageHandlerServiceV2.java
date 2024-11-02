package by.zapolski.bybitbot.service;

import by.zapolski.bybitbot.constant.PositionSide;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.websocket_message.public_channel.PublicTickerData;
import com.bybit.api.client.domain.websocket_message.public_channel.WebSocketTickerMessage;
import com.bybit.api.client.restApi.BybitApiPositionRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TickerMessageHandlerServiceV2 {

    @Value("${trade.mark-price.pulling.delay}")
    private Duration pullingDelay;

    @Value("${trade.symbol}")
    private String symbol;

    @Value("${trade.take-profit-percent}")
    private Double takeProfitPercent;

    @Value("${trade.stop-loss-percent}")
    private Double stopLossPercent;

    @Value("${trade.usdt-position-value}")
    private Double usdtPositionValue;

    @Value("${trade.position-side:#{null}}")
    private PositionSide positionSide;

    @Value("${trade.position-mark-price:#{null}}")
    private Double positionMarkPrice;

    @Value("${trade.initiate-position-side}")
    private PositionSide initiatePositionSide;

    private final BybitApiTradeRestClient apiTradeRestClient;
    private final ObjectMapper objectMapper;

    private LocalDateTime lastTickTs;
    private boolean isPositionOpen;
    private Double actualMarkPrice;

    @PostConstruct
    private void setUp() {
        this.lastTickTs = LocalDateTime.now();
    }

    @SneakyThrows
    public void tickerMessageHandle(String message) {
        WebSocketTickerMessage tickerData = objectMapper.readValue(message, WebSocketTickerMessage.class);
        String markPrice = Optional.ofNullable(tickerData)
                .map(WebSocketTickerMessage::getData)
                .map(PublicTickerData::getMarkPrice)
                .orElse(null);

        // убеждаемся, что у нас есть хоть одна цена
        // больше надо для инициализации, возможно можно запросом вытянуть, чтобы сэкономить мс в хендлере 
        actualMarkPrice = StringUtils.hasText(markPrice) ? Double.valueOf(markPrice) : actualMarkPrice;
        if (actualMarkPrice == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (lastTickTs.plus(pullingDelay).isBefore(now)) {
            lastTickTs = now;

            if (isPositionOpen) {
                // анализируем дельту
                // по результатам если в плюс, то закрываем позицию и открываем такую же иначе разворачиваем позицию
                Double delta = (actualMarkPrice - positionMarkPrice) / positionMarkPrice * 100L;
                log.info(String.format("Дельта: %s; маркет прайс: %s; маркет прайс позиции: %s; тип позиции %s.", delta, actualMarkPrice, positionMarkPrice,
                        positionSide));

                if (PositionSide.BUY.equals(positionSide)) { // long
                    if (delta > 0) { // дельта положительная для long 
                        if (delta.compareTo(takeProfitPercent) >= 0) {
                            // закрываем позицию, открываем long
                            log.info("Закрываем позицию {}.", positionSide);
                            placeOrderClosePosition(symbol, PositionSide.SELL, positionMarkPrice);
                            log.info("Позиция закрыта.");
                            Thread.sleep(1000);
                            log.info("Открываем позицию.");
                            placeOrderOpenPosition(symbol, positionSide, actualMarkPrice);
                            log.info("Позиция открыта.");
                            positionMarkPrice = actualMarkPrice;
                        }
                    } else { // дельта отрицательная для long
                        delta = -delta;
                        if (delta.compareTo(stopLossPercent) >= 0) {
                            // разворачиваем позицию
                            log.info("Разворачиваем позицию {}", positionSide);
                            placeOrderOpenPosition(symbol, PositionSide.SELL, actualMarkPrice / 2);
                            log.info("Позиция открыта.");
                            positionMarkPrice = actualMarkPrice;
                            positionSide = PositionSide.SELL;
                        }
                    }
                } else { // short
                    if (delta > 0) { // дельта положительная для short 
                        if (delta.compareTo(stopLossPercent) >= 0) {
                            // разворачиваем позицию 
                            log.info("Разворачиваем позицию {}", positionSide);
                            placeOrderOpenPosition(symbol, PositionSide.BUY, actualMarkPrice / 2);
                            log.info("Позиция открыта.");
                            positionMarkPrice = actualMarkPrice;
                            positionSide = PositionSide.BUY;
                        }
                    } else { // дельта отрицательная для short
                        delta = -delta;
                        if (delta.compareTo(takeProfitPercent) >= 0) {
                            // закрываем позицию, открываем short
                            log.info("Закрываем позицию {}.", positionSide);
                            placeOrderClosePosition(symbol, PositionSide.BUY, positionMarkPrice);
                            log.info("Позиция закрыта.");
                            Thread.sleep(1000);
                            log.info("Открываем позицию.");
                            placeOrderOpenPosition(symbol, positionSide, actualMarkPrice);
                            log.info("Позиция открыта.");
                            positionMarkPrice = actualMarkPrice;
                        }
                    }
                }
            } else {
                if (positionSide != null) {
                    isPositionOpen = true;
                } else {
                    log.info("Открываем позицию...");
                    placeOrderOpenPosition(symbol, initiatePositionSide, actualMarkPrice);
                    log.info("Позиция открыта...");
                    positionMarkPrice = actualMarkPrice;
                    positionSide = initiatePositionSide;
                    isPositionOpen = true;
                    log.info(String.format("Дельта: %s; маркет прайс: %s; маркет прайс позиции: %s; тип позиции %s.", 0, actualMarkPrice, positionMarkPrice,
                            positionSide));
                }
            }
        }
    }

    private void placeOrderOpenPosition(String symbol, PositionSide side, Double markPrice) {
        Double qty = usdtPositionValue / markPrice;
        Map<String, Object> order = Map.of(
                "category", CategoryType.LINEAR.getCategoryTypeId(),
                "symbol", symbol,
                "side", side.getTransactionSide(),
                "orderType", "Market",
                "qty", String.format("%.0f", qty)
        );
        Object response = apiTradeRestClient.createOrder(order);
        log.info(response.toString());
        LinkedHashMap<String, Object> linkedHashMap = (LinkedHashMap<String, Object>) response;
        Object retCode = linkedHashMap.get("retCode");
        if (retCode == null || (int) retCode != 0) {
            throw new IllegalArgumentException("Ошибка при открытии позиции!");
        }
    }

    // обратный ордер с количеством больше текущей, reduceOnly = true
    private void placeOrderClosePosition(String symbol, PositionSide side, Double positionMarkPrice) {
        Double qty = usdtPositionValue / positionMarkPrice + 1;
        Map<String, Object> order = Map.of(
                "category", CategoryType.LINEAR.getCategoryTypeId(),
                "symbol", symbol,
                "side", side.getTransactionSide(),
                "orderType", "Market",
                "qty", String.format("%.0f", qty),
                "reduceOnly", true
        );
        Object response = apiTradeRestClient.createOrder(order);
        log.info(response.toString());
        LinkedHashMap<String, Object> linkedHashMap = (LinkedHashMap<String, Object>) response;
        Object retCode = linkedHashMap.get("retCode");
        if (retCode == null || (int) retCode != 0) {
            throw new IllegalArgumentException("Ошибка при закрытии позиции!");
        }
    }
}