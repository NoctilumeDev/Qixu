package dev.noctilume.qixu.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@RestControllerAdvice
public class ApiErrors {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(DomainException.class)
    ResponseEntity<Object> domain(DomainException e, HttpServletRequest r) {
        return ResponseEntity.status(e.status()).body(Api.error(e.code(),e.getMessage(),r));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<Object> invalid(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(422).body(Api.error("INVALID_INPUT","请检查填写内容和时间格式。",r));
    }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Object> database(DataAccessException e, HttpServletRequest r) {
        return ResponseEntity.status(503).body(Api.error("DATABASE_UNAVAILABLE","数据服务暂时不可用，请稍后重试。",r));
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Object> missing(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(404).body(Api.error("NOT_FOUND","请求的内容不存在。",r));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Object> method(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(405).body(Api.error("METHOD_NOT_ALLOWED","该操作不支持此请求方式。",r));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception e, HttpServletRequest r) {
        LOG.error("Unexpected API error; requestId={}",r.getAttribute("requestId"),e);
        return ResponseEntity.status(500).body(Api.error("INTERNAL_ERROR","服务暂时出现问题，请稍后重试。",r));
    }
}
