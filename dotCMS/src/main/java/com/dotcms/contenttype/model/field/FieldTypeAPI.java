package com.dotcms.contenttype.model.field;

import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import java.util.*;

/**
 * Factory of {@link FieldType}
 */
public class FieldTypeAPI {

    private Map<String, Field> fieldsMap;

    private static class SingletonHolder {
        private static final FieldTypeAPI INSTANCE = new FieldTypeAPI();

        private SingletonHolder(){}
    }

    public static FieldTypeAPI getInstance() {

        return FieldTypeAPI.SingletonHolder.INSTANCE;
    }

    private FieldTypeAPI(){
        fieldsMap = new HashMap<>();
        LegacyFieldTypes[] legacyFieldTypes = LegacyFieldTypes.values();

        for (LegacyFieldTypes legacyFieldType : legacyFieldTypes) {
            String id = legacyFieldType.legacyValue();

            Class<? extends Field> fieldClass = legacyFieldType.implClass();
            addField(id, fieldClass);
        }
    }

    public void addField(String id, Class<? extends Field> fieldClass) {
        fieldsMap.put( id, createFieldIntance( fieldClass ) );
    }

    public Collection<FieldType> getFieldTypes(User user){
        Collection<FieldType> fieldTypes = new TreeSet();

        for (Map.Entry<String, Field> fieldEntry : fieldsMap.entrySet()) {
            String id = fieldEntry.getKey();
            Field field = fieldEntry.getValue();

            Class<? extends Field> fieldClass = field.getClass();

            try {
                String label = LanguageUtil.get( user, field.getContentTypeFieldLabelKey() );
                String helpText = UtilMethods.escapeSingleQuotes( LanguageUtil.get( user, field.getContentTypeFieldHelpTextKey() ) );
                Collection<ContentTypeFieldProperties> fieldContentTypeProperties = field.getFieldContentTypeProperties();

                if (!fieldContentTypeProperties.isEmpty()) {
                    FieldType fieldType = new FieldType(id, label, fieldContentTypeProperties, helpText, fieldClass.getName());
                    fieldTypes.add(fieldType);
                }
            } catch (LanguageException e) {
                continue;
            }
        }

        return fieldTypes;
    }

    private Field createFieldIntance(Class<? extends Field> fieldClass){
        return FieldBuilder.builder(fieldClass)
                .name("testing")
                .build();
    }
}