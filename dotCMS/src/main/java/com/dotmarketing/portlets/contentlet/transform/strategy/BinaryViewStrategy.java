package com.dotmarketing.portlets.contentlet.transform.strategy;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Class intended to collect binaries and present them as Map with entries like `fieldVariable` followed by `Map`
 */
public class BinaryViewStrategy extends AbstractTransformStrategy<Contentlet> {

    /**
     * Regular constructor takes a toolbox
     * @param toolBox
     */
    BinaryViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    /**
     * Main Transform function
     * @param contentlet
     * @param map
     * @param options
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    protected Map<String, Object> transform(final Contentlet contentlet,
    final Map<String, Object> map, final Set<TransformOptions> options, final User user) {

        final List<Field> binaries = contentlet.getContentType().fields(BinaryField.class);

        if (!binaries.isEmpty()) {
            for (final Field field : binaries) {
                try {
                    map.put(field.variable() + "Map", transform(field, contentlet));
                    final Metadata metadata = contentlet.getBinaryMetadata(field.variable());
                    if (metadata != null) {
                        //This clearly replaces the binary by a string which is the expected output on BinaryToMapTransformer.
                        map.put(field.variable(), metadata.getName());
                    }
                } catch (DotDataException e) {
                    Logger.warn(this,
                            "Unable to get Binary from field with var " + field.variable());
                }
            }
        }
        return map;
    }

    /**
     * Transform function
     */
    public static Map<String, Object> transform(final Field field, final Contentlet contentlet) {
        Metadata metadata;
        try {
            metadata = contentlet.getBinaryMetadata(field.variable());
        } catch (Exception e) {
            throw new DotStateException(e);
        }

        if (metadata == null) {
            return emptyMap();
        }

        return transform(metadata, contentlet, field);
    }

    /**
     * Transform function
     */
    public static Map<String, Object> transform(final Metadata metadata, final Contentlet contentlet,
            final Field field) {
        DotPreconditions.checkNotNull(metadata, IllegalArgumentException.class, "File can't be null");
        final Map<String, Object> map = new HashMap<>();

        final Identifier identifier = Try.of(()-> APILocator.getIdentifierAPI().find(contentlet.getIdentifier())).getOrNull();

        String assetName = metadata.getName();
        if( contentlet.isFileAsset() && null != identifier){
            assetName = identifier.getAssetName();
        }

        map.put("versionPath",
                "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getInode()) + "/" + field
                        .variable() + "/" + assetName);
        final int contentLanguageSize = Try
                .of(() -> APILocator.getLanguageAPI().getLanguages()).getOrElse(emptyList()).size();
        map.put("idPath",
                "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getIdentifier()) + "/"
                        + field.variable() + "/" + assetName
                        + (contentLanguageSize > 1 ? "?language_id=" + contentlet.getLanguageId()
                        : StringPool.BLANK));
        map.put("name", assetName);
        map.put("size", metadata.getSize());
        map.put("mime", metadata.getContentType());
        map.put("isImage", metadata.isImage());
        map.put("width", metadata.getWidth());
        map.put("height", metadata.getHeight());
        map.put("path", metadata.getPath());
        map.put("title", metadata.getTitle());
        map.put("sha256", metadata.getSha256());
        map.put("modDate", metadata.getModDate());
        map.put("focalPoint",  Try.of(()->  metadata.getCustomMeta().get("focalPoint").toString()).getOrElse("0.0"));
        return map;
    }
}
