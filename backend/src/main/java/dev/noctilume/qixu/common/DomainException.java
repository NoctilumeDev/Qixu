package dev.noctilume.qixu.common;

public final class DomainException extends RuntimeException {
  private final int status;
  private final String code;

  public DomainException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static DomainException unauthorized() {
    return new DomainException(401, "SESSION_REQUIRED", "请重新登录后继续。");
  }

  public static DomainException forbidden() {
    return new DomainException(403, "SCOPE_FORBIDDEN", "当前身份没有这项操作权限。");
  }

  public static DomainException missing() {
    return new DomainException(404, "RESOURCE_NOT_FOUND", "记录不存在或已不可访问。");
  }

  public static DomainException invalid(String message) {
    return new DomainException(422, "INVALID_INPUT", message);
  }
}
