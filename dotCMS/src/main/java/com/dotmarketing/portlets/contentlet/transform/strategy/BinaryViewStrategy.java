package com.dotmarketing.portlets.contentlet.transform.strategy;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
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
    BinaryViewStrategy(final TransformToolbox toolBox) {
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
    protected Map<String, Object> transform(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user) {

        final List<Field> binaries = contentlet.getContentType().fields(BinaryField.class);

        if (!binaries.isEmpty()) {
            for (final Field field : binaries) {
                try {
                    map.put(field.variable() + "Map", transform(field, contentlet));
                    final File conBinary = contentlet.getBinary(field.variable());
                    if (conBinary != null) {
                        //This clearly replaces the binary by a string which is the expected output on BinaryToMapTransformer.
                        map.put(field.variable(), conBinary.getName());
                    }
                } catch (IOException e) {
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
        File file;
        try {
            file = contentlet.getBinary(field.variable());
        } catch (IOException e) {
            throw new DotStateException(e);
        }

        if (file == null) {
            return emptyMap();
        }

        return transform(file, contentlet, field);
    }

    /**
     * Transform function
     */
    public static Map<String, Object> transform(final File file, final Contentlet contentlet,
            final Field field) {
        DotPreconditions.checkNotNull(file, IllegalArgumentException.class, "File can't be null");
        final Map<String, Object> map = new HashMap<>();

        map.put("versionPath",
                "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getInode()) + "/" + field
                        .variable() + "/" + file.getName());
        final int contentLanguageSize = Try
                .of(() -> APILocator.getLanguageAPI().getLanguages()).getOrElse(emptyList()).size();
        map.put("idPath",
                "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getIdentifier()) + "/"
                        + field.variable() + "/" + file.getName()
                        + (contentLanguageSize > 1 ? "?language_id=" + contentlet.getLanguageId()
                        : StringPool.BLANK));
        map.put("name", file.getName());
        map.put("size", file.length());
        map.put("mime", Config.CONTEXT.getMimeType(file.getName()));
        map.put("isImage", UtilMethods.isImage(file.getName()));

        return map;
    }
}
