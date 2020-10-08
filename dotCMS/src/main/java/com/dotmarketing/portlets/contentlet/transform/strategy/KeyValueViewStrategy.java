package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class intended to collect {@link com.dotcms.contenttype.model.field.KeyValueField} and present them as Map with entries like `fieldVariable`
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

            keyValueFields.forEach(field ->
                    map.put(field.variable(), contentlet.getKeyValueProperty(field.variable())));
        }

        return map;
    }

}
