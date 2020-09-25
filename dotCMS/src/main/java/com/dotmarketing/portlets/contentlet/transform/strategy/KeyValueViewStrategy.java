package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.StringUtils;
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

        if (null != contentlet && null != contentlet.getContentType()) {

            final List<Field> keyValueFields = contentlet.getContentType().fields(KeyValueField.class);

            if (UtilMethods.isSet(keyValueFields)) {

                keyValueFields.stream().filter(field -> this.isValidKeyValue(field, contentlet))
                        .forEach(field ->
                                map.put(field.variable(), this.getKeyValueObject(field, contentlet)));
            }
        }

        return map;
    }

    private Object  getKeyValueObject (final Field field, final Contentlet contentlet) {

        return contentlet.getMap().get(field.variable()) instanceof Map?
            contentlet.getMap().get(field.variable()):
            contentlet.getKeyValueProperty(field.variable());
    }
    private boolean isValidKeyValue (final Field field, final Contentlet contentlet) {

        return contentlet.getMap().containsKey(field.variable()) &&
                (contentlet.getMap().get(field.variable()) instanceof Map || // if it is a map ok, or if it is a string should contains a {
                        (UtilMethods.isSet(contentlet.getMap().get(field.variable())) &&
                                StringUtils.isJson(contentlet.getMap().get(field.variable()).toString().trim())));
    }

}
