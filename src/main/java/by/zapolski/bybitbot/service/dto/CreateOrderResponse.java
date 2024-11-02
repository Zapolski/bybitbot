package by.zapolski.bybitbot.service.dto;


import lombok.Data;

@Data
public class CreateOrderResponse {

    private String retMsg;
    private Result result;


    @Data
    public static class Result {
        private String orderId;
        private String orderLinkId;
    }
}