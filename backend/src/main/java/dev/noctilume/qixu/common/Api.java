package dev.noctilume.qixu.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public final class Api {
  private Api() {}

  public static Object ok(Object data, HttpServletRequest request) {
    return Map.of("data", data, "requestId", request.getAttribute("requestId"));
  }

  public static Object error(String code, String message, HttpServletRequest request) {
    return Map.of(
        "error",
        Map.of("code", code, "message", message),
        "requestId",
        request.getAttribute("requestId"));
  }
}
