package com.dotcms.publisher.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.MimetypesFileTypeMap;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.servlet.SolrRequestParsers;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import edu.emory.mathcs.backport.java.util.Arrays;
/**
 * This class manage all the operation we can do over a from/to a PublishQueue index (search, add and delete)
 * @author Oswaldo
 *
 */
public class PublisherUtil {	

	/**
	 * Adding SolrInputDocument to PublishQueue Index
	 * @param PublishQueueServerUrl PublishQueue Server Url
	 * @param doc SolrInputDocument to include
	 * @return boolean, true if the element were added to the PublishQueue index
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public static void addToPublishQueueIndex(String PublishQueueServerUrl, SolrInputDocument doc) throws SolrServerException, IOException {		
		CommonsHttpSolrServer server = new CommonsHttpSolrServer(PublishQueueServerUrl);
		server.setParser(new XMLResponseParser());
		/*Add collection to solr index*/
		Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		docs.add( doc );			
		UpdateResponse rsp = server.add( docs );
		Logger.debug(PublisherUtil.class, "ADDING SORL INDEX: "+rsp);
		/*Commit collection to solr index*/
		UpdateResponse rsp2 = server.commit();		
		Logger.debug(PublisherUtil.class, "COMMITING SORL INDEX: "+rsp2);		
	}

	/**
	 * Adding documents collection to PublishQueue Index
	 * @param PublishQueueServerUrl PublishQueue Server Url
	 * @param docs Collection<SolrInputDocument> collection of elements to include
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public static void addToPublishQueueIndex(String PublishQueueServerUrl, Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
		CommonsHttpSolrServer server = new CommonsHttpSolrServer(PublishQueueServerUrl);
		server.setParser(new XMLResponseParser());
		/*Add collection to solr index*/
		UpdateResponse rsp = server.add( docs );
		Logger.debug(PublisherUtil.class, "ADDING SORL INDEX: "+rsp);
		/*Commit collection to solr index*/
		UpdateResponse rsp2 = server.commit();		
		Logger.debug(PublisherUtil.class, "COMMITING SORL INDEX: "+rsp2);		
	}

	/**
	 * Deleting document from PublishQueue Index
	 * @param PublishQueueServerUrl PublishQueue Server Url
	 * @param id ID of the element to delete 
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public static void deleteFromPublishQueueIndexById(String PublishQueueServerUrl, String id) throws SolrServerException, IOException {
		CommonsHttpSolrServer server = new CommonsHttpSolrServer(PublishQueueServerUrl);
		server.setParser(new XMLResponseParser());
		/*Add collection to solr index*/
		UpdateResponse rsp = server.deleteById(id);
		Logger.debug(PublisherUtil.class, "DELETING SORL INDEX: "+rsp);
		/*Commit collection to solr index*/
		UpdateResponse rsp2 = server.commit();	
		Logger.debug(PublisherUtil.class, "COMMITING SORL INDEX: "+rsp2);		
	}

	/**
	 * Deleting document from PublishQueue Index
	 * @param PublishQueueServerUrl PublishQueue Server Url
	 * @param ids List od ID's of the elements to delete 
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public static boolean deleteFromPublishQueueIndexById(String PublishQueueServerUrl, List<String> ids) {
		try {
			CommonsHttpSolrServer server = new CommonsHttpSolrServer(PublishQueueServerUrl);
			server.setParser(new XMLResponseParser());
			/*Add collection to solr index*/
			UpdateResponse rsp = server.deleteById(ids);
			Logger.debug(PublisherUtil.class, "DELETING SORL INDEX: "+rsp);
			/*Commit collection to solr index*/
			UpdateResponse rsp2 = server.commit();	
			Logger.debug(PublisherUtil.class, "COMMITING SORL INDEX: "+rsp2);
			return true;
		} catch (Exception e) {
			Logger.error(PublisherUtil.class, e.getMessage(), e);
			return false;
		} 
	}

	/**
	 * Print documents and facets
	 * 
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public static void print(QueryResponse response) {
		SolrDocumentList docs = response.getResults();
		if (docs != null) {
			Logger.debug(PublisherUtil.class, docs.getNumFound() + " documents found, "+ docs.size() + " returned : ");
			for (int i = 0; i < docs.size(); i++) {
				SolrDocument doc = docs.get(i);
				Logger.debug(PublisherUtil.class,"\t" + doc.toString());
			}
		}

		List<FacetField> fieldFacets = response.getFacetFields();
		if (fieldFacets != null && fieldFacets.isEmpty()) {
			for (FacetField fieldFacet : fieldFacets) {
				Logger.debug(PublisherUtil.class,"\t" + fieldFacet.getName() + " :\t");
				if (fieldFacet.getValueCount() > 0) {
					for (Count count : fieldFacet.getValues()) {
						Logger.debug(PublisherUtil.class,count.getName() + "["+ count.getCount() + "]\t");
					}
				}
				Logger.debug(PublisherUtil.class,"");
			}
		}

		Map<String, Integer> queryFacets = response.getFacetQuery();
		if (queryFacets != null && !queryFacets.isEmpty()) {
			Logger.debug(PublisherUtil.class,"\nQuery facets : ");
			for (String queryFacet : queryFacets.keySet()) {
				Logger.debug(PublisherUtil.class,"\t" + queryFacet + "\t["+ queryFacets.get(queryFacet) + "]");
			}
			Logger.debug(PublisherUtil.class,"");
		}

		NamedList<NamedList<Object>> spellCheckResponse = (NamedList<NamedList<Object>>) response.getResponse().get("spellcheck");

		if (spellCheckResponse != null) {
			Iterator<Entry<String, NamedList<Object>>> wordsIterator = spellCheckResponse.iterator();

			while (wordsIterator.hasNext()) {
				Entry<String, NamedList<Object>> entry = wordsIterator.next();
				String word = entry.getKey();
				NamedList<Object> spellCheckWordResponse = entry.getValue();
				boolean correct = spellCheckWordResponse.get("frequency").equals(1);
				Logger.debug(PublisherUtil.class,"Word: " + word + ",\tCorrect?: " + correct);
				NamedList<Integer> suggestions = (NamedList<Integer>) spellCheckWordResponse.get("suggestions");
				if (suggestions != null && suggestions.size() > 0) {
					Logger.debug(PublisherUtil.class,"Suggestions : ");
					Iterator<Entry<String, Integer>> suggestionsIterator = suggestions.iterator();
					while (suggestionsIterator.hasNext()) {
						Logger.debug(PublisherUtil.class,"\t"+ suggestionsIterator.next().getKey());
					}
				}
				Logger.debug(PublisherUtil.class,"");
			}
		}
	}

	/**
	 * Different types of searches
	 */

	/**
	 * Execute a search in the PublishQueue index, passing all the parameter directly in a url query.
	 * For example, query:"indent=on&version=2.2&q=%2Bcat%3Aelectronics&fq=&start=0&rows=20&fl=*%2Cscore&qt=&wt=&explainOther=&hl.fl="
	 * @param PublishQueueServerUrl PublishQueue Server Url
	 * @param query PublishQueue query
	 * @return QueryResponse
	 * @throws SolrServerException
	 * @throws MalformedURLException
	 */
	public static QueryResponse executePublishQueueGenericSearch(String PublishQueueServerUrl, String query) throws SolrServerException, MalformedURLException {
		CommonsHttpSolrServer server = new CommonsHttpSolrServer(PublishQueueServerUrl);
		server.setParser(new XMLResponseParser());
		SolrParams solrParams = SolrRequestParsers.parseQueryString(query);
		return server.query(solrParams);
	}

	/**
	 * Execute a search in the specified PublishQueue index using a url parameter 
	 * @param PublishQueueServerUrl PublishQueue Server Url
	 * @param start initial value to return
	 * @param rows number of row to return
	 * @param queryType  qt=spellcheck || qt=spellchecker (optional)
	 * @param facet Facet parameter. Values accepted "on" or "off"
	 * @param ident Ident parameter. Values accepted "on" or "off"
	 * @param query PublishQueue query
	 * @param myCollection PublishQueue collection name (optional)
	 * @param username PublishQueue username (optional)
	 * @param password PublishQueue password (optional)
	 * @return QueryResponse
	 * @throws SolrServerException
	 * @throws MalformedURLException
	 */
	public static QueryResponse executeURLSolrParamsSearch(String PublishQueueServerUrl, int start, int rows, String queryType,String facet, String ident, String query, String myCollection, String username, String password)
	throws SolrServerException, MalformedURLException {

		CommonsHttpSolrServer server = new CommonsHttpSolrServer(PublishQueueServerUrl);
		server.setParser(new XMLResponseParser());

		StringBuffer request = new StringBuffer();
		if(UtilMethods.isSet(myCollection)){
			request.append("collectionName=" + myCollection);
		}
		if(UtilMethods.isSet(username)){
			request.append("&username=" + username);
		}
		if(UtilMethods.isSet(password)){
			request.append("&password=" + password);
		}
		if(UtilMethods.isSet(ident)){
			request.append("&indent="+ident);
		}
		if(UtilMethods.isSet(facet)){
			request.append("&facet=" + facet);
		}
		if(UtilMethods.isSet(queryType)){
			// qt=spellcheck || qt=spellchecker
			request.append("&qt="+queryType);
		}
		request.append("&q=" + query);
		request.append("&start=" + start);
		request.append("&rows=" + rows);
		SolrParams solrParams = SolrRequestParsers.parseQueryString(request.toString());

		return server.query(solrParams);
	}

	/**
	 * Execute a search in the specified PublishQueue index using a ModifiableSolrParams
	 * @param PublishQueueServerUrl PublishQueue Server Url
	 * @param start initial value to return
	 * @param rows number of row to return
	 * @param queryType  qt=spellcheck || qt=spellchecker (optional)
	 * @param facet Facet parameter. Values accepted "on" or "off"
	 * @param ident Ident parameter. Values accepted "on" or "off"
	 * @param query PublishQueue query
	 * @param myCollection PublishQueue collection name (optional)
	 * @param username PublishQueue username (optional)
	 * @param password PublishQueue password (optional)
	 * @return QueryResponse
	 * @throws SolrServerException
	 * @throws MalformedURLException
	 */
	public static QueryResponse executeModifiableSolrParamsSearch(String PublishQueueServerUrl, int start, int rows,String queryType, String facet, String ident, String query, String myCollection, String username, String password)
	throws SolrServerException, MalformedURLException {

		CommonsHttpSolrServer server = new CommonsHttpSolrServer(PublishQueueServerUrl);
		server.setParser(new XMLResponseParser());

		ModifiableSolrParams solrParams = new ModifiableSolrParams();
		if(UtilMethods.isSet(myCollection)){
			solrParams.set("collectionName", myCollection);
		}
		if(UtilMethods.isSet(username)){
			solrParams.set("username", username);
		}
		if(UtilMethods.isSet(password)){
			solrParams.set("password", password);
		}
		if(UtilMethods.isSet(ident)){
			solrParams.set("indent", ident);
		}
		if(UtilMethods.isSet(facet)){
			solrParams.set("facet", facet);
		}
		if(UtilMethods.isSet(queryType)){
			// qt=spellcheck || qt=spellchecker
			solrParams.set("qt",queryType);
		}
		solrParams.set("q", query);
		solrParams.set("start", start);
		solrParams.set("rows", rows);
		return server.query(solrParams);
	}


	/**
	 * Execute a search in the specified PublishQueue index using a SolrQuery
	 * @param PublishQueueServerUrl PublishQueue Server Url
	 * @param start initial value to return
	 * @param rows number of row to return
	 * @param queryType  qt=spellcheck || qt=spellchecker (optional)
	 * @param facet Facet parameter. Values accepted "on" or "off"
	 * @param ident Ident parameter. Values accepted "on" or "off"
	 * @param query PublishQueue query
	 * @param myCollection PublishQueue collection name (optional)
	 * @param username PublishQueue username (optional)
	 * @param password PublishQueue password (optional)
	 * @return QueryResponse
	 * @throws SolrServerException
	 * @throws MalformedURLException
	 */
	public static QueryResponse executeSolrQuerySearch(String PublishQueueServerUrl, int start, int rows,String queryType, String facet, String ident, String query, String myCollection, String username, String password)
	throws SolrServerException, MalformedURLException {
		CommonsHttpSolrServer server = new CommonsHttpSolrServer(PublishQueueServerUrl);
		server.setParser(new XMLResponseParser());

		SolrQuery solrQuery = new SolrQuery();
		if(UtilMethods.isSet(myCollection)){
			solrQuery.set("collectionName", myCollection);
		}
		if(UtilMethods.isSet(username)){
			solrQuery.set("username", username);
		}
		if(UtilMethods.isSet(password)){
			solrQuery.set("password", password);
		}
		if(UtilMethods.isSet(ident)){
			solrQuery.set("indent", ident);
		}
		if(UtilMethods.isSet(facet)){
			solrQuery.set("facet", facet);
		}
		if(UtilMethods.isSet(queryType)){
			// qt=spellcheck || qt=spellchecker
			solrQuery.setQueryType(queryType);
		}
		solrQuery.setQuery(query);
		solrQuery.setStart(start);
		solrQuery.setRows(rows);
		return server.query(solrQuery);
	}

	private static final String PGVALIDATETABLESQL="SELECT COUNT(table_name) as exist FROM information_schema.tables WHERE Table_Name = 'solr_queue'";
	private static final String PGCREATESQL="CREATE TABLE solr_queue (id bigserial PRIMARY KEY NOT NULL, solr_operation int8, asset_identifier VARCHAR(36) NOT NULL, language_id  int8 NOT NULL, entered_date TIMESTAMP, last_try TIMESTAMP, num_of_tries int8 NOT NULL DEFAULT 0, in_error bool DEFAULT 'f', last_results TEXT)";
	private static final String MYCREATESQL="CREATE TABLE IF NOT EXISTS solr_queue (id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL, solr_operation bigint, asset_identifier VARCHAR(36) NOT NULL, language_id bigint NOT NULL, entered_date DATETIME, last_try DATETIME, num_of_tries bigint NOT NULL DEFAULT 0, in_error varchar(1) DEFAULT '0', last_results LONGTEXT)";
	private static final String MSVALIDATETABLESQL="SELECT COUNT(*) as exist FROM sysobjects WHERE name = 'solr_queue'";
	private static final String MSCREATESQL="CREATE TABLE solr_queue (id bigint IDENTITY (1, 1)PRIMARY KEY NOT NULL, solr_operation numeric(19,0), asset_identifier VARCHAR(36) NOT NULL, language_id numeric(19,0) NOT NULL, entered_date DATETIME, last_try DATETIME, num_of_tries numeric(19,0) NOT NULL DEFAULT 0, in_error tinyint DEFAULT 0, last_results TEXT)";
	private static final String OCLVALIDATETABLESQL="SELECT COUNT(*) as exist FROM user_tables WHERE table_name='SOLR_QUEUE'";
	private static final String OCLCREATESQL="CREATE TABLE SOLR_QUEUE (id INTEGER NOT NULL, solr_operation number(19,0), asset_identifier VARCHAR(36) NOT NULL, language_id number(19,0) NOT NULL, entered_date DATE, last_try DATE, num_of_tries number(19,0) DEFAULT 0 NOT NULL, in_error number(1,0) DEFAULT 0, last_results NCLOB,PRIMARY KEY (id))";
	private static final String OCLCREATESEQSQL="CREATE SEQUENCE SOLR_QUEUE_SEQ START WITH 1 INCREMENT BY 1"; 
	private static final String OCLCREATETRIGERSQL="CREATE OR REPLACE TRIGGER SOLR_QUEUE_TRIGGER before insert on SOLR_QUEUE for each row begin select SOLR_QUEUE_SEQ.nextval into :new.id from dual; end;";

	/**
	 * Create dotcms PublishQueue assets index table
	 * @return boolean, true if the table was created successfully
	 */
	public static boolean createPublishQueueTable(){
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/
				dc.setSQL(PGVALIDATETABLESQL);
				long existTable = (Long)dc.loadObjectResults().get(0).get("exist");

				if(existTable == 0){
					dc.setSQL(PGCREATESQL);
					dc.loadResult();	
				}
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYCREATESQL);
				dc.loadResult();				
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSVALIDATETABLESQL);
				int existTable = (Integer)dc.loadObjectResults().get(0).get("exist");

				if(existTable == 0){
					dc.setSQL(MSCREATESQL);
					dc.loadResult();
				}
			}else{
				dc.setSQL(OCLVALIDATETABLESQL);
				BigDecimal existTable = (BigDecimal)dc.loadObjectResults().get(0).get("exist");
				if(existTable.longValue() == 0){
					dc.setSQL(OCLCREATESEQSQL);
					dc.loadResult();
					dc.setSQL(OCLCREATESQL);
					dc.loadResult();					
					dc.setSQL(OCLCREATETRIGERSQL);
					dc.loadResult();
				}
			}
			return true;
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			return false;
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}


	private static final String PGDELETESQL="DROP TABLE solr_queue";
	private static final String MYDELETESQL="DROP TABLE solr_queue";
	private static final String MSDELETESQL="DROP TABLE solr_queue";
	private static final String OCLDELETESQL="DROP TABLE SOLR_QUEUE";
	private static final String OCLDELETESEQSQL="DROP SEQUENCE SOLR_QUEUE_SEQ";
	private static final String OCLDELETETRIGGERSQL="DROP TRIGGER SOLR_QUEUE_TRIGGER";

	/**
	 * Delete dotcms PublishQueue assets index table
	 * @return boolean, true if the table was created successfully
	 */
	public static boolean deletePublishQueueTable(){
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PGDELETESQL);
				dc.loadResult();				
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYDELETESQL);
				dc.loadResult();				
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSDELETESQL);
				dc.loadResult();
			}else{
				dc.setSQL(OCLDELETETRIGGERSQL);
				dc.loadResult();
				dc.setSQL(OCLDELETESEQSQL);
				dc.loadResult();
				dc.setSQL(OCLDELETESQL);
				dc.loadResult();							
			}
			return true;
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			return false;
		}finally{
			DbConnectionFactory.closeConnection();
		}
	} 

	/**
	 * Validate if a FieldVariable is present in a FieldVariable List
	 * @param list List<FieldVariable>
	 * @param fieldVariableName Variable Name
	 * @return boolean
	 */
	public static boolean containsFieldVariable(List<FieldVariable> list, String fieldVariableName){
		boolean containsVariable =false;
		if(!UtilMethods.isSet(fieldVariableName)){
			return false;
		}
		for(FieldVariable fv : list){
			if(fv.getKey().equals(fieldVariableName)){
				containsVariable= true;
				break;
			}
		}		
		return containsVariable;
	}

	/**
	 * Validate if a FieldVariable is present in a FieldVariable List
	 * @param list List<FieldVariable>
	 * @param fieldVariableName List<FieldVariable> variable names
	 * @return boolean
	 */
	public static boolean containsFieldVariable(List<FieldVariable> list, List<String> fieldVariableNames){
		boolean containsVariable =false;
		if(!UtilMethods.isSet(fieldVariableNames)){
			return false;
		}
		for(FieldVariable fv : list){
			if(fieldVariableNames.contains(fv.getKey())){
				containsVariable= true;
				break;
			}
		}		
		return containsVariable;
	}

	/**
	 * Validate if a Metadata FieldVariable is present in a FieldVariable List
	 * @param list List<FieldVariable>
	 * @param field String with the metadata field name
	 * @return boolean
	 */
	public static boolean containsFieldVariableIgnoreField(List<FieldVariable> list,String dynamicIgnoreMetadaField, String field){
		boolean containsMetadataField =false;
		if(!UtilMethods.isSet(dynamicIgnoreMetadaField) || !UtilMethods.isSet(field)){
			return false;
		}
		for(FieldVariable fv : list){
			if(fv.getKey().equals(dynamicIgnoreMetadaField)){
				String[] values = fv.getValue().split(",");
				for(String val : values){
					if(field.equals(val.trim())){
						containsMetadataField= true;
						break;
					}
				}
			}
		}		 
		return containsMetadataField;
	}

	/**
	 * Get all the existing parents for a category
	 * @param cat child category
	 * @param user current user
	 * @return Set<Category>
	 * @throws DotPublisherException
	 */
	public static Set<Category> getAllParentsCategories(Category cat, User user) throws DotPublisherException{
		Set<Category> results = new HashSet<Category>();
		List<Category> parents;
		try {
			parents = APILocator.getCategoryAPI().getParents(cat, user, false);
			results.addAll(parents);
			for(Category parent : parents){
				results.addAll(getAllParentsCategories(parent,user));
			}
		} catch (Exception e) {
			throw new DotPublisherException(e.getMessage());
		}

		return results;
	}

	/**
	 * Check if the field contains the field attribute to modify the field name to use in PublishQueue Index
	 * @param list List<FieldVariable>
	 * @param fieldAttribute String name of the field with the new field name to use in PublishQueue index
	 * @param defaultPublishQueueFieldName String field velocity var name
	 * @return String
	 */
	public static String getPublishQueueFieldName(List<FieldVariable> list, String fieldAttribute, String defaultPublishQueueFieldName){
		for(FieldVariable fv : list){
			if(fv.getKey().equals(fieldAttribute)){
				String values = fv.getValue();
				return values;
			}
		}		 
		return defaultPublishQueueFieldName;
	}

	/**
	 * Returns a map with the given file's meta data
	 * @param f Field
	 * @param file
	 * @return
	 */
	public static Map<String, String> getMetaDataMap(Field f, File file)  {
		Map<String, String> metaMap = new HashMap<String, String>();
		Parser parser = getParser(file);
		Metadata met = new Metadata();
		//set -1 for no limit when parsing text content
		ContentHandler handler =  new BodyContentHandler(-1);
		ParseContext context = new ParseContext();
		InputStream fis = null;
		try {
			fis = new FileInputStream(file);
			parser.parse(fis,handler,met,context);
			metaMap = new HashMap<String, String>();

			Set<String> allowedFields=null;
			List<FieldVariable> fieldVariables=APILocator.getFieldAPI().getFieldVariablesForField(f.getInode(), APILocator.getUserAPI().getSystemUser(), false);
			for(FieldVariable fv : fieldVariables) {
				if(fv.getKey().equals("dotIndexPattern")) {
					String[] names=fv.getValue().split(",");
					allowedFields=new HashSet<String>();
					for(String n : names){
						allowedFields.add(n.trim());
					}
				}
			}

			for(int i = 0; i <met.names().length; i++) {
				String name = met.names()[i];
				if(UtilMethods.isSet(name) && met.get(name)!=null){
					// we will want to normalize our metadata for searching
					String[]x  = translateKey(name);
					for(String y : x){
						if(!UtilMethods.isSet(allowedFields) || allowedFields.contains(y))
							metaMap.put(y, met.get(name));	
					}
				}
			}
			if(handler!=null && UtilMethods.isSet(handler.toString())){
				metaMap.put(FileAssetAPI.CONTENT_FIELD, handler.toString());	
			}
		} catch (Exception e) {
			Logger.error(PublisherUtil.class, "Could not parse file metadata for file : "+ file.getAbsolutePath()); 
		} finally{
			metaMap.put(FileAssetAPI.SIZE_FIELD,  String.valueOf(file.length()));
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {}
			}
		}

		return metaMap;
	}
	
	/**
	 * Returns an object represent the single row of publishing_end_point table.
	 * We descrypt the auth_key in this case.
	 * 
	 * Oct 30, 2012 - 11:21:23 AM
	 */
	public static PublishingEndPoint getObjectByMap(Map<String, Object> row){
		PublishingEndPoint pep = new PublishingEndPoint();
		pep.setId(row.get("id").toString());
		pep.setGroupId(row.get("group_id").toString());
		pep.setAddress(row.get("address").toString());
		pep.setPort(row.get("port").toString());
		pep.setProtocol(row.get("protocol").toString());		
		pep.setServerName(new StringBuilder(row.get("server_name").toString()));
		pep.setAuthKey(new StringBuilder(row.get("auth_key").toString()));
		pep.setEnabled(Integer.parseInt(row.get("enabled").toString())==1);
		pep.setSending(Integer.parseInt(row.get("sending").toString())==1);
		return pep;
	}
	
	/**
	 * normalize metadata from various filetypes
	 * this method will return an array of metadata keys
	 * that we can use to normalize the values in our fileAsset metadata
	 * For example, tiff:ImageLength = "height" for image files, so 
	 * we return {"tiff:ImageLength", "height"} and both metadata
	 * are written to our metadata field
	 * @param key
	 * @return
	 */
	private static String[] translateKey(String key){
		String[] x= getTranslationMap().get(key);
		if(x ==null){
			x = new String[]{StringUtils.sanitizeCamelCase(key)};
		}
		return x;
	}


	private static Map<String, String[]> translateMeta = null;

	/**
	 * 
	 * @param binFile
	 * @return
	 */
	private static Parser getParser(File binFile) {
		String mimeType =  new MimetypesFileTypeMap().getContentType(binFile);
		String[] mimeTypes = Config.getStringArrayProperty("CONTENT_PARSERS_MIMETYPES");
		String[] parsers = Config.getStringArrayProperty("CONTENT_PARSERS");
		int index = Arrays.binarySearch(mimeTypes, mimeType);
		if(index>-1 && parsers.length>0){
			String parserClassName = parsers[index];
			Class<Parser> parserClass;
			try {
				parserClass = (Class<Parser>)Class.forName(parserClassName);
				return parserClass.newInstance();
			} catch(Exception e){
				Logger.warn(PublisherUtil.class, "A content parser for mime type " + mimeType + " was found but could not be instantiated, using default content parser."); 
			}
		}
		return  new AutoDetectParser();
	}	

	private static Map<String, String[]> getTranslationMap(){
		if(translateMeta ==null){
			synchronized ("translateMeta".intern()) {
				if(translateMeta ==null){
					translateMeta=	new HashMap<String, String[]>();
					translateMeta.put("tiff:ImageWidth"		, new String[]{"tiff:ImageWidth","width"});
					translateMeta.put("tiff:ImageLength"	, new String[]{"tiff:ImageLength","height"});
				}
			}
		}
		return translateMeta;
	}	
}