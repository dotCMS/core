package com.dotcms.rendering.velocity.services;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;
import java.io.StringWriter;

import org.apache.velocity.runtime.resource.ResourceManager;



public class VelocityResourceKey implements Serializable {

    private final static char RESOURCE_TEMPLATE = ResourceManager.RESOURCE_TEMPLATE + '0';

    private static final long serialVersionUID = 1L;
    public final String path, language, id1, id2, cacheKey;
    public final VelocityType type;
    public final PageMode mode;
    public VelocityResourceKey(final Object obj) {
        this((String) obj);
    }
    public VelocityResourceKey(final String filePath) {
        path = cleanKey(filePath);

        final String[] pathArry = path.split("[/\\.]", 0);
        this.mode = PageMode.get(pathArry[1]);
        this.id1 = pathArry[2].indexOf("_") > -1 ? pathArry[2].substring(0, pathArry[2].indexOf("_")) : pathArry[2];
        this.language = pathArry[2].indexOf("_") > -1 ? pathArry[2].substring(pathArry[2].indexOf("_") + 1, pathArry[2].length())
                : String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId());

        this.id2 = pathArry.length > 4 ? pathArry[3] : null;
        this.type = VelocityType.resolveVelocityType(filePath);
        this.cacheKey = cacheKey();
    }

    private String cacheKey() {
        StringWriter sw = new StringWriter();
            sw.append(mode.name())
                .append('-')
                .append(id1);
        if(type != VelocityType.CONTAINER && id2!=null) {
            sw.append('-');
            sw.append(id2);
        }
        sw.append('-'); 
        sw.append(language); 
        sw.append("."); 
        sw.append(type.fileExtension); 
        return sw.toString();
       

    }
    
    private String cleanKey(final String key) {
        String newkey = (key.charAt(0) == RESOURCE_TEMPLATE ) ? key.substring(1) : key;
        newkey = (newkey.charAt(0) != '/')  ? "/" + newkey : newkey;
        return (newkey.contains("\\")) ?UtilMethods.replace(newkey, "\\", "/") : newkey;

    }

    @Override
    public String toString() {
        return "VelocityResourceKey [path=" + path + ", language=" + language + ", id1=" + id1 + ", id2=" + id2 + ", type=" + type
                + ", mode=" + mode + "]";
    }

    

}
