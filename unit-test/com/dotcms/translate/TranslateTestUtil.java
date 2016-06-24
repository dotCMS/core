package com.dotcms.translate;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;

import java.util.Arrays;
import java.util.List;

public class TranslateTestUtil {
    static final Language english = new Language(1, "en", "US", "English", "United States");
    static final Language spanish = new Language(2, "es", "ES", "Espanol", "Espana");
    static final Language french = new Language(3, "fr", "FR", "French", "France");
    static final String TEXT_FIELD_VN =  "textFieldVN";
    static final String WYSIWYG_VN =  "wVN";
    static final String TEXT_AREA_VN =  "textAreaVN";

    public static Contentlet getEnglishContent() {
        // mock content and return the created fields when requested
        Contentlet toTranslate = new Contentlet();
        toTranslate.setLanguageId(english.getId());
        toTranslate.setIdentifier("identifier");

        toTranslate.setStringProperty(TEXT_FIELD_VN,"English Value 1");
        toTranslate.setStringProperty(WYSIWYG_VN,"English Value 2");
        toTranslate.setStringProperty(TEXT_AREA_VN,"English Value 3");

        List<Field> fieldsToTranslate = getFieldsForContent();
        toTranslate.setProperty("fieldsToTranslate", fieldsToTranslate);

        return toTranslate;
    }

    public static List<Field> getFieldsForContent() {
        // create three fields
        Field textField = new Field();
        textField.setVelocityVarName(TEXT_FIELD_VN);
        textField.setFieldType(Field.FieldType.TEXT.toString());

        Field wField = new Field();
        wField.setVelocityVarName(WYSIWYG_VN);
        wField.setFieldType(Field.FieldType.WYSIWYG.toString());

        Field textAreaField = new Field();
        textAreaField.setVelocityVarName(TEXT_AREA_VN);
        textAreaField.setFieldType(Field.FieldType.TEXT_AREA.toString());

        return Arrays.asList(textField, wField, textAreaField);
    }

    public static List<Language> getTranslateToAsList() {
        return Arrays.asList(spanish, french);
    }
}
