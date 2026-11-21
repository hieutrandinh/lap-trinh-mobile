package org.example.be.core.util;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiRes<T> {

    int status;
    T data;

    @JsonValue
    public T getData() {
        return data;
    }

    public static <T> ApiRes<T> success(int status) {
        return new ApiRes<>(status, null);
    }

    public static <T> ApiRes<T> success(int status, T data) {
        return new ApiRes<>(status, data);
    }

}
