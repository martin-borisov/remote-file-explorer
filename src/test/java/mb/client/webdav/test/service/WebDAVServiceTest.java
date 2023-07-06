package mb.client.webdav.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

import mb.client.webdav.model.WebDAVHost;
import mb.client.webdav.model.WebDAVResource;
import mb.client.webdav.service.WebDAVService;

public class WebDAVServiceTest {
    
    @Test
    public void verifyList() throws Exception {
        WebDAVHost host = new WebDAVHost(URI.create("https://www.dummy.com"), "/webdav", "user", "password");
        WebDAVService service = new WebDAVService(host);
        
        // Create some data
        List<DavResource> data = Arrays.asList(
                ServiceTestUtil.createDavResource(host.getBaseUriString() + "/file1", "test/test", false),
                ServiceTestUtil.createDavResource(host.getBaseUriString() + "/file2", "test/test", false),
                ServiceTestUtil.createDavResource(host.getBaseUriString() + "/dir1", "test/test", true)
                );
        MockSardineImpl mock = new MockSardineImpl();
        mock.setDavResListToReturn(data);
        injectDummy(service, mock);
        
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
    
    private void injectDummy(WebDAVService service, Sardine sardine) throws Exception {
        Field field = service.getClass().getDeclaredField("sardine");
        field.setAccessible(true);
        field.set(service, sardine);
    }
}
