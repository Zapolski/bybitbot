package by.zapolski.bybitbot.service;

import static com.bybit.api.client.security.HmacSHA256Signer.sign;

import by.zapolski.bybitbot.service.dto.PositionInfoResponse;
import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.domain.CategoryType;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class PositionService {

    private final RestTemplate positionRestTemplate;    

    @Value("${trade.api-key}")
    private String apiKey;

    @Value("${trade.api-secret}")
    private String apiSecret;

    @Value("${trade.symbol}")
    private String symbol;

    public PositionInfoResponse getPositionInfo() {
        Map<String, Object> uriVariables = Map.of(
                "category", CategoryType.LINEAR.getCategoryTypeId(),
                "symbol", symbol
        );

        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String recvWindow = "5000";
        String signature = sign(apiKey, apiSecret, "", timestamp, recvWindow);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-BAPI-TIMESTAMP", timestamp);
        headers.set("X-BAPI-SIGN", signature);
        headers.set("X-BAPI-API-KEY", apiKey);
        headers.set("X-BAPI-RECV-WINDOW", recvWindow);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        return positionRestTemplate.exchange(
                BybitApiConfig.DEMO_TRADING_DOMAIN + "/v5/position/list",
                HttpMethod.GET,
                requestEntity,
                PositionInfoResponse.class,
                uriVariables
        ).getBody();
    }
   
}