package by.zapolski.bybitbot.service;

import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.restApi.BybitApiAsyncMarketDataRestClient;
import com.bybit.api.client.websocket.httpclient.WebsocketStreamClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeLauncherV2 implements ApplicationListener<ApplicationReadyEvent> {

    private final BybitApiAsyncMarketDataRestClient bybitApiAsyncMarketDataRestClient;
    private final WebsocketStreamClient websocketStreamClient;

    @Value("${trade.api-key}")
    String apiKey;

    @Value("${trade.api-secret}")
    String apiSecret;

    @Value("${trade.symbol}")
    private String symbol;

    @Override
    @SneakyThrows
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        websocketStreamClient.getPublicChannelStream(List.of(String.format("tickers.%s", symbol)), BybitApiConfig.V5_PUBLIC_LINEAR);
    }
}