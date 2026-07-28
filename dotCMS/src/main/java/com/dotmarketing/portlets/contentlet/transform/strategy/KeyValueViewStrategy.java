package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.OrderedKeyValueMap;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects all {@link KeyValueField} instances from a contentlet and exposes them in the
 * response map under their field variable names.
 *
 * <p>Each field value is wrapped in an {@link OrderedKeyValueMap} before being placed in the
 * response. This ensures that the insertion order saved to the DB is preserved in the REST
 * response, even though {@code DotObjectMapperProvider} configures Jackson with
 * {@code SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS=true} (which re-sorts raw {@link Map}
 * keys alphabetically). {@link OrderedKeyValueMap} carries a custom {@code @JsonSerialize}
 * annotation that bypasses that flag.</p>
 *
 * <p>The wrapping is a no-op when the contentlet was loaded via the JSON-column path, because
 * {@link KeyValueField#asMap} already returns an {@link OrderedKeyValueMap}. It acts as a
 * safety net for the legacy-column path where
 * {@link Contentlet#getKeyValueProperty} may return a plain {@link java.util.LinkedHashMap}.</p>
 *
 * @author jsanca
 */
public class KeyValueViewStrategy extends AbstractTransformStrategy<Contentlet> {

    /**
     * Regular constructor takes a toolbox
     * @param toolBox
     */
    public KeyValueViewStrategy(final APIProvider toolBox) {
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

        final List<Field> keyValueFields = contentlet.getContentType().fields(KeyValueField.class);

        if (UtilMethods.isSet(keyValueFields)) {

            keyValueFields.forEach(field -> {
                final Map<String, Object> raw = contentlet.getKeyValueProperty(field.variable());
                map.put(field.variable(),
                        raw instanceof OrderedKeyValueMap ? raw : new OrderedKeyValueMap(raw));
            });
        }

        return map;
    }

}
