package com.dotcms.contenttype.model.field.layout;


import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.datagen.*;

import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;

import com.dotcms.datagen.ContentTypeDataGen;

import com.dotcms.integrationtestutil.content.ContentUtils;

import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldUtilTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testSetFieldInternationalization() throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();
        final ContentType languageVariableContentType = ContentTypeDataGen.createLanguageVariableContentType();
        ContentType formContentType = null;

        final String fieldName = String.format("test%d", new Date().getTime());

        formContentType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.FORM)
                .fields(
                        CollectionsUtils.list(
                                ImmutableRowField.builder()
                                        .name("roe")
                                        .sortOrder(5)
                                        .build(),
                                ImmutableColumnField.builder()
                                        .name("column")
                                        .sortOrder(6)
                                        .build(),
                                ImmutableTextField.builder()
                                        .name(fieldName)
                                        .sortOrder(7)
                                        .build()
                        )
                )
                .nextPersisted();

        final String key = String.format("%s.%s.name", formContentType.variable(), fieldName);
        final String languageVariableValue = "test";
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final boolean live = false;

        ContentUtils.createTestKeyValueContent(key, languageVariableValue, languageId, languageVariableContentType, systemUser);

        final ContentTypeInternationalization contentTypeInternationalization =
                new ContentTypeInternationalization(languageId, live, systemUser);

        Map<String, Object> fieldMap = map("name", fieldName, "variable", fieldName);
        fieldMap  = APILocator.getContentTypeFieldAPI().getFieldInternationalization(formContentType, contentTypeInternationalization, fieldMap);

        assertEquals(languageVariableValue, fieldMap.get("name"));
    }

    @Test
    public void testSetFieldInternationalizationWithAnonymousUser() throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();
        final ContentType languageVariableContentType = ContentTypeDataGen.createLanguageVariableContentType();
        ContentType formContentType = null;

        final String fieldName = String.format("test%d", new Date().getTime());

        formContentType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.FORM)
                .fields(
                        CollectionsUtils.list(
                                ImmutableRowField.builder()
                                        .name("roe")
                                        .sortOrder(5)
                                        .build(),
                                ImmutableColumnField.builder()
                                        .name("column")
                                        .sortOrder(6)
                                        .build(),
                                ImmutableTextField.builder()
                                        .name(fieldName)
                                        .sortOrder(7)
                                        .build()
                        )
                )
                .nextPersisted();

        final String key = String.format("%s.%s.name", formContentType.variable(), fieldName);
        final String languageVariableValue = "test";
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final boolean live = true;

        final Contentlet content = ContentUtils.createTestKeyValueContent(key, languageVariableValue, languageId, languageVariableContentType, systemUser);
        PermissionUtilTest.addAnonymousUser(content);

        final ContentTypeInternationalization contentTypeInternationalization =
                new ContentTypeInternationalization(languageId, live, null);

        Map<String, Object> fieldMap = map("name", fieldName, "variable", fieldName);
        fieldMap  = APILocator.getContentTypeFieldAPI().getFieldInternationalization(formContentType, contentTypeInternationalization, fieldMap);

        assertEquals(languageVariableValue, fieldMap.get("name"));
    }


}
