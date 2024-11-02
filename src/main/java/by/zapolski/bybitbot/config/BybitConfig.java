package by.zapolski.bybitbot.config;

import by.zapolski.bybitbot.service.TickerMessageHandlerServiceV2;
import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.restApi.BybitApiAsyncMarketDataRestClient;
import com.bybit.api.client.restApi.BybitApiPositionRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.bybit.api.client.websocket.httpclient.WebsocketStreamClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class BybitConfig {

    @Value("${trade.api-key}")
    private String apiKey;

    @Value("${trade.api-secret}")
    private String apiSecret;

    @Value("${trade.timeout}")
    private Duration timeout;

    @Bean
    BybitApiPositionRestClient apiPositionRestClient() {
        return BybitApiClientFactory.newInstance().newPositionRestClient();
    }

    @Bean
    BybitApiAsyncMarketDataRestClient asyncMarketDataRestClient() {
        return BybitApiClientFactory.newInstance().newAsyncMarketDataRestClient();
    }

    @Bean
    BybitApiTradeRestClient bybitApiTradeRestClient() {
        return BybitApiClientFactory.newInstance(apiKey, apiSecret, BybitApiConfig.DEMO_TRADING_DOMAIN).newTradeRestClient();
    }

    // даже на демо-торговле важно брать основной стрим, т.к. на демо-стриме почему-то показатели markPrice с отклонениями
    @Bean
    WebsocketStreamClient websocketStreamClient(TickerMessageHandlerServiceV2 messageHandlerServiceV2) {
        var client = BybitApiClientFactory.newInstance(BybitApiConfig.STREAM_MAINNET_DOMAIN, false).newWebsocketClient(2000);
        client.setMessageHandler(messageHandlerServiceV2::tickerMessageHandle);
        return client;
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestTemplate positionRestTemplate() {
        return buildRestTemplate();
    }

    private RestTemplate buildRestTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-BAPI-SIGN", apiSecret)
                .defaultHeader("X-BAPI-API-KEY", apiKey)
                .defaultHeader("X-BAPI-RECV-WINDOW", "5000")
                .build();
    }

}