package com.jonasjschreiber.fileengine.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(FileStorageException.class)
  public ModelAndView handleException(FileStorageException exception, RedirectAttributes redirectAttributes) {
    ModelAndView mav = new ModelAndView();
    mav.addObject("message", exception.getMsg());
    mav.setViewName("error");
    return mav;
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception,
                                                                   HttpHeaders headers,
                                                                   HttpStatus status,
                                                                   WebRequest request) {
    return handleExceptionInternal(request, exception, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                HttpHeaders headers,
                                                                HttpStatus status,
                                                                WebRequest request) {
    return handleExceptionInternal(request, exception, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<Object> handleAllUncaughtRuntimeException(RuntimeException exception, WebRequest request) {
    return handleExceptionInternal(request, exception, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<Object> handleAllUncaughtException(RuntimeException exception, WebRequest request) {
    return handleExceptionInternal(request, exception, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public ResponseEntity<Object> handleExceptionInternal(WebRequest request, Exception exception, HttpStatus status) {
    log.error("Exception on request: {}", request, exception);
    Map<String, String> body = new HashMap<String, String>() {{
      put("timestamp", Instant.now().toString());
      put("status", Integer.toString(status.value()));
      put("error", status.getReasonPhrase());
      put("path", (String) request.getAttribute("org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping", 0));
      put("message", exception.getMessage());
    }};
    return ResponseEntity.status(status).body(body);
  }

}
