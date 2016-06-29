package com.dotcms.translate;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractTranslationService implements TranslationService {

    protected ApiProvider apiProvider;

    /**
     * Will return un-persisted new versions of the src content translated to the
     * languages specified
     *
     * @param src               content to translate
     * @param langs             list of language to translate the content to
     * @param fieldsToTranslate list of fields to translate
     * @return a list of un-persisted new versions of the content translated to the requested languages
     */
    @Override
    public List<Contentlet> translateContent(Contentlet src, List<Language> langs, List<Field> fieldsToTranslate,
                                             User user)
        throws TranslationException {

        List<Contentlet> ret = new ArrayList<>();

        for (Language lang : langs) {
            ret.add(translateContent(src, lang, fieldsToTranslate, user));
        }

        return ret;
    }

    /**
     * Will return un-persisted new versions of the src content translated to the
     * language specified.
     *
     * @param src               content to translate
     * @param translateTo       language to translate te content to
     * @param fieldsToTranslate list of fields to translate
     * @return the translated un-persisted content
     */
    @Override
    public Contentlet translateContent(Contentlet src, Language translateTo, List<Field> fieldsToTranslate, User user)
        throws TranslationException {

        Preconditions.checkNotNull(src, "Unable to translate null content.");
        Preconditions.checkNotNull(fieldsToTranslate, "List of fields to translate can't be null");
        Preconditions.checkArgument(!fieldsToTranslate.isEmpty(), "List of fields to translate can't be empty");

        try {
            Structure srcSt = src.getStructure();

            List<Field> filteredFields = fieldsToTranslate.stream()
                // exclude fileAsset name from translation
                .filter( f-> ! (srcSt.isFileAsset() && f.getVelocityVarName().equals("fileName")))
                // exclude null field values from translation
                .filter(f -> src.getStringProperty(f.getVelocityVarName())!=null)
                .collect(Collectors.toList());

            List<String> valuesToTranslate = filteredFields.stream()
                    .map(f -> src.getStringProperty(f.getVelocityVarName()))
                    .collect(Collectors.toList());

            Language translateFrom = apiProvider.languageAPI().getLanguage(src.getLanguageId());
            List<String> translatedValues = translateStrings(valuesToTranslate, translateFrom, translateTo);

            Contentlet translated = apiProvider.contentletAPI()
                .checkout(src.getInode(), user, false);

            translated.setInode("");
            translated.setLanguageId(translateTo.getId());

            int i = 0;
            for (Field field : filteredFields) {
                translated.setStringProperty(field.getVelocityVarName(), translatedValues.get(i));
                i++;
            }

            return translated;

        } catch (DotContentletStateException | DotDataException | DotSecurityException e) {
            throw new TranslationException("Error translating content", e);
        }
    }
}
