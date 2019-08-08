package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.integrationtestutil.content.ContentUtils;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;

public class FieldUtilTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testSetFieldInternationalization() throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();
        final ContentType languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE);

        final String fieldName = String.format("test%d", new Date().getTime());
        final String key = String.format("ContactUs.%s.name", fieldName);
        final String languageVariableValue = "test";
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final boolean live = false;

        final Contentlet content = ContentUtils.createTestKeyValueContent(key, languageVariableValue, languageId, languageVariableContentType, systemUser);

        try {
            final ContentType contactUs = APILocator.getContentTypeAPI(systemUser).find("ContactUs");
            final ContentTypeInternationalization contentTypeInternationalization =
                    new ContentTypeInternationalization(languageId, live, systemUser);

            final Map<String, Object> fieldMap = map("name", fieldName, "variable", fieldName);
            FieldUtil.setFieldInternationalization(contactUs, contentTypeInternationalization, fieldMap);

            assertEquals(languageVariableValue, fieldMap.get("name"));
        } finally {

            //Clean up
            if (null != content) {
                deleteContentlets(systemUser, content);
            }
        }
    }
}
