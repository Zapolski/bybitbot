package by.zapolski.bybitbot.constant;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PositionSide {
    BUY("Buy"),
    SELL("Sell");

    @JsonValue
    private final String transactionSide;

    PositionSide(String transactionSide) {
        this.transactionSide = transactionSide;
    }
    
    public PositionSide reverse() {
        if (PositionSide.values().length > 2) {
            throw new IllegalArgumentException("There are more than 2 elements in the enum. It is impossible to reverse");
        }
        return PositionSide.BUY.equals(this) 
                ? PositionSide.SELL
                : PositionSide.BUY;
    }
}