package mb.client.webdav.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.github.sardine.DavResource;

import javafx.application.Platform;
import javafx.scene.media.Media;
import mb.client.webdav.media.MPMedia;
import mb.client.webdav.model.WebDAVHost;
import mb.client.webdav.model.WebDAVResource;

public class WebDAVUtil {

    /**
     * Converts a {@link DavResource} to a {@link WebDAVResource}
     */
    public static WebDAVResource webDAVResourceFromSardineResource(DavResource sardineRes, WebDAVHost host) {
        return new WebDAVResource(sardineRes.getHref().toString(), sardineRes.getName(), sardineRes.getContentType(), sardineRes.getContentLength(), 
                sardineRes.getCreation(), sardineRes.getModified(), sardineRes.isDirectory(), true, host.getBaseURI());
    }
    
    /**
     * Creates a root resource from a {@link WebDAVHost}
     */
    public static WebDAVResource webDAVResourceFromHost(WebDAVHost host) {
        return new WebDAVResource(host.getBaseURI().toString() + host.getRoot(), host.getRoot());
    }
    
    /**
     * Utility to start a task
     * @param task The {@link Runnable} to start
     * @return The same {@link Runnable} returned for convenience
     */
    public static Runnable startTask(Runnable task) {
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        return task;
    }
    
    /**
     * Utility to execute a task after a delay
     */
    public static void startLoadTimer(Runnable task, long delayMs) {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            public void run() {
                Platform.runLater(task);
            }
        }, delayMs);
    }
    
    /**
     * Extracts the message chain from a throwable
     */
    public static List<String> getExceptionMessageChain(Throwable throwable) {
        List<String> result = new ArrayList<String>();
        while (throwable != null) {
            result.add(throwable.getMessage());
            throwable = throwable.getCause();
        }
        return result;
    }
    
    /**
     * Converts {@link WebDAVResource} to {@link Media}
     */
    public static MPMedia mpMediaFromWebDAVResource(WebDAVResource res) {
        MPMedia media = null;
        if(isMedia(res)) {
            media = new MPMedia(res.getName(), res.getBaseURI() + res.getAbsolutePath(), 
                    isAudioMedia(res) ? MPMedia.Type.AUDIO : MPMedia.Type.VIDEO);
        }
        return media;
    }
    
    /**
     * Checks if resource is playable media
     */
    public static boolean isMedia(WebDAVResource res) {
        return isAudioMedia(res) || isVideoMedia(res);
    }
    
    /**
     * Checks if resource is audio media
     */
    public static boolean isAudioMedia(WebDAVResource res) {
        return res.getType().startsWith("audio/") || 
                (res.getType().contains("octet-stream") && res.getAbsolutePath().endsWith("dsf"));
    }
    
    /**
     * Checks if resource is video media
     */
    public static boolean isVideoMedia(WebDAVResource res) {
        return res.getType().startsWith("video/");
    }
    
    /**
     * Splits a path delimited by <code>/</code> in an array of elements
     */
    public static String[] pathToElements(String path) {
        return Arrays.stream(path.split("/"))
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }
    
    /**
     * Properly encodes URI path segments
     */
    public static String encodeUrlPath(String pathSegment) {
        final StringBuilder sb = new StringBuilder();

        try {
            for (int i = 0; i < pathSegment.length(); i++) {
                final char c = pathSegment.charAt(i);

                if (((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || (c == '-')
                        || (c == '.') || (c == '_') || (c == '~')) {
                    sb.append(c);
                } else {
                    final byte[] bytes = String.valueOf(c).getBytes("UTF-8");
                    for (byte b : bytes) {
                        sb.append('%').append(Integer.toHexString((b >> 4) & 0xf)).append(Integer.toHexString(b & 0xf));
                    }
                }
            }

            return sb.toString();
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
