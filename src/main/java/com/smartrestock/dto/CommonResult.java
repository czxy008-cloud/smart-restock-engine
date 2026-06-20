package com.smartrestock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResult<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(200, "success", data);
    }

    public static <T> CommonResult<T> error(String message) {
        return new CommonResult<>(500, message, null);
    }

    public static <T> CommonResult<T> error(Integer code, String message) {
        return new CommonResult<>(code, message, null);
    }
}
