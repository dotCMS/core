package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test {@link MessageTool}
 * @author jsanca
 */
public class MessageToolTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    /**
     * Method to test: {@link JSONTool#generate(String)}
     * Given Scenario: Parsing an plain object with new lines and spaces
     * ExpectedResult: Expected a list of map with the properties
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_message_tool() throws Exception {

        final StringBuffer stringBuffer = new StringBuffer();
        final MessageTool.MessageDelegate messageDelegate = new MessageTool.MessageDelegate() {
            @Override
            public void pushMessage(SystemMessage message, List<String> users) {

                stringBuffer.append(message.getMessage());
            }
        };
        final MessageTool messageTool   = new MessageTool(messageDelegate);
        final String message = "test";
        messageTool.sendInfo(message, 10, Arrays.asList("dotcms.1"));
        assertEquals(message, stringBuffer.toString());
    }


}
