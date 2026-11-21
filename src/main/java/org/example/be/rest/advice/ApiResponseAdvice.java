package org.example.be.rest.advice;

import org.example.be.core.util.ApiRes;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Lets controllers return ApiRes directly and still set the real HTTP status
 * from ApiRes.status, instead of wrapping every response in ResponseEntity.
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<ApiRes<?>> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiRes.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public ApiRes<?> beforeBodyWrite(ApiRes<?> body, MethodParameter returnType, MediaType selectedContentType,
                                      Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                      ServerHttpRequest request, ServerHttpResponse response) {
        response.setStatusCode(HttpStatusCode.valueOf(body.getStatus()));
        return body;
    }

}
