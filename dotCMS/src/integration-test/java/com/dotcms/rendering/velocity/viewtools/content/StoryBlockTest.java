package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.ContentTypeFieldLayoutAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;

/**
 * Integration Tests related to the Story Block feature.
 *
 * @author Jose Castro
 * @since Aug 23rd, 2022
 */
public class StoryBlockTest extends IntegrationTestBase {

    protected Host defaultSite = null;
    protected final User SYSTEM_USER = APILocator.systemUser();

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void before () {
        defaultSite = Try.of(()->APILocator.getHostAPI().findDefaultHost(
                APILocator.systemUser(), false)).getOrNull();
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b>
     *     {@link com.dotcms.contenttype.business.ContentTypeFieldLayoutAPI#updateField(ContentType, Field, User)}</li>
     *     <li><b>Given Scenario:</b> Transform an existing WYSIWYG field into a Block Editor field.</li>
     *     <li><b>Expected Result:</b> After re-loading the test Content Type and its fields, there must be only one
     *     Block Editor field and no WYSIWYG field.</li>
     * </ul>
     */
    @Test
    public void testTransformingWysiwygIntoStoryBlock() {
        // Initialization
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(SYSTEM_USER);
        final ContentTypeFieldLayoutAPI contentTypeFieldLayoutAPI = APILocator.getContentTypeFieldLayoutAPI();
        final String transformationTypeName = "TestWysiwygTransformationType" + System.currentTimeMillis();
        ContentType transformationType = TestDataUtils.getWysiwygLikeContentType(transformationTypeName, defaultSite,
                null);
        final String transformationTypeId = transformationType.id();
        List<Field> wysiwygFieldList = transformationType.fields(WysiwygField.class);
        final Field wysiwygField = wysiwygFieldList.get(0);
        JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(wysiwygField);
        final Map<String, Object> dataMap = jsonFieldTransformer.mapObject();

        // Test data generation
        final String classKey = "clazz";
        final String classValue = StoryBlockField.class.getCanonicalName();
        final String fieldTypeKey = "fieldType";
        final String fieldTypeValue = "Story-Block";
        final String fieldTypeLabelKey = "fieldTypeLabel";
        final String fieldTypeLabelValue = "Block Editor";
        // Update the respective properties that allow you to transform the WYSIWYG into Block Editor
        dataMap.put(classKey, classValue);
        dataMap.put(fieldTypeKey, fieldTypeValue);
        dataMap.put(fieldTypeLabelKey, fieldTypeLabelValue);
        // Transform the Map into JSON, and the JSON into the expected field object so it can be saved
        jsonFieldTransformer = new JsonFieldTransformer(new JSONObject(dataMap).toString());
        final Field newWysiwygField = jsonFieldTransformer.from();
        FieldLayout fieldLayout = null;
        try {
            fieldLayout = contentTypeFieldLayoutAPI.updateField(transformationType, newWysiwygField, SYSTEM_USER);
        } catch (final DotSecurityException | DotDataException e) {
            Assert.fail("WYSIWYG Field could not be transformed into Story Block");
        }

        // Assertions
        assertTrue("WYSIWYG field could not be transformed into Block Editor", null != fieldLayout);
        transformationType =
                Try.of(() -> contentTypeAPI.find(transformationTypeId)).getOrNull();
        assertTrue("Re-loading the test Content Type failed, this should never happen!", null != transformationType);
        final List<Field> storyBlockFieldList = transformationType.fields(StoryBlockField.class);
        assertTrue("No Block Editor fields were found, which means the transformation failed",
                UtilMethods.isSet(storyBlockFieldList));
        wysiwygFieldList = transformationType.fields(WysiwygField.class);
        assertTrue("There must be no WYSIWYG fields in the end.", null == wysiwygFieldList || wysiwygFieldList.isEmpty());

        // Cleanup
        try {
            contentTypeAPI.delete(transformationType);
        } catch (final DotSecurityException | DotDataException e) {
            // Test Content Type could not be deleted. Just move on
        }
    }

}
