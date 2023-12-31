package spharos.user.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import spharos.user.global.common.response.ResponseCode;

@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {

    private final ResponseCode responseCode;

}
