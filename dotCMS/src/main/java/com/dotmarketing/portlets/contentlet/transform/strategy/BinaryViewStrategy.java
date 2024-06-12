package com.dotmarketing.portlets.contentlet.transform.strategy;

import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.AVOID_MAP_SUFFIX_FOR_VIEWS;
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
                    final String sufix = options.contains(AVOID_MAP_SUFFIX_FOR_VIEWS)
                            ? "" : "Map";

                    map.put(field.variable(),"/dA/" + contentlet.getInode() + "/" + field.variable()  + "/file" );
                    map.put(field.variable() + sufix, transform(contentlet, field));

                } catch (Exception e) {
                    Logger.warn(this,
                            "Unable to get Binary from field with var " + field.variable());
                }
            }
        }
        return map;
    }



    public static Map<String, Object> transformDeprecated(final Contentlet con,
            final Field field) {
        return new BinaryViewStrategy(null).transform(con, field);
    }



    /**
     * Transform function
     */
    public Map<String, Object> transform(final Contentlet contentlet,
            final Field field) {

        final Map<String, Object> map = new HashMap<>();
        Metadata metadata = Try.of(()->contentlet.getBinaryMetadata(field.variable())).getOrElseThrow(()->new DotStateException("Unable to get Binary from field with var " + field.variable()));



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
        map.remove("path");
        map.put("focalPoint", Try.of(()-> metadata.getCustomMeta().getOrDefault("focalPoint", "0.0").toString()).getOrElse("0.0"));
        putBinaryLinks(field.variable(), assetName, contentlet, map);
        return map;
    }

    /**
     * put the version and fields specifics for the binary fields
     * @param velocityVarName
     * @param assetName
     * @param contentlet
     * @param map
     */
    private void putBinaryLinks(final String velocityVarName, final String assetName, final Contentlet contentlet, final Map<String, Object> map){
        //The binary-field per se. Must be replaced by file-name. We dont want to disclose any file specifics.
        final String dAPath = "/dA/%s/%s/%s";
        map.put(velocityVarName + "Version",
                String.format(dAPath, contentlet.getInode(),
                        velocityVarName, assetName));
        map.put(velocityVarName,
                String.format(dAPath, contentlet.getIdentifier(),
                        velocityVarName, assetName));
        map.put(velocityVarName + "ContentAsset",
                contentlet.getIdentifier() + "/" + velocityVarName);
    }

}
