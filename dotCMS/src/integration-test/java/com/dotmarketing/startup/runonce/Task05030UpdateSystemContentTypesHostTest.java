package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class Task05030UpdateSystemContentTypesHostTest {

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, DotSecurityException {
        final Task05030UpdateSystemContentTypesHost updateTypesHost = new Task05030UpdateSystemContentTypesHost();

        updateTypesHost.executeUpgrade();

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        final List<ContentType> systemTypesWithoutSystemHost = contentTypeAPI.findAll().stream()
                .filter((contentType)->contentType.fixed() && !contentType.host().equals(Host.SYSTEM_HOST))
                .collect(Collectors.toList());

        assertTrue(systemTypesWithoutSystemHost.isEmpty());

        final ContentType commentsType = contentTypeAPI.find("Comments");

        assertFalse(commentsType.fixed());
    }
}
