package mb.client.webdav.service;

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
     */
    public static void startTask(Runnable task) {
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
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
     * Splits a path delimited by <code>/</code> in an array of elements.
     */
    public static String[] pathToElements(String path) {
        return Arrays.stream(path.split("/"))
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }
}
