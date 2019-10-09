package com.dotcms.keyvalue.busines;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.integrationtestutil.content.ContentUtils;
import com.dotcms.keyvalue.business.KeyValueAPIImpl;
import com.dotcms.keyvalue.model.KeyValue;
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

import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class KeyValueAPIImplTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void shouldReturnLiveVersion() throws DotSecurityException, DotDataException {
        final User systemUser = APILocator.systemUser();
        final ContentType languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE);

        final String key = String.format("test%d", new Date().getTime());
        final String value = "test";
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        final Contentlet content = ContentUtils.createTestKeyValueContent(key, value, languageId, languageVariableContentType, systemUser);

        try {

            final KeyValue keyValue = APILocator.getKeyValueAPI().get(key, languageId, languageVariableContentType, systemUser, true, false);

            assertEquals(key, keyValue.getKey());
            assertEquals(value, keyValue.getValue());
        } finally {

            //Clean up
            if (null != content) {
                deleteContentlets(systemUser, content);
            }
        }
    }

    @Test
    public void shouldReturnNull() throws DotSecurityException, DotDataException {
        final User systemUser = APILocator.systemUser();
        final ContentType languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE);

        final String key = String.format("test%d", new Date().getTime());
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        final KeyValue keyValue = APILocator.getKeyValueAPI().get(key, languageId, languageVariableContentType, systemUser, true, false);

        assertNull(keyValue);

    }

    @Test
    public void shouldReturnWorkingVersion() throws DotSecurityException, DotDataException, InterruptedException {
        final User systemUser = APILocator.systemUser();
        final ContentType languageVariableContentType = APILocator.getContentTypeAPI(systemUser).find(LanguageVariableAPI.LANGUAGEVARIABLE);

        final String key = String.format("test%d", new Date().getTime());
        final String liveValue = "test";
        final String workingValue = "test 2";
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        final Contentlet content = ContentUtils.createTestKeyValueContent(key, liveValue, languageId, languageVariableContentType, systemUser);

        try {
            ContentUtils.updateTestKeyValueContent(content, key, workingValue, languageId, languageVariableContentType, systemUser);
            Thread.sleep(1000);
            final KeyValue keyValue = APILocator.getKeyValueAPI().get(key, languageId, languageVariableContentType, systemUser, false, false);

            assertEquals(key, keyValue.getKey());
            assertEquals(workingValue, keyValue.getValue());
        } finally {

            //Clean up
            if (null != content) {
                deleteContentlets(systemUser, content);
            }
        }
    }
}
