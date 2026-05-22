package com.vanthucac.user.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserException extends BusinessException {

    public UserException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static UserException userNotFound() {
        return new UserException(
                "User not found",
                UserErrorCode.USER_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
    }
}