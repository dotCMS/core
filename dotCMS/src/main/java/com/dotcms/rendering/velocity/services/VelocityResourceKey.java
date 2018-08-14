package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.model.field.Field;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import java.io.File;
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
    
    public VelocityResourceKey(final Field asset, Contentlet con, PageMode mode) {
        this(File.separator + mode.name() + File.separator + con.getIdentifier() + File.separator + asset.id() +  "." + VelocityType.FIELD.fileExtension);
    }
    
    public VelocityResourceKey(final Template asset, final PageMode mode) {
        this(File.separator + mode.name() + File.separator + asset.getIdentifier() +  "." + VelocityType.TEMPLATE.fileExtension);
    }
    public VelocityResourceKey(final Container asset, final PageMode mode) {
        this(asset, Container.LEGACY_RELATION_TYPE, mode);
    }
    
    public VelocityResourceKey(final Container asset, final String uuid, final PageMode mode) {
        this(File.separator + mode.name() + File.separator + asset.getIdentifier() + File.separator + uuid +  "." + VelocityType.CONTAINER.fileExtension);
    }


    public VelocityResourceKey(final HTMLPageAsset asset, final PageMode mode, final long language) {
        this(File.separator + mode.name() + File.separator + asset.getIdentifier() + "_" + language + "." + VelocityType.HTMLPAGE.fileExtension);
    }
    public VelocityResourceKey(final Contentlet asset, final PageMode mode, final long language) {
        this(File.separator + mode.name() + File.separator + asset.getIdentifier() + "_" + language + "." + VelocityType.CONTENT.fileExtension);
    }
    public VelocityResourceKey(final String filePath) {
        path = cleanKey(filePath);

        final String[] pathArry = path.split("["+File.separator+"\\.]", 0);
        this.mode = PageMode.get(pathArry[1]);
        this.id1 = pathArry[2].contains("_") ? pathArry[2].substring(0, pathArry[2].indexOf("_")) : pathArry[2];
        this.language = pathArry[2].contains("_") ? pathArry[2].substring(pathArry[2].indexOf("_") + 1, pathArry[2].length())
                : String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId());

        this.id2 = pathArry.length > 4 ? pathArry[3] : null;
        this.type = VelocityType.resolveVelocityType(filePath);
        this.cacheKey = cacheKey();
    }

    private String cacheKey() {

        if (type == VelocityType.CONTAINER) {
            return new StringWriter()
                .append(mode.name())
                .append('-')
                .append(id1)
                .append('-')
                .append(language)
                .append(".")
                .append(type.fileExtension)
                .toString();
        } else {
            return path;
        }


    }

    private String cleanKey(final String key) {
        String newkey = (key.charAt(0) == RESOURCE_TEMPLATE) ? key.substring(1) : key;
        newkey = (newkey.charAt(0) != File.separator.charAt(0)) ? File.separator + newkey : newkey;
        return (newkey.contains("\\")) ? UtilMethods.replace(newkey, "\\", File.separator) : newkey;

    }

    @Override
    public String toString() {
        return "VelocityResourceKey [path=" + path + ", language=" + language + ", id1=" + id1 + ", id2=" + id2 + ", type=" + type
                + ", mode=" + mode + "]";
    }



}
