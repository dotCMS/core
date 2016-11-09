package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import com.dotcms.enterprise.LicenseService;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;

public class ESIndexHelperTest {

	/**
	 * Process [["index","dummyIndex"]] to find "dummyIndex"
	 */
	@Test
	public void getIndexNameOrAliasTest_index(){
		String searchIndex = "dummyIndex";
		final ESIndexAPI esIndexAPI = mock(ESIndexAPI.class) ;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		Map<String,String> indexInfo = new HashMap<String,String>();
		indexInfo.put("index", searchIndex);
		List<String> aliasList = new ArrayList<String>();

		when(siteSearchAPI.listIndices()).thenReturn(aliasList);
		when(esIndexAPI.getAliasToIndexMap(aliasList)).thenReturn(new HashMap<String,String>());

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);

		String response = esIndexHelper.getIndexNameOrAlias(indexInfo,esIndexAPI);
		assertNotNull(response);
		assertEquals(searchIndex, response);
	}

	/**
	 * Process [["alias","dummyIndex"]] to find "dummyIndex"
	 */
	@Test
	public void getIndexNameOrAliasTest_alias(){
		String searchIndex = "dummyIndex";
		final ESIndexAPI esIndexAPI = mock(ESIndexAPI.class) ;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		Map<String,String> indexInfo = new HashMap<String,String>();
		indexInfo.put("alias", searchIndex);
		List<String> aliasList = new ArrayList<String>();
		// add name to alias list
		aliasList.add(searchIndex);

		when(siteSearchAPI.listIndices()).thenReturn(aliasList);
		when(esIndexAPI.getAliasToIndexMap(aliasList)).thenReturn(indexInfo);
		when(LicenseService.getLevel()).thenReturn(200);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);

		String response = esIndexHelper.getIndexNameOrAlias(indexInfo,esIndexAPI);
		assertNotNull(response);
		assertEquals(searchIndex, response);
	}

	@Test
	public void getIndexNameOrAliasTest_noIndex(){
		String searchIndex = "dummyIndex";
		final ESIndexAPI esIndexAPI = mock(ESIndexAPI.class) ;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		Map<String,String> indexInfo = new HashMap<String,String>();
		indexInfo.put("index", searchIndex);
		List<String> aliasList = new ArrayList<String>();
		aliasList.add("live_index");

		when(siteSearchAPI.listIndices()).thenReturn(aliasList);
		when(esIndexAPI.getAliasToIndexMap(aliasList)).thenReturn(new HashMap<String,String>());

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);

		String response = esIndexHelper.getIndexNameOrAlias(indexInfo,esIndexAPI);
		assertNotNull(response);
		assertEquals(searchIndex, response);
	}

	/**
	 * Process [["index", null]] should fail and return null
	 */
	@Test
	public void getIndexNameOrAliasTest_nullIndexName(){
		String searchIndex = null;
		final ESIndexAPI esIndexAPI = mock(ESIndexAPI.class) ;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		Map<String,String> indexInfo = new HashMap<String,String>();
		indexInfo.put("index", searchIndex);
		List<String> indexes = new ArrayList<String>();
		indexes.add("live_index");

		when(siteSearchAPI.listIndices()).thenReturn(indexes);
		when(esIndexAPI.getAliasToIndexMap(indexes)).thenReturn(new HashMap<String,String>());

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);

		String response = esIndexHelper.getIndexNameOrAlias(indexInfo,esIndexAPI);
		assertNull(response);
	}

	/**
	 * Process [["something", "dummyIndex"]] should fail and return null
	 */
	@Test
	public void getIndexNameOrAliasTest_invalidIndexKey(){
		String searchIndex = "dummyIndex";
		final ESIndexAPI esIndexAPI = mock(ESIndexAPI.class) ;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		Map<String,String> indexInfo = new HashMap<String,String>();
		indexInfo.put("something", searchIndex);
		List<String> indexes = new ArrayList<String>();
		indexes.add("live_index");

		when(siteSearchAPI.listIndices()).thenReturn(indexes);
		when(esIndexAPI.getAliasToIndexMap(indexes)).thenReturn(new HashMap<String,String>());

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);

		String response = esIndexHelper.getIndexNameOrAlias(indexInfo,esIndexAPI);
		assertNull(response);
	}

	/**
	 * Process [[null, "dummyIndex"]] should fail and return null
	 */
	@Test
	public void getIndexNameOrAliasTest_nullIndexKey(){
		String searchIndex = "dummyIndex";
		final ESIndexAPI esIndexAPI = mock(ESIndexAPI.class) ;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		Map<String,String> indexInfo = new HashMap<String,String>();
		indexInfo.put(null, searchIndex);
		List<String> indexes = new ArrayList<String>();
		indexes.add("live_index");

		when(siteSearchAPI.listIndices()).thenReturn(indexes);
		when(esIndexAPI.getAliasToIndexMap(indexes)).thenReturn(new HashMap<String,String>());

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);

		String response = esIndexHelper.getIndexNameOrAlias(indexInfo,esIndexAPI);
		assertNull(response);
	}

	/**
	 * successful name ending on .zip
	 */
	@Test
	public void isSnapshotFilename(){
		String indexName = "Live_dummy.zip";
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		boolean response = esIndexHelper.isSnapshotFilename(indexName);
		assertTrue(response);
	}

	/**
	 * successful name ending on .zip
	 */
	@Test
	public void isSnapshotFilename_mixCase(){
		String indexName = "Live_dummy.zIp";
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		boolean response = esIndexHelper.isSnapshotFilename(indexName);
		assertTrue(response);
	}

	/**
	 * wrong name ending on .tar
	 */
	@Test
	public void isSnapshotFilename_wrongName(){
		String indexName = "Live_dummy.tar";
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		boolean response = esIndexHelper.isSnapshotFilename(indexName);
		assertFalse(response);
	}

	/**
	 * wrong name with no extension
	 */
	@Test
	public void isSnapshotFilename_noExtension(){
		String indexName = "Live_dummy";
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		boolean response = esIndexHelper.isSnapshotFilename(indexName);
		assertFalse(response);
	}

	/**
	 * wrong name, null value
	 */
	@Test
	public void isSnapshotFilename_null(){
		String indexName = null;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		boolean response = esIndexHelper.isSnapshotFilename(indexName);
		assertFalse(response);
	}

	/**
	 * wrong name, empty
	 */
	@Test
	public void isSnapshotFilename_empty(){
		String indexName = StringUtils.EMPTY;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		boolean response = esIndexHelper.isSnapshotFilename(indexName);
		assertFalse(response);
	}

	/**
	 * Getting live_dummy from live_dummy.zip
	 */
	@Test
	public void getIndexFromFilename(){
		String indexName = "live_dummy.zip";
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		String response  = esIndexHelper.getIndexFromFilename(indexName);
		assertEquals(response, "live_dummy");
	}

	/**
	 * Getting live_dummy from Live_dummy.Zip
	 */
	@Test
	public void getIndexFromFilename_mixCase(){
		String indexName = "Live_dummy.Zip";
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		String response  = esIndexHelper.getIndexFromFilename(indexName);
		assertEquals(response, "live_dummy");
	}

	/**
	 * failing with null
	 */
	@Test
	public void getIndexFromFilename_null(){
		String indexName = null;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		String response  = esIndexHelper.getIndexFromFilename(indexName);
		assertNull(response);
	}

	/**
	 * failing with null
	 */
	@Test
	public void getIndexFromFilename_empty(){
		String indexName = StringUtils.EMPTY;
		final SiteSearchAPI siteSearchAPI = mock(SiteSearchAPI.class);
		final LicenseService LicenseService = mock(LicenseService.class);

		ESIndexHelper esIndexHelper = new ESIndexHelper(siteSearchAPI,LicenseService);
		String response  = esIndexHelper.getIndexFromFilename(indexName);
		assertNull(response);
	}
}

