package mb.client.webdav.media.audio;

import java.util.EventListener;
import java.util.Map;

public abstract class AudioPlayerListener implements EventListener {
    
    public void onOpen(Map<String, Object> properties) {
    }
    
    public void onStart() {
    }
    
    public void onStop() {
    }
    
    public void onClose() {
    }
    
    public void onEndOfMedia() {
    }
    
    public void onProgress(int elapsedSeconds) {
    }
    
    public void onPause() {
    }
    
    public void onResume() {
    }
}
