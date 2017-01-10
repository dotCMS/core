package com.dotcms.translate;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
import java.util.List;

public class TranslationUtil {

    private static class Holder {
        private static final TranslationUtil INSTANCE;
        private static final TranslationService SERVICE;

        static {
            INSTANCE = new TranslationUtil();
            try {
                String clazz = Config.getStringProperty("TRANSLATION_SERVICE",
                    GoogleTranslationService.class.getName());
                SERVICE = (TranslationService) Class.forName(clazz).newInstance();

            } catch (Exception e) {
                Logger.error(TranslationUtil.class, e.getMessage(), e);
                throw new DotStateException(e.getMessage());
            }
        }

    }

    private TranslationUtil() {
    }

    public static TranslationUtil getUtil() {
        return Holder.INSTANCE;
    }

    public static TranslationService getService() {
        return Holder.SERVICE;
    }

    /**
     * Returns a dotCMS language or languages based soley on the language Code,
     * meaning, if you pass in fr, it would return fr-ca and fr-fr, assuming you
     * had them set up in dotCMS
     */
    public List<Language> getLanguagesByLanguageCode(String languageCode) {
        List<Language> allLangs = APILocator.getLanguageAPI().getLanguages();
        List<Language> returnLangs = new ArrayList<Language>();

        if (!UtilMethods.isSet(languageCode)) {
            return returnLangs;
        }

        if ("all".equalsIgnoreCase(languageCode)) {
            return allLangs;
        }

        for (Language l : allLangs) {
            if (languageCode.equalsIgnoreCase(l.getLanguageCode())) {
                returnLangs.add(l);
            }
        }
        return returnLangs;
    }

    /**
     * Return the list of fields of a given content. Fields can by filtered by type by providing a list of types
     * expressed as strings. E.g. Arrays.asList("text","wysiwyg","textarea"). See {@link Field.FieldType}.
     * <p>Certain fields can be excluded from the result by providing a list of fields to exclude. This list takes
     * the velocity name (as String) of each field.
     *
     * @param contentlet      the content to get its field from
     * @param typesFilter     list of types to filter the results by
     * @param fieldsToExclude list of fields' velocityNames to exclude from the result
     * @return the fields of the contentlet
     */
    public List<Field> getFieldsOfContentlet(Contentlet contentlet, List<String> typesFilter,
                                             List<String> fieldsToExclude) {
        List<Field> fieldsToFilter = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
        return filterFields(fieldsToFilter, typesFilter, fieldsToExclude);
    }

    @VisibleForTesting
    protected List<Field> filterFields(List<Field> fieldsToFilter, List<String> typesFilter,
                                       List<String> fieldsToExclude) {
        List<Field> fields = new ArrayList<>();

        boolean filterByType = typesFilter != null && !typesFilter.isEmpty();
        boolean ignoreFields = fieldsToExclude != null && !fieldsToExclude.isEmpty();

        if(!filterByType) return fields;

        for (Field f : fieldsToFilter) {
            boolean addField = false;

            if(typesFilter.contains(f.getFieldType())) {
                if(f.getFieldType().equals(Field.FieldType.TEXT.toString())
                    && f.getDataType().equals(Field.DataType.TEXT.toString())) {
                    addField = true;
                } else if (!f.getFieldType().equals(Field.FieldType.TEXT.toString())) {
                    addField = true;
                }
            }

            boolean exclude = ignoreFields && fieldsToExclude.contains(f.getVelocityVarName());

            if (addField && !exclude) {
                fields.add(f);
            }
        }

        return fields;
    }

    /**
     * Return a list of {@link Language} from a list of language codes (e.g. "en", "es", "fr").
     *
     * <p>Language with a particular Id can be excluded from the result
     */
    public List<Language> getLanguagesByLanguageCodes(List<String> languagesCodes) {
        List<Language> languages = new ArrayList<>();

        for (String to : languagesCodes) {
            languages.addAll(getLanguagesByLanguageCode(to));
        }

        return languages;
    }

}
