package mb.client.rfe.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigService {

    private static final Logger LOG = 
            Logger.getLogger(ConfigService.class.getName());
    private static final String FILE_PATH = "config.properties";
	private static ConfigService ref;
	private Properties properties;

	public static ConfigService getInstance() {
		synchronized (ConfigService.class) {
			if (ref == null) {
				ref = new ConfigService();
			}
		}
		return ref;
	}

	private ConfigService() {
	}

	public String getProperty(String key) {
		return getProperties().getProperty(key);
	}
	
	public String getOrCreateProperty(String key, String defaultValue) {
		String val = null;
		synchronized (ConfigService.class) {
			val = getProperties().getProperty(key);
			if(val == null) {
				val = defaultValue;
				getProperties().setProperty(key, val);
				storeProperties();
			}
		}
		return val;
	}
	
	public void setProperty(String key, String value) {
        getProperties().setProperty(key, value);
        synchronized (ConfigService.class) {
            storeProperties();
        }
	}

	@SuppressWarnings("serial")
    private Properties getProperties() {
		synchronized (ConfigService.class) {
			if (properties == null) {
			    
			    Path path = Paths.get(FILE_PATH);
			    if(!Files.exists(path)) {
			        try {
                        Files.createFile(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
			    }
			    
			    try(FileInputStream fis = new FileInputStream(FILE_PATH)) {
	                 properties = new Properties()  {
	                        
	                        // Order alphabetically
	                        public synchronized Enumeration<Object> keys() {
	                            return Collections.enumeration(new TreeSet<Object>(super.keySet()));
	                        }
	                    };
	                    properties.load(fis);
			    } catch (Exception e) {
			        LOG.log(Level.WARNING, "Failed to load properties file", e);
			        throw new RuntimeException(e);
                } 
			}
		}
		return properties;
	}
	
	private void storeProperties() {
		try(FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
		    getProperties().store(fos, null);
		} catch (Exception e) {
		    LOG.log(Level.WARNING, "Failed to save properties file", e);
        } 
	}
}