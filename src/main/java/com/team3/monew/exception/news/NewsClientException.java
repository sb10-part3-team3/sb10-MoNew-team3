package com.team3.monew.exception.news;

public class NewsClientException extends RuntimeException {

  private final boolean retryable;

  public NewsClientException(String message, boolean retryable) {
    super(message);
    this.retryable = retryable;
  }

  public boolean isRetryable() {
    return retryable;
  }
}
