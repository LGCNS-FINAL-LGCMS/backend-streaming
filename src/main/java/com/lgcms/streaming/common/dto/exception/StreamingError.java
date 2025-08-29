package com.lgcms.streaming.common.dto.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StreamingError implements ErrorCodeInterface {
    ENCODING_ERROR("STRE-01","인코딩에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR)
    ;

    private final String status;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.builder()
            .status(status)
            .message(message)
            .httpStatus(httpStatus)
            .build();
    }
}
