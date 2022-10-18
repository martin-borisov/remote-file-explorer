package mb.client.webdav.media.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class AudioSource {
    
    private URL url;
    private File file;
    
    public AudioSource(URL url) {
        this.url = url;
    }

    public AudioSource(File file) {
        this.file = file;
    }
    
    public AudioSource(String path) {
        
        URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Path must be valid", e);
        }
        
        if("file".equalsIgnoreCase(uri.getScheme())) {
            this.file = new File(uri);
        } else {
            try {
                this.url = uri.toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException("Path must be valid", e);
            }
        }
    }

    public InputStream openStream() throws AudioPlayerException {
        InputStream is;
        try {
            if(url != null) {
                is = url.openStream();
            } else if(file != null) {
                is = new FileInputStream(file);
            } else {
                throw new RuntimeException("Valid URL or file should be passed to this instance");
            }
        } catch (FileNotFoundException e) {
            throw new AudioPlayerException(e);
        } catch (IOException e) {
            throw new AudioPlayerException(e);
        }
        return is;
    }

}
