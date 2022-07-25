package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.security.apps.SecretsStore;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import java.io.File;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentWorkflowHandlerTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
    }

    private static final String INPUT_FILE_PATH = "/bundlers-test/workflow/non-step-push-content-workflow.contentworkflow.xml";

    /**
     * Basically this case is described in https://github.com/dotCMS/core/issues/22087
     * Pushing a piece of content that has been passed by the reset-workflow-action
     * therefore lacks of any assigned step.
     */
    @Test
    public void Test_Handle_Push_Content_File_With_No_Step_On_It() {

        final File file = FileTestUtil.getFileInResources(INPUT_FILE_PATH);
        final PublisherConfig config = mock(PublisherConfig.class);
        when(config.getId()).thenReturn(UUIDGenerator.generateUuid());

        ContentWorkflowHandler contentWorkflowHandler = new ContentWorkflowHandler(config);
        try {
            contentWorkflowHandler.handle(file.getParentFile());
        }catch (Exception e){
            Logger.error(ContentWorkflowHandlerTest.class,"Error reading contentworkflow file",e);
            fail("Error reading contentworkflow file");
        }

    }

}
