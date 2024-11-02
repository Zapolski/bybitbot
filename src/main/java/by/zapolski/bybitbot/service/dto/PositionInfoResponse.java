package by.zapolski.bybitbot.service.dto;

import by.zapolski.bybitbot.constant.PositionSide;
import lombok.Data;

@Data
public class PositionInfoResponse {

    private Integer retCode;
    private String retMsg;
    private Result result;


    @Data
    public static class Result {
        private PositionSide side;
        private Double markPrice;
    }
}