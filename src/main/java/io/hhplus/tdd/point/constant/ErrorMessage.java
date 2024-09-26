package io.hhplus.tdd.point.constant;

public enum ErrorMessage {

    POINT_NOT_FOUND("포인트가 없습니다. user id: %d"),
    INVALID_CHARGE_AMOUNT("충전할 수 없는 금액입니다. amount: %d"),
    NEGATIVE_ID_ERROR("음수 아이디는 사용할 수 없습니다. id: %d"),
    NEGATIVE_AMOUNT_ERROR("충전할 수 없는 금액입니다. amount: %d"),
    INSUFFICIENT_BALANCE("사용자의 포인트 잔액이 부족합니다. id: %d, 잔액: %d"),
    IN_VALID_TRANSACTION_TYPE("유효하지 않은 거래유형 입니다. transactionType: %d")
    ;

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}