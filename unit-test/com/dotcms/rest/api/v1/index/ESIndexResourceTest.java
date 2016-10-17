package com.dotcms.rest.api.v1.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexHelper;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.business.LayoutAPI;
import com.liferay.portal.model.User;

public class ESIndexResourceTest {


    public ESIndexResourceTest() {

	}

	/**
	 * Tests the download of a live index named live_20161004130547
	 * @throws Exception
	 */
    @Test
    public void testDownloadSnapshot_liveIndex() throws Exception {

    	final String liveIndex = "live_20161004130547";
    	final String workingIndex = "work_20161004130447";
    	final String requestParams = "/index/" + liveIndex;

    	final HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final WebResource webResource = mock(WebResource.class);
        final ESIndexAPI indexAPI = mock(ESIndexAPI.class);
        final ESIndexHelper indexHelper = mock(ESIndexHelper.class);
        final ResponseUtil responseUtil = mock(ResponseUtil.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final IndiciesAPI indiciesAPI = mock(IndiciesAPI.class);

        final User user = new User();
        IndiciesInfo indiciesInfo = new IndiciesInfo();
        indiciesInfo.live = liveIndex;
        indiciesInfo.working = workingIndex;
        File tempFile = File.createTempFile(liveIndex, null);
        tempFile.deleteOnExit();
        Map<String,String> paramsMap = new HashMap<String,String>();
        paramsMap.put("index", liveIndex);

        when(webResource.init(requestParams, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);
        when(initDataObject.getParamsMap()).thenReturn(paramsMap);
        when(layoutAPI.doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", initDataObject.getUser())).thenReturn(true);
        when(indiciesAPI.loadIndicies()).thenReturn(indiciesInfo);
        when(indexHelper.getIndexNameOrAlias(paramsMap,indexAPI)).thenReturn(liveIndex);
        when(indexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", liveIndex)).thenReturn(tempFile);

        ESIndexResource esIndexResource = new ESIndexResource(indexAPI, indexHelper, responseUtil, webResource, layoutAPI, indiciesAPI);

        final Response response1 = esIndexResource.snapshotIndex(request, requestParams);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
    }

    /**
	 * Tests the download of the default index named live_20161004130547
	 * @throws Exception
	 */
    @Test
    public void testDownloadSnapshot_liveDefaultIndex() throws Exception {

    	final String liveIndex = "live_20161004130547";
    	final String workingIndex = "work_20161004130447";
    	final String requestParams = "/index/live";

    	final HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final WebResource webResource = mock(WebResource.class);
        final ESIndexAPI indexAPI = mock(ESIndexAPI.class);
        final ESIndexHelper indexHelper = mock(ESIndexHelper.class);
        final ResponseUtil responseUtil = mock(ResponseUtil.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final IndiciesAPI indiciesAPI = mock(IndiciesAPI.class);

        final User user = new User();
        IndiciesInfo indiciesInfo = new IndiciesInfo();
        indiciesInfo.live = liveIndex;
        indiciesInfo.working = workingIndex;
        File tempFile = File.createTempFile(liveIndex, null);
        tempFile.deleteOnExit();
        Map<String,String> paramsMap = new HashMap<String,String>();
        paramsMap.put("index", "live");

        when(webResource.init(requestParams, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);
        when(initDataObject.getParamsMap()).thenReturn(paramsMap);
        when(layoutAPI.doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", initDataObject.getUser())).thenReturn(true);
        when(indiciesAPI.loadIndicies()).thenReturn(indiciesInfo);
        when(indexHelper.getIndexNameOrAlias(paramsMap, indexAPI)).thenReturn("live");
        when(indexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", liveIndex)).thenReturn(tempFile);

        ESIndexResource esIndexResource = new ESIndexResource(indexAPI, indexHelper, responseUtil, webResource, layoutAPI, indiciesAPI);

        final Response response1 = esIndexResource.snapshotIndex(request, requestParams);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
    }

    /**
	 * Tests the download of a working index named work_20161004130447
	 * @throws Exception
	 */
    @Test
    public void testDownloadSnapshot_workingIndex() throws Exception {

    	final String liveIndex = "live_20161004130547";
    	final String workingIndex = "work_20161004130447";
    	final String requestParams = "/index/" + workingIndex;

    	final HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final WebResource webResource = mock(WebResource.class);
        final ESIndexAPI indexAPI = mock(ESIndexAPI.class);
        final ESIndexHelper indexHelper = mock(ESIndexHelper.class);
        final ResponseUtil responseUtil = mock(ResponseUtil.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final IndiciesAPI indiciesAPI = mock(IndiciesAPI.class);

        final User user = new User();
        IndiciesInfo indiciesInfo = new IndiciesInfo();
        indiciesInfo.live = liveIndex;
        indiciesInfo.working = workingIndex;
        File tempFile = File.createTempFile(workingIndex, null);
        tempFile.deleteOnExit();
        Map<String,String> paramsMap = new HashMap<String,String>();
        paramsMap.put("index", workingIndex);

        when(webResource.init(requestParams, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);
        when(initDataObject.getParamsMap()).thenReturn(paramsMap);
        when(layoutAPI.doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", initDataObject.getUser())).thenReturn(true);
        when(indiciesAPI.loadIndicies()).thenReturn(indiciesInfo);
        when(indexHelper.getIndexNameOrAlias(paramsMap, indexAPI)).thenReturn(workingIndex);
        when(indexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", workingIndex)).thenReturn(tempFile);

        ESIndexResource esIndexResource = new ESIndexResource(indexAPI, indexHelper, responseUtil, webResource, layoutAPI, indiciesAPI);

        final Response response1 = esIndexResource.snapshotIndex(request, requestParams);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
    }

    /**
	 * Tests the download of a working index named work_20161004130447
	 * @throws Exception
	 */
    @Test
    public void testDownloadSnapshot_workingDefaultIndex() throws Exception {

    	final String liveIndex = "live_20161004130547";
    	final String workingIndex = "work_20161004130447";
    	final String requestParams = "/index/working";

    	final HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final WebResource webResource = mock(WebResource.class);
        final ESIndexAPI indexAPI = mock(ESIndexAPI.class);
        final ESIndexHelper indexHelper = mock(ESIndexHelper.class);
        final ResponseUtil responseUtil = mock(ResponseUtil.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final IndiciesAPI indiciesAPI = mock(IndiciesAPI.class);

        final User user = new User();
        IndiciesInfo indiciesInfo = new IndiciesInfo();
        indiciesInfo.live = liveIndex;
        indiciesInfo.working = workingIndex;
        File tempFile = File.createTempFile(workingIndex, null);
        tempFile.deleteOnExit();
        Map<String,String> paramsMap = new HashMap<String,String>();
        paramsMap.put("index", "working");

        when(webResource.init(requestParams, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);
        when(initDataObject.getParamsMap()).thenReturn(paramsMap);
        when(layoutAPI.doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", initDataObject.getUser())).thenReturn(true);
        when(indiciesAPI.loadIndicies()).thenReturn(indiciesInfo);
        when(indexHelper.getIndexNameOrAlias(paramsMap, indexAPI)).thenReturn(workingIndex);
        when(indexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", workingIndex)).thenReturn(tempFile);

        ESIndexResource esIndexResource = new ESIndexResource(indexAPI, indexHelper, responseUtil, webResource, layoutAPI, indiciesAPI);

        final Response response1 = esIndexResource.snapshotIndex(request, requestParams);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
    }

    /**
	 * Tests call without index name
	 * @throws Exception
	 */
    @Test
    public void testDownloadSnapshot_noIndex() throws Exception {

    	final String requestParams = "/index/";

    	final HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final WebResource webResource = mock(WebResource.class);
        final ESIndexAPI indexAPI = mock(ESIndexAPI.class);
        final ESIndexHelper indexHelper = mock(ESIndexHelper.class);
        final ResponseUtil responseUtil = mock(ResponseUtil.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final IndiciesAPI indiciesAPI = mock(IndiciesAPI.class);

        final User user = new User();
        Map<String,String> paramsMap = new HashMap<String,String>();

        when(webResource.init(requestParams, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);
        when(initDataObject.getParamsMap()).thenReturn(paramsMap);
        when(layoutAPI.doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", initDataObject.getUser())).thenReturn(true);
        when(indexHelper.getIndexNameOrAlias(paramsMap, indexAPI)).thenReturn(null);
        when(responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, Locale.getDefault(), null, "snapshot.wrong.arguments")).thenReturn(Response.status(Status.BAD_REQUEST).build());

        ESIndexResource esIndexResource = new ESIndexResource(indexAPI, indexHelper, responseUtil, webResource, layoutAPI, indiciesAPI);

        Response response1 = esIndexResource.snapshotIndex(request, requestParams);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 400);
    }
}

