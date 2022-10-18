package mb.client.webdav.media.audio;

public class AudioPlayerException extends Exception {
    private static final long serialVersionUID = 1L;

    public AudioPlayerException(String message) {
        super(message);
    }

    public AudioPlayerException(Throwable cause) {
        super(cause);
    }

    public AudioPlayerException(String message, Throwable cause) {
        super(message, cause);
    }
}
