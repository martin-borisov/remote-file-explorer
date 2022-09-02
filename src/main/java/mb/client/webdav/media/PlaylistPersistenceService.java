package mb.client.webdav.media;

import java.beans.DefaultPersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlaylistPersistenceService {
    
    private static final Logger LOG = 
            Logger.getLogger(PlaylistPersistenceService.class.getName());
    private static final String PATH = "playlist.xml";
    private static PlaylistPersistenceService ref;
    
    public static PlaylistPersistenceService getInstance() {
        synchronized (PlaylistPersistenceService.class) {
            if (ref == null) {
                ref = new PlaylistPersistenceService();
            }
        }
        return ref;
    }
    
    private PlaylistPersistenceService() {
    }
    
    @SuppressWarnings("unchecked")
    public List<MPMedia> loadPlaylist() {
        List<MPMedia> playlist = new ArrayList<>();
        if(Files.exists(Paths.get(PATH))) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(PATH);
            } catch (FileNotFoundException e) {
                LOG.log(Level.WARNING, "Playlist file not found", e);
                return null;
            }
            
            XMLDecoder dec = new XMLDecoder(new BufferedInputStream(fis));
            playlist = (List<MPMedia>) dec.readObject();
            dec.close();
        }
        return playlist;
    }
    
    public void savePlaylist(List<MPMedia> playlist) {
        
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(PATH);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        // TODO Don't sore user and password but query current host or ask user if media host not current!
        XMLEncoder enc = new XMLEncoder(new BufferedOutputStream(fos));
        enc.setPersistenceDelegate(MPMedia.class,
                new DefaultPersistenceDelegate(new String[] {"name", "source", "user", "password", "type"}));
        enc.writeObject(playlist);
        enc.close();
    }

}

