package mb.client.webdav.service;

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
import java.util.Collections;
import java.util.List;

import mb.client.webdav.model.WebDAVHost;

public class HostPersistenceService {
    
    private static final String PATH = "hosts.xml";
    private static HostPersistenceService ref;
    private List<WebDAVHost> hosts;
    
    public static HostPersistenceService getInstance() {
        synchronized (HostPersistenceService.class) {
            if (ref == null) {
                ref = new HostPersistenceService();
            }
        }
        return ref;
    }
    
    private HostPersistenceService() {
        loadHosts();
    }
    
    public List<WebDAVHost> getHosts() {
        return Collections.unmodifiableList(hosts); 
    }
    
    public void addHost(WebDAVHost host) {
        hosts.add(host);
        saveHosts();
    }
    
    public boolean deleteHost(WebDAVHost host) {
        boolean success = hosts.remove(host);
        if(success) {
            saveHosts();
        }
        return success;
    }
    
    @SuppressWarnings("unchecked")
    private void loadHosts() {
        hosts = new ArrayList<>();
        
        if(Files.exists(Paths.get(PATH))) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(PATH);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            
            XMLDecoder dec = new XMLDecoder(new BufferedInputStream(fis));
            hosts = (List<WebDAVHost>) dec.readObject();
            dec.close();
        }
    }
    
    private void saveHosts() {
        
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(PATH);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        XMLEncoder enc = new XMLEncoder(new BufferedOutputStream(fos));
        enc.setPersistenceDelegate(WebDAVHost.class,
                new DefaultPersistenceDelegate(new String[] {"baseURI", "root", "user", "password"}));
        enc.writeObject(hosts);
        enc.close();
    }

}
