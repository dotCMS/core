package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.MAP_SUFFIX_FOR_VIEWS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.BINARIES;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.FILTER_BINARIES;
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
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class intended to collect binaries and present them as Map with entries like `fieldVariable` followed by `Map`
 */
public class BinaryViewStrategy extends AbstractTransformStrategy<Contentlet> {

    /**
     * Regular constructor takes a toolbox
     * @param toolBox
     */
    @VisibleForTesting
    public BinaryViewStrategy(final APIProvider toolBox) {
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

        if (!options.contains(BINARIES) || options.contains(FILTER_BINARIES)) {
            return map;
        }


        final List<Field> binaries = contentlet.getContentType().fields(BinaryField.class);


        if (!binaries.isEmpty()) {
            for (final Field field : binaries) {
                try {
                    final String sufix = options.contains(MAP_SUFFIX_FOR_VIEWS)
                            ? "Map" : "";


                    map.put(field.variable() + sufix, transform(contentlet, field, false));

                } catch (Exception e) {
                    Logger.warnAndDebug(this.getClass(),
                            "Unable to get Binary from field with var " + field.variable(),e);
                }
            }
        }
        return map;
    }


    ThreadLocal<Map<String,Object>> mapThreadLocal = ThreadLocal.withInitial(HashMap::new);


    /**
     * Transform function
     */
    public Map<String, Object> transform(final Contentlet contentlet,
            final Field field, boolean isFromFileField) {

        final Map<String, Object> map = mapThreadLocal.get();
        map.clear();
        Metadata metadata = Try.of(()->contentlet.getBinaryMetadata(field.variable())).getOrElseThrow(()->new DotStateException("Unable to get Binary from field with var " + field.variable()));

        map.putAll(metadata.getMap());

        if(isFromFileField){
            map.put("identifier",contentlet.getIdentifier());
            map.put("inode",contentlet.getInode());
            map.put("apiPath", "/api/v1/content/"+contentlet.getInode());
        }

        final Identifier identifier = Try.of(()-> APILocator.getIdentifierAPI().find(contentlet.getIdentifier())).getOrNull();

        String assetName = contentlet.isFileAsset() && null != identifier ? identifier.getAssetName() :   metadata.getName();

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
        map.putIfAbsent("name", assetName);
        map.putIfAbsent("size", metadata.getSize());
        map.putIfAbsent("mime", metadata.getContentType());
        map.putIfAbsent("isImage", metadata.isImage());
        map.putIfAbsent("width", metadata.getWidth());
        map.putIfAbsent("height", metadata.getHeight());
        map.putIfAbsent("path", metadata.getPath());
        map.putIfAbsent("title", metadata.getTitle());
        map.putIfAbsent("sha256", metadata.getSha256());
        map.putIfAbsent("modDate", metadata.getModDate());

        String assetPath = contentlet.isFileAsset() && null != identifier ? identifier.getPath() :   map.get("idPath").toString();

        map.put("path", assetPath);
        map.putIfAbsent("focalPoint", Try.of(()-> metadata.getCustomMeta().getOrDefault("focalPoint", "0.0").toString()).getOrElse("0.0"));

        return map;
    }



}
