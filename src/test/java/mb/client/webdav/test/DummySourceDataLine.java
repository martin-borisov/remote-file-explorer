package mb.client.webdav.test;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class DummySourceDataLine implements SourceDataLine {
    
    private static final Logger LOG = Logger.getLogger(DummySourceDataLine.class.getName());
    
    private boolean open, running;
    private List<LineListener> listeners = new ArrayList<LineListener>();

    @Override
    public void drain() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void start() {
        running = true;
        sendEvent(new LineEvent(this, LineEvent.Type.START, 0));
    }

    @Override
    public void stop() {
        running = false;
        sendEvent(new LineEvent(this, LineEvent.Type.STOP, 0));
    }
    
    @Override
    public void open() throws LineUnavailableException {
        open = true;
        sendEvent(new LineEvent(this, LineEvent.Type.OPEN, 0));
    }

    @Override
    public void close() {
        open = false;
        sendEvent(new LineEvent(this, LineEvent.Type.CLOSE, 0));
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isActive() {
        return open;
    }

    @Override
    public AudioFormat getFormat() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getBufferSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int available() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFramePosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLongFramePosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getMicrosecondPosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public javax.sound.sampled.Line.Info getLineInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public Control[] getControls() {
        return new Control[]{};
    }

    @Override
    public boolean isControlSupported(Type control) {
        return false;
    }

    @Override
    public Control getControl(Type control) {
        return null;
    }

    @Override
    public void addLineListener(LineListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeLineListener(LineListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        open();
    }

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
        open();
    }

    @Override
    public int write(byte[] b, int off, int len) {
        LOG.info(MessageFormat.format("{0} bytes written", len));
        return len;
    }
    
    final void sendEvent(LineEvent event) {
        for (LineListener listener : listeners) {
            listener.update(event);
        }
    }

}
