package mb.client.webdav.test.service;

import java.net.URISyntaxException;

import com.github.sardine.DavResource;
import com.github.sardine.model.Collection;
import com.github.sardine.model.Getcontenttype;
import com.github.sardine.model.Prop;
import com.github.sardine.model.Propstat;
import com.github.sardine.model.Resourcetype;
import com.github.sardine.model.Response;

public class ServiceTestUtil {
    
    public static DavResource createDavResource(String href, String contentType, boolean dir) {
        Response res = new Response();
        res.getHref().add(href);
        
        // Content type
        if(contentType != null && !dir) {
            Getcontenttype ct = new Getcontenttype();
            ct.getContent().add(contentType);
            Prop prop = new Prop();
            prop.setGetcontenttype(ct);
            Propstat propstat = new Propstat();
            propstat.setProp(prop);
            res.getPropstat().add(propstat);
        } else {
            Resourcetype rt = new Resourcetype();
            rt.setCollection(new Collection());
            Prop prop = new Prop();
            prop.setResourcetype(rt);
            Propstat propstat = new Propstat();
            propstat.setProp(prop);
            res.getPropstat().add(propstat);
        }
        
        try {
            return new DavResource(res);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
