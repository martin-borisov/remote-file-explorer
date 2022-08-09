package mb.client.webdav.service;

public class WebDAVServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public WebDAVServiceException() {
        super();
    }

    public WebDAVServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public WebDAVServiceException(String message) {
        super(message);
    }

    public WebDAVServiceException(Throwable cause) {
        super(cause);
    }

}
