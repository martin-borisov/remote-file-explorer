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

import mb.client.webdav.model.ResourceHost;

public class HostPersistenceService {
    
    private static final String FILE_PATH = "hosts.xml";
    private static HostPersistenceService ref;
    private List<ResourceHost> hosts;
    
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
    
    public List<ResourceHost> getHosts() {
        return Collections.unmodifiableList(hosts); 
    }
    
    public void addHost(ResourceHost host) {
        hosts.add(host);
        saveHosts();
    }
    
    public boolean deleteHost(ResourceHost host) {
        boolean success = hosts.remove(host);
        if(success) {
            saveHosts();
        }
        return success;
    }
    
    @SuppressWarnings("unchecked")
    private void loadHosts() {
        hosts = new ArrayList<>();
        if(Files.exists(Paths.get(FILE_PATH))) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(FILE_PATH);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            XMLDecoder dec = new XMLDecoder(new BufferedInputStream(fis));
            hosts = (List<ResourceHost>) dec.readObject();
            dec.close();
        }
    }
    
    private void saveHosts() {
        
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(FILE_PATH);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        XMLEncoder enc = new XMLEncoder(new BufferedOutputStream(fos));
        enc.setPersistenceDelegate(ResourceHost.class,
                new DefaultPersistenceDelegate(new String[] {"baseURI", "root", "user", "password"}));
        enc.writeObject(hosts);
        enc.close();
    }

}
