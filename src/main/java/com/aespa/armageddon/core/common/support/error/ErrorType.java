package com.aespa.armageddon.core.common.support.error;

import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

public enum ErrorType {

    // 기본 에러 발생
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "예기치 않은 오류가 발생했습니다.", LogLevel.ERROR),

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, ErrorCode.C002, "잘못된 입력값입니다.", LogLevel.WARN),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.C003, "지원하지 않는 HTTP 메서드입니다.", LogLevel.WARN),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, ErrorCode.C004, "잘못된 타입입니다.", LogLevel.WARN),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, ErrorCode.C005, "필수 요청 파라미터가 누락되었습니다.", LogLevel.WARN),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.U001, "사용자를 찾을 수 없습니다.", LogLevel.WARN),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, ErrorCode.U002, "이미 사용 중인 이메일입니다.", LogLevel.WARN),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, ErrorCode.U003, "이미 사용 중인 아이디입니다.", LogLevel.WARN),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, ErrorCode.U004, "비밀번호가 올바르지 않습니다.", LogLevel.WARN),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, ErrorCode.A001, "인증이 필요합니다.", LogLevel.WARN),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, ErrorCode.A002, "접근이 거부되었습니다.", LogLevel.WARN),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, ErrorCode.A003, "아이디 또는 비밀번호가 올바르지 않습니다.", LogLevel.WARN),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, ErrorCode.A004, "세션이 만료되었습니다.", LogLevel.WARN),
    INVALID_PASSWORD_RESET_CODE(HttpStatus.BAD_REQUEST, ErrorCode.A005, "비밀번호 재설정 코드가 유효하지 않거나 만료되었습니다.", LogLevel.WARN),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, ErrorCode.A006, "이메일 인증이 필요합니다.", LogLevel.WARN),
    INVALID_EMAIL_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, ErrorCode.A007, "이메일 인증 코드가 유효하지 않거나 만료되었습니다.", LogLevel.WARN),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, ErrorCode.A008, "기존 비밀번호와 동일한 비밀번호로는 변경할 수 없습니다.", LogLevel.WARN),

    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.T001, "거래내역을 찾을 수 없습니다.", LogLevel.WARN)

    ;

    private final HttpStatus status;

    private final ErrorCode code;

    private final String message;

    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode code, String message, LogLevel logLevel) {

        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

}
