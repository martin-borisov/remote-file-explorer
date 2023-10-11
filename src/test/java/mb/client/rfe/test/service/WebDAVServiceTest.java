package mb.client.rfe.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

import javafx.util.Callback;
import mb.client.rfe.model.ResourceHost;
import mb.client.rfe.model.WebDAVResource;
import mb.client.rfe.service.ResourceRepositoryService;
import mb.client.rfe.service.WebDAVService;

public class WebDAVServiceTest {
    
    private static ResourceHost host;
    
    @BeforeClass
    public static void setupClass() {
        host = new ResourceHost(URI.create("https://www.dummy.com"), "/webdav", "user", "password");
    }
    
    @Test
    public void verifyList() throws Exception {
        
        // Create test data
        List<DavResource> data = Arrays.asList(
                ServiceTestUtil.createDavResource(host.getBaseUriString() + "/file1", "test/test", false),
                ServiceTestUtil.createDavResource(host.getBaseUriString() + "/file2", "test/test", false),
                ServiceTestUtil.createDavResource(host.getBaseUriString() + "/dir1", "test/test", true)
                );
        MockSardineImpl mock = new MockSardineImpl();
        mock.setDavResListToReturn(data);
        
        ResourceRepositoryService service = new WebDAVService(host);
        injectMock(service, mock);
        
        // Execute and verify
        List<WebDAVResource> resources = service.list("doesn't matter");
        assertEquals("Correct resource count", 3, resources.size());
        assertTrue("Correct resource name", resources.get(0).getName().equals("file1"));
        assertTrue("Correct resource path", 
                resources.get(0).getAbsolutePath().equals(host.getBaseUriString() + "/file1"));
        assertFalse("Correct resource type", resources.get(0).isDirectory());
        assertTrue("Resource is a directory", resources.get(2).isDirectory());
        
        List<WebDAVResource> dirs = service.listDirs("doesn't matter");
        assertEquals("Correct dir count", 1, dirs.size());
        assertTrue("Resource is a directory", dirs.get(0).isDirectory());
        
        List<WebDAVResource> files = service.listFiles("doesn't matter");
        assertEquals("Correct dir count", 2, files.size());
        assertFalse("Resource is a file", files.get(0).isDirectory());
    }
    
    @Test
    public void verifyDownload() throws Exception {
        
        // Create test data
        InputStream is = WebDAVServiceTest.class.getResourceAsStream("/click-track.mp3");
        MockSardineImpl mock = new MockSardineImpl();
        mock.setIsToReturn(is);
        
        ResourceRepositoryService service = new WebDAVService(host);
        injectMock(service, mock);
        
        // Execute and verify
        File file = service.download(new WebDAVResource("testfile1", "/testfile1"), new Callback<Integer, Void>() {
            public Void call(Integer param) {
                return null;
            }
        });
        
        assertNotNull("Non null file returned", file);
        assertTrue("Downloads dir exists", Files.exists(Paths.get("downloads")));
        assertTrue("Downloaded file exists", Files.exists(Paths.get("downloads/testfile1")));
        
        // Cleanup 
        Files.delete(Paths.get("downloads/testfile1"));
    }
    
    private void injectMock(ResourceRepositoryService service, Sardine sardine) throws Exception {
        Field field = service.getClass().getDeclaredField("sardine");
        field.setAccessible(true);
        field.set(service, sardine);
    }
}
