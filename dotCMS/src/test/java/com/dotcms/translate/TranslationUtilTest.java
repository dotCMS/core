package com.dotcms.translate;

import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TranslationUtilTest {

    public static final List<String> filterTypes = Arrays
        .asList(FieldType.TEXT.toString(),FieldType.WYSIWYG.toString(),FieldType.TEXT_AREA.toString());

    @Test
    public void testFilterFields_NullFilter_NullExclude() throws Exception {

        List<Field> fieldsToFilter = new ArrayList<>();

        Field dateField = new Field();
        dateField.setFieldType(FieldType.DATE.name());

        Field checkBoxField = new Field();
        checkBoxField.setFieldType(FieldType.CHECKBOX.name());

        fieldsToFilter.add(dateField);
        fieldsToFilter.add(checkBoxField);

        List<Field> result = TranslationUtil.getUtil().filterFields(fieldsToFilter, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterFields_FilterTypeDate_NullExclude() throws Exception {

        List<Field> fieldsToFilter = new ArrayList<>();

        Field dateField = new Field();
        dateField.setFieldType(FieldType.DATE.toString());

        Field checkBoxField = new Field();
        checkBoxField.setFieldType(FieldType.CHECKBOX.toString());

        fieldsToFilter.add(dateField);
        fieldsToFilter.add(checkBoxField);

        List<Field> result = TranslationUtil.getUtil()
            .filterFields(fieldsToFilter, Collections.singletonList(FieldType.DATE.toString()), null);

        assertEquals(checkBoxField, result.get(0));
    }

    @Test
    public void testFilterFields_FilterTypeDate_OneExclusion() throws Exception {

        List<Field> fieldsToFilter = new ArrayList<>();

        Field dateField = new Field();
        dateField.setFieldType(FieldType.DATE.toString());

        Field checkBoxField = new Field();
        checkBoxField.setFieldType(FieldType.CHECKBOX.toString());
        checkBoxField.setVelocityVarName("velVar");

        fieldsToFilter.add(dateField);
        fieldsToFilter.add(checkBoxField);

        List<Field> result = TranslationUtil.getUtil()
            .filterFields(fieldsToFilter, Collections.singletonList(FieldType.DATE.toString()),
                Collections.singletonList("velVar"));

        assertEquals(result, Collections.singletonList(dateField));
    }

    @Test
    public void testFilterFields_FilterTypeDate_OneBadExclusion() throws Exception {

        List<Field> fieldsToFilter = new ArrayList<>();

        Field dateField = new Field();
        dateField.setFieldType(FieldType.DATE.toString());

        Field checkBoxField = new Field();
        checkBoxField.setFieldType(FieldType.CHECKBOX.toString());
        checkBoxField.setVelocityVarName("velVar");

        fieldsToFilter.add(dateField);
        fieldsToFilter.add(checkBoxField);

        List<Field> result = TranslationUtil.getUtil()
            .filterFields(fieldsToFilter, Collections.singletonList(FieldType.DATE.toString()),
                Collections.singletonList("thisIsNotaVelVarName"));



        assertEquals(result, Collections.singletonList(dateField));
    }

    @Test
    public void testFilterFields_FilterTextTypes_NullExclude() throws Exception {

        List<Field> fieldsToFilter = new ArrayList<>();

        Field dateField = new Field();
        dateField.setFieldType(FieldType.DATE.toString());

        Field checkBoxField = new Field();
        checkBoxField.setFieldType(FieldType.CHECKBOX.toString());

        Field textField = new Field();
        textField.setFieldType(FieldType.TEXT.toString());
        textField.setFieldContentlet("text1");

        Field textFieldButNumeric = new Field();
        textFieldButNumeric.setFieldType(FieldType.TEXT.toString());
        textFieldButNumeric.setFieldContentlet("integer1");

        Field wField = new Field();
        wField.setFieldType(FieldType.WYSIWYG.toString());

        Field textAreaField = new Field();
        textAreaField.setFieldType(FieldType.TEXT_AREA.toString());

        fieldsToFilter.add(dateField);
        fieldsToFilter.add(checkBoxField);
        fieldsToFilter.add(textField);
        fieldsToFilter.add(textFieldButNumeric);
        fieldsToFilter.add(wField);
        fieldsToFilter.add(textAreaField);

        List<Field> result = TranslationUtil.getUtil()
            .filterFields(fieldsToFilter, filterTypes, null);

        List<Field> expectedList = Arrays.asList(textField, wField, textAreaField);

        assertEquals(result, expectedList);
    }
}