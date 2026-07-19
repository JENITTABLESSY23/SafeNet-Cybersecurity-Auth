package com.safenet.dto;

public class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;
    private String  error;

    private ApiResponse() {}

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true; r.data = data; return r;
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true; r.message = message; r.data = data; return r;
    }

    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false; r.error = error; return r;
    }

    public boolean isSuccess() { return success; }
    public String  getMessage(){ return message; }
    public T       getData()   { return data; }
    public String  getError()  { return error; }
}
