package com.dotcms.rendering.velocity.services;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.runtime.resource.ResourceManager;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.Optional;

public class VelocityResourceKey implements Serializable {

    private static final char RESOURCE_TEMPLATE = ResourceManager.RESOURCE_TEMPLATE + '0';
    private static final String HOST_INDICATOR = "///";
    private static final long serialVersionUID = 1L;

    public final String path, language, id1, id2, cacheKey, variant;
    public final VelocityType type;
    public final PageMode mode;

    public VelocityResourceKey(final Object obj) {
        this((String) obj);
    }
    
    public VelocityResourceKey(final Field asset, Optional<Contentlet> con, PageMode mode) {
        this("/" + mode.name() + "/" + (con.isPresent() ?  con.get().getInode() : FieldLoader.FIELD_CONSTANT  ) + "/" + asset.id() +  "." + VelocityType.FIELD.fileExtension);
    }
    
    public VelocityResourceKey(final Template asset, final PageMode mode) {
        this("/" + mode.name() + "/" + asset.getIdentifier() +  "." + VelocityType.TEMPLATE.fileExtension);
    }
    public VelocityResourceKey(final Container asset, final PageMode mode) {
        this(asset, Container.LEGACY_RELATION_TYPE, mode);
    }
    
    public VelocityResourceKey(final Container asset, final String uuid, final PageMode mode) {
        this("/" + mode.name() + "/" + asset.getIdentifier() + "/" + uuid +  "." + VelocityType.CONTAINER.fileExtension);
    }


    public VelocityResourceKey(final HTMLPageAsset asset, final PageMode mode, final long language) {
        this(asset, mode, language, asset.getVariantId());
    }

    public VelocityResourceKey(final HTMLPageAsset asset, final PageMode mode, final long language, final String variantName) {
        this(getHTMLPageFilePath(asset.getIdentifier(), mode, language, variantName));
    }

    public static String getHTMLPageFilePath(HTMLPageAsset asset, PageMode mode, long language) {
        return getHTMLPageFilePath(asset.getIdentifier(), mode, language, asset.getVariantId());
    }

    public static String getHTMLPageFilePath(String id, PageMode mode, long language, String variantId) {
        return "/" + mode.name() + "/" + id + StringPool.UNDERLINE + language +
                StringPool.UNDERLINE +  variantId + "." + VelocityType.HTMLPAGE.fileExtension;
    }

    public VelocityResourceKey(final Contentlet asset, final PageMode mode, final long language) {
        this("/" + mode.name() + "/" + asset.getIdentifier() + StringPool.UNDERLINE + language +
                StringPool.UNDERLINE +  asset.getVariantId() + "." + VelocityType.CONTENT.fileExtension);
    }
    public VelocityResourceKey(final String filePath) {

        path = cleanKey(filePath);

        final String[] pathArry = path.split("[/\\.]", 0);

        this.mode = PageMode.get(pathArry[1]);

        final String[] underlineSplit = pathArry[2].split(StringPool.UNDERLINE);
        this.id1 = underlineSplit[0];

        this.language = underlineSplit.length > 1 ? underlineSplit[1]
                : String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId());

        this.variant = underlineSplit.length > 2 ?
                Arrays.stream(Arrays.copyOfRange(underlineSplit, 2, underlineSplit.length))
                        .collect(Collectors.joining(StringPool.UNDERLINE)) :
                VariantAPI.DEFAULT_VARIANT.name();

        this.type = VelocityType.resolveVelocityType(filePath);
        this.id2 = pathArry.length > 4 ? pathArry[3] : null;
        this.cacheKey = cacheKey();

    }

    private Tuple2<String, String> getPathNormalizedFileAssetContainerPath(final String path) {

        final int startPath = path.indexOf(HOST_INDICATOR);
        final int endPath   = path.lastIndexOf(StringPool.FORWARD_SLASH);
        final String containerPath  = path.substring(startPath, endPath);
        // we just want to replace the container path in the keypath, with a single number, but 1 is not important will be discard later.
        final String normalizedPath = StringUtils.replace(path, containerPath, "/1");

        return Tuple.of(containerPath, normalizedPath);
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
        newkey = (newkey.charAt(0) != '/') ? "/" + newkey : newkey;
        return (newkey.contains("\\")) ? UtilMethods.replace(newkey, "\\", "/") : newkey;

    }

    @Override
    public String toString() {
        return "VelocityResourceKey [path=" + path + ", language=" + language + ", id1=" + id1 + ", id2=" + id2 + ", type=" + type
                + ", mode=" + mode + "]";
    }



}
