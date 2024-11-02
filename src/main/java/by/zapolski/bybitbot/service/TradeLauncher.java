package by.zapolski.bybitbot.service;

import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.BybitApiAsyncMarketDataRestClient;
import com.bybit.api.client.restApi.BybitApiAsyncTradeRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.bybit.api.client.websocket.httpclient.WebsocketStreamClient;
import java.util.Map;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
//@Component
@RequiredArgsConstructor
public class TradeLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private final BybitApiAsyncMarketDataRestClient bybitApiAsyncMarketDataRestClient;
    private final WebsocketStreamClient websocketStreamClient;
    private final BybitApiAsyncTradeRestClient apiAsyncTradeRestClient;

    @Value("${trade.api-key}")
    String apiKey;

    @Value("${trade.api-secret}")
    String apiSecret;

    @Override
    @SneakyThrows
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        //websocketStreamClient.getPublicChannelStream(List.of("tickers.BTCUSDT"), BybitApiConfig.V5_PUBLIC_LINEAR);

        // создание ордера
        Map<String, Object> order =Map.of(
                "category", CategoryType.LINEAR.getCategoryTypeId(),
                "symbol", "BTCUSDT",
                "side", "Buy",
                "orderType", "Market",
                "qty", "0.016",
                "positionIdx", "1"
        );
        apiAsyncTradeRestClient.createOrder(order, System.out::println);

//        BybitApiTradeRestClient bybitApiTradeRestClient =
//                BybitApiClientFactory.newInstance(apiKey, apiSecret, BybitApiConfig.DEMO_TRADING_DOMAIN).newTradeRestClient();
//        Object result = bybitApiTradeRestClient.getOpenOrders(TradeOrderRequest.builder()
//                .category(CategoryType.LINEAR)
//                .symbol("ALTUSDT")
//                //.openOnly(1)
//                .build());
//        log.info(result.toString());

        Queue<Integer> fifo = new CircularFifoQueue<Integer>(2);
        fifo.add(1);
        fifo.add(2);
        fifo.add(3);
        System.out.println(fifo);
        
        fifo.forEach(System.out::println);
    }
}