package mb.client.webdav.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

public class ConfigService {

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

	private Properties getProperties() {
		synchronized (ConfigService.class) {
			if (properties == null) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream("config.properties");
					properties = new Properties()  {
						private static final long serialVersionUID = 2309880819740125962L;
						
						// Order alphabetically
						public synchronized Enumeration<Object> keys() {
					        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
					    }
					};
					properties.load(fis);
				} catch (Exception e) {
					throw new RuntimeException("Failed to load properties file", e);
				} finally {
					if(fis != null) {
						try {
							fis.close();
						} catch (IOException e) {
						}
					}
				}
			}
		}
		return properties;
	}
	
	private void storeProperties() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("config.properties");
			getProperties().store(fos, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
	}
}