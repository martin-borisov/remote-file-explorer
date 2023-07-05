package mb.client.webdav.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import mb.client.webdav.media.audio.AudioPlayer;
import mb.client.webdav.media.audio.AudioPlayerListener;
import mb.client.webdav.media.audio.AudioSystemWrapper;

public class AudioPlayerTest {
    
    @Before
    public void setup() {
        AudioSystemWrapper.setDefaultSourceDataLine(new DummySourceDataLine());
    }

    @Test
    public void verifyOpenAndPlay() throws Exception {
        TestAudioPlayerListener listener = new TestAudioPlayerListener();
        AudioPlayer player = createAndOpenAudioPlayer(listener);
        
        player.play();
        assertEquals(1, listener.openCallCount);
        assertEquals(1, listener.startCallCount);
    }
    
    @Test
    public void verifyPauseAndResume() throws Exception {
        TestAudioPlayerListener listener = new TestAudioPlayerListener();
        AudioPlayer player = createAndOpenAudioPlayer(listener);
        
        player.play();
        assertEquals(1, listener.openCallCount);

        player.pause();
        assertEquals(1, listener.pauseCallCount);
        
        player.resume();
        assertEquals(1, listener.resumeCallCount);
    }
    
    @Test
    public void verifyProgressAndStop() throws Exception {
        TestAudioPlayerListener listener = new TestAudioPlayerListener();
        AudioPlayer player = createAndOpenAudioPlayer(listener);
        player.play();
        assertEquals(1, listener.openCallCount);
        
        // Have to wait for some data to be processed
        Thread.sleep(1000);
        assertTrue(listener.progressCallCount > 0);
        
        player.stop();
        assertTrue(listener.stopCallCount > 0);
    }
    
    private static File loadDefaultAudioFile() throws URISyntaxException {
        URL resource = AudioPlayerTest.class.getResource("/click-track.mp3");
        assertNotNull("Test file not found", resource);
        return new File(resource.toURI());
    }
    
    private static AudioPlayer createAndOpenAudioPlayer(AudioPlayerListener listener) throws Exception {
        AudioPlayer player = new AudioPlayer();
        player.addListener(listener);
        player.open(loadDefaultAudioFile());
        return player;
    }
    
    private static class TestAudioPlayerListener extends AudioPlayerListener {
        public int openCallCount, startCallCount, pauseCallCount, resumeCallCount, progressCallCount,
            stopCallCount;
        public void onOpen(Map<String, Object> properties) {
            openCallCount++;
        }
        public void onStart() {
            startCallCount++;
        }
        public void onPause() {
            pauseCallCount++;
        }
        public void onResume() {
            resumeCallCount++;
        }
        public void onProgress(int elapsedSeconds) {
            progressCallCount++;
        }
        public void onStop() {
            stopCallCount++;
        }
        
        
    }
}
