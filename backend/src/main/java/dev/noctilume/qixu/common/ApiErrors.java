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
import org.springframework.transaction.TransactionSystemException;

@RestControllerAdvice
public class ApiErrors {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<Object> oversized(Exception e,HttpServletRequest r) {
        return ResponseEntity.status(413).body(Api.error("PAYLOAD_TOO_LARGE","照片须不超过1MiB，整个上传请求不超过2MiB。",r));
    }
    @ExceptionHandler(DomainException.class)
    ResponseEntity<Object> domain(DomainException e, HttpServletRequest r) {
        return ResponseEntity.status(e.status()).body(Api.error(e.code(),e.getMessage(),r));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    ResponseEntity<Object> invalid(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(422).body(Api.error("INVALID_INPUT","请检查填写内容和时间格式。",r));
    }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Object> database(DataAccessException e, HttpServletRequest r) {
        return databaseUnavailable(r);
    }
    @ExceptionHandler(TransactionSystemException.class)
    ResponseEntity<Object> transaction(TransactionSystemException e, HttpServletRequest r) {
        // A failed rollback may replace the original commit communications error.
        // Neither exception establishes that the business transaction rolled back.
        Throwable original=e.getOriginalException();
        if(e.contains(java.sql.SQLException.class) || e.contains(DataAccessException.class)
                || original instanceof DataAccessException || original instanceof java.sql.SQLException) {
            return databaseUnavailable(r);
        }
        return unexpected(e,r);
    }
    private ResponseEntity<Object> databaseUnavailable(HttpServletRequest r) {
        return ResponseEntity.status(503).body(Api.error("DATABASE_UNAVAILABLE","数据服务暂时不可用；写入结果可能尚未确认，请通过原请求回执核对。",r));
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
