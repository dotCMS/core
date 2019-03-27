package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.StringPool;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;

public class JSONToolFTest {

    @Test
    public void testJSONFetch_GivenUnicodeTextInContent_ShouldReturnProperlyEncoded()
        throws DotDataException, DotSecurityException, JSONException {
        Contentlet contentWithUnicode = null;
        try {
            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            final ContentType contentGenericType = contentTypeAPI.find("webPageContent");
            final String unicodeText = "Numéro de téléphone";

            final long spanishId = 2;
            final ContentletDataGen contentletDataGen = new ContentletDataGen(contentGenericType.id());
            contentWithUnicode = contentletDataGen.setProperty("title", "TestContent")
                .setProperty("body", unicodeText ).languageId(spanishId).nextPersisted();

            final HttpServletRequest request = ServletTestRunner.localRequest.get();
            final String serverName = request.getServerName();
            final long serverPort = request.getServerPort();

            final JSONTool jsonTool = new JSONTool();
            final JSONObject
                object =
                (JSONObject) jsonTool
                    .fetch("http://" + serverName + StringPool.COLON + serverPort + "/api/content/inode/"
                        + contentWithUnicode.getInode());

            assertEquals(unicodeText, ((JSONObject) ((JSONArray) object.get("contentlets")).get(0)).get("body"));
        } finally {
            DateUtil.sleep(1000L);
            ContentletDataGen.remove(contentWithUnicode);
        }
    }
}
