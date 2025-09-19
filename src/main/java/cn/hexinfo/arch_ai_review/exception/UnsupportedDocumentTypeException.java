package cn.hexinfo.arch_ai_review.exception;

public class UnsupportedDocumentTypeException extends RuntimeException {
    
    public UnsupportedDocumentTypeException(String message) {
        super(message);
    }
    
    public UnsupportedDocumentTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
