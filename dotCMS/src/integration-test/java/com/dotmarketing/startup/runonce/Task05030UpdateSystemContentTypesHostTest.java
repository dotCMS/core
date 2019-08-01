package com.dotmarketing.startup.runonce;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05030UpdateSystemContentTypesHostTest {

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, DotSecurityException {

        ContentType commentsContentType = null;

        try {

            try {
                commentsContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find("Comments");
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (null == commentsContentType) {

                // Creating a Comments content type just to make sure the task is working
                commentsContentType = new ContentTypeDataGen().fixed(Boolean.TRUE)
                        .name("Comments").velocityVarName("Comments").nextPersisted();
            }

            final Task05030UpdateSystemContentTypesHost updateTypesHost = new Task05030UpdateSystemContentTypesHost();

            updateTypesHost.executeUpgrade();

            final ContentTypeAPI contentTypeAPI = APILocator
                    .getContentTypeAPI(APILocator.systemUser());
            final List<ContentType> systemTypesWithoutSystemHost = contentTypeAPI.findAll().stream()
                    .filter((contentType) -> contentType.fixed() && !contentType.host()
                            .equals(Host.SYSTEM_HOST))
                    .collect(Collectors.toList());

            assertTrue(systemTypesWithoutSystemHost.isEmpty());

            final ContentType commentsType = contentTypeAPI.find("Comments");

            assertFalse(commentsType.fixed());
        } finally {
            ContentTypeDataGen.remove(commentsContentType);
        }

    }

}