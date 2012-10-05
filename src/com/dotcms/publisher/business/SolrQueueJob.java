package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrInputDocument;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.publisher.util.SolrUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class read the publishing_queue table and add/update or delete elements in the solr index
 * @author Oswaldo
 *
 */
public class SolrQueueJob implements StatefulJob {

	private String pluginId = "com.dotcms.solr";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private UserAPI userAPI = APILocator.getUserAPI();
	private HostAPI hostAPI = APILocator.getHostAPI();
	private SolrAPI solrAPI = SolrAPI.getInstance();
	private FileAPI fileAPI = APILocator.getFileAPI();
	private IdentifierAPI identifierAPI = APILocator.getIdentifierAPI() ;

	public void execute(JobExecutionContext arg0) throws JobExecutionException {

		int serversNumber = 0;
		int documentsPerRequest = 1;
		try {
			Logger.info(SolrQueueJob.class, "Running Solr Queue Job");

			User user = userAPI.getSystemUser();
			serversNumber = Integer.parseInt(pluginAPI.loadProperty(pluginId, "com.dotcms.solr.SOLR_SERVER_NUMBER"));

			/*Number of documents to send in one request*/
			documentsPerRequest = Integer.parseInt(pluginAPI.loadProperty(pluginId, "com.dotcms.solr.DOCUMENTS_PER_REQUEST"));
			if(documentsPerRequest < 1){
				documentsPerRequest = 1;
			}

			/*Get attribute to modify field varname */
			String solrField = pluginAPI.loadProperty(pluginId, "com.dotcms.solr.MODIFY_FIELD_NAME_ATTRIBUTE");

			/*Get the name of the attribute that indicates if a field shouldn't be include in the solr index*/
			String ignoreField = pluginAPI.loadProperty(pluginId, "com.dotcms.solr.IGNORE_FIELDS_WITH_ATTRIBUTE");

			String ignoreHostVariableForFileAssetsString = pluginAPI.loadProperty(pluginId, "com.dotcms.solr.IGNORE_HOST_SUBSTITUTE");
			boolean isHostSubstituteRequired = "false".equalsIgnoreCase(ignoreHostVariableForFileAssetsString);

			/*Metadata field to ignore*/
			String dynamicIgnoreMetadaField = pluginAPI.loadProperty(pluginId, "com.dotcms.solr.IGNORE_METADATA_FIELD_ATTRIBUTES");

			String ignoreMetadataFieldsString = pluginAPI.loadProperty(pluginId, "com.dotcms.solr.IGNORE_METADATA_FIELDS");
			List<String> ignoreMetadataFields = new ArrayList<String>();
			if(UtilMethods.isSet(ignoreMetadataFieldsString)){
				for(String attr : ignoreMetadataFieldsString.split(",")){
					ignoreMetadataFields.add(attr.trim());
				}
			}

			/*the job is executed only if there are solr servers and entries in the publishing_queue table to process */
			if(serversNumber > 0){

				List<Map<String,Object>> solrQueue = solrAPI.getSolrQueueContentletToProcess();
				Logger.info(SolrQueueJob.class, "Solr Queue element(s) to process: "+solrQueue.size());

				if(solrQueue.size() > 0){
					Collection<SolrInputDocument> addDocs = new ArrayList<SolrInputDocument>(); 
					List<Map<String,Object>> solrIdAddDocs = new ArrayList<Map<String,Object>>(); 
					List<String> deleteDocs = new ArrayList<String>(); 
					List<Map<String,Object>> solrIdDeleteDocs = new ArrayList<Map<String,Object>>(); 

					for(Map<String,Object> solr : solrQueue){
						try {							
							if(Long.parseLong(solr.get("solr_operation").toString()) == SolrAPI.ADD_OR_UPDATE_SOLR_ELEMENT){
								SolrInputDocument doc = new SolrInputDocument();
								String identifier =(String)solr.get("asset_identifier");
								long languageId =Long.parseLong(solr.get("language_id").toString());
								Contentlet con = null;
								Host host=null;

								try {
									con = conAPI.findContentletByIdentifier(identifier, true, languageId, user, false);																	
								}catch(Exception e){
									Logger.debug(SolrQueueJob.class,e.getMessage(),e);
								}
								if(UtilMethods.isSet(con)){
									doc.addField("id", con.getIdentifier());
									doc.addField("inode", con.getInode());
									doc.addField("modUser", con.getModUser());
									String modDate = UtilMethods.dateToHTMLDate(con.getModDate(), "yyyy-MM-dd")+"T"+ UtilMethods.dateToHTMLDate(con.getModDate(), "HH:mm:ss.S")+"Z";
									doc.addField("modDate",modDate);
									doc.addField("host", con.getHost());
									doc.addField("folder", con.getFolder());									
									Structure st = con.getStructure();
									doc.addField("structureName", st.getName());
									doc.addField("structureInode", st.getInode());
									doc.addField("structureId", st.getVelocityVarName());
									doc.addField("structureType", st.getStructureType());

									if(UtilMethods.isSet(st.getDescription())){
										doc.addField("structureDescription", st.getDescription());
									} else {
										doc.addField("structureDescription", "");
									}

									for(Field f : st.getFieldsBySortOrder()){

										List<FieldVariable> fieldVariables = APILocator.getFieldAPI().getFieldVariablesForField(f.getInode(), user, false);
										if(!SolrUtil.containsFieldVariable(fieldVariables, ignoreField)){
											Object value = conAPI.getFieldValue(con, f);											
											String solrFieldName = SolrUtil.getSolrFieldName(fieldVariables, solrField, f.getVelocityVarName());

											if(f.getFieldType().equals(Field.FieldType.DATE.toString()) || f.getFieldType().equals(Field.FieldType.DATE_TIME.toString())){
												String date = "";
												if(UtilMethods.isSet(value)){
													date = UtilMethods.dateToHTMLDate((Date)value, "yyyy-MM-dd")+"T"+ UtilMethods.dateToHTMLDate((Date)value, "HH:mm:ss.S")+"Z";
												}
												doc.addField(solrFieldName, date);
											}else if(f.getFieldType().equals(Field.FieldType.FILE.toString()) || f.getFieldType().equals(Field.FieldType.IMAGE.toString())){
												String path = "";
												if(UtilMethods.isSet(value)){
													Identifier assetIdentifier = identifierAPI.find((String)value);													
													host = hostAPI.find(assetIdentifier.getHostId(), user, false);
													String hostName ="";
													if (isHostSubstituteRequired) {
														if(UtilMethods.isSet(host)){
															hostName="http://"+host.getHostname();
														}else{
															host = hostAPI.findDefaultHost(user, false);
															hostName="http://"+host.getHostname();
														}
													}
													path = hostName+assetIdentifier.getParentPath()+assetIdentifier.getAssetName();

													/**
													 * Add to solr index file or image field metadata
													 */
													if(con.getStructure().getStructureType()!=Structure.STRUCTURE_TYPE_FILEASSET){
														Contentlet fileCon = conAPI.findContentletByIdentifier(assetIdentifier.getInode(), true, languageId, user, false);
														IFileAsset fileA = APILocator.getFileAssetAPI().fromContentlet(fileCon);
														java.io.File file = fileA.getFileAsset();
														Map<String, String> keyValueMap = SolrUtil.getMetaDataMap(f, file);
														for(String key : keyValueMap.keySet()){
															if(!ignoreMetadataFields.contains(key) && !SolrUtil.containsFieldVariableIgnoreField(fieldVariables,dynamicIgnoreMetadaField, key)){
																doc.addField(solrFieldName+"_"+key, keyValueMap.get(key));
															}
														}
													}
												}
												doc.addField(solrFieldName, path );

											}else if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){
												String path="";
												if(UtilMethods.isSet(value)){
													java.io.File fileValue = (java.io.File)value;
													String fileName = fileValue.getName();													
													host = hostAPI.find(con.getHost(), user, false);
													String hostName ="";
													if (isHostSubstituteRequired) {
														if(UtilMethods.isSet(host)){
															hostName="http://"+host.getHostname();
														}else{
															host = hostAPI.findDefaultHost(user, false);
															hostName="http://"+host.getHostname();
														}	
													}
													if(UtilMethods.isImage(fileName)){
														path=hostName+"/contentAsset/image/"+con.getInode()+"/"+f.getVelocityVarName()+"/?byInode=true";
													}else{
														path=hostName+"/contentAsset/raw-data/"+ con.getInode() + "/" + f.getVelocityVarName() + "?byInode=true";
													}

													/**
													 * Add to solr index binary field metadata
													 */
													if(con.getStructure().getStructureType()!=Structure.STRUCTURE_TYPE_FILEASSET){
														Map<String, String> keyValueMap = SolrUtil.getMetaDataMap(f, fileValue);
														for(String key : keyValueMap.keySet()){
															if(!ignoreMetadataFields.contains(key) && !SolrUtil.containsFieldVariableIgnoreField(fieldVariables,dynamicIgnoreMetadaField, key)){
																doc.addField(solrFieldName+"_"+key, keyValueMap.get(key));
															}
														}
													}
												}
												doc.addField(solrFieldName, path);
											}else if(f.getFieldType().equals(Field.FieldType.TAG.toString())||
													f.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) ||
													f.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) ||
													f.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString())){												
												String valueString = (String)value;
												if(UtilMethods.isSet(valueString)){
													doc.addField(solrFieldName, valueString.split(","));
												}else{
													doc.addField(solrFieldName,valueString);
												}
											}else if(f.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())){
												Map<String, Object> keyValueMap = null;
												String JSONValue = UtilMethods.isSet(value)? (String)value:"";
												//Convert JSON to Table Display {key, value, order}
												if(UtilMethods.isSet(JSONValue)){
													keyValueMap =  com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(JSONValue);
												}
												for(String key : keyValueMap.keySet()){
													if(!ignoreMetadataFields.contains(key) && !SolrUtil.containsFieldVariableIgnoreField(fieldVariables,dynamicIgnoreMetadaField, key)){
														doc.addField(solrFieldName+"_"+key, keyValueMap.get(key));
													}
												}
											}else if(f.getFieldType().equals(Field.FieldType.CATEGORY.toString())){
												@SuppressWarnings("unchecked")
												HashSet<Category> categorySet = (HashSet<Category>) value;
												Set<String> categoryList = new HashSet<String>();

												for(Category cat : categorySet){
													if(UtilMethods.isSet(cat.getKey())){
														categoryList.add(cat.getKey());
													}else{
														categoryList.add(cat.getCategoryVelocityVarName());
													}
													Set<Category> parents = SolrUtil.getAllParentsCategories(cat, user);
													for(Category parentCat : parents){
														if(UtilMethods.isSet(parentCat.getKey())){
															categoryList.add(parentCat.getKey());
														}else{
															categoryList.add(parentCat.getCategoryVelocityVarName());
														}
													}
												}

												for(String categoryVar : categoryList){
													doc.addField(solrFieldName, categoryVar);
												}
											}else{
												doc.addField(solrFieldName, value);
											}
										}
									}
									/*variables  to send docs in groups per request in solr*/
									addDocs.add(doc);
									solrIdAddDocs.add(solr);

									if(addDocs.size() == documentsPerRequest){
										/* Add or update index element*/
										Logger.debug(SolrQueueJob.class,"Sending Add/Update Document(s) group request to Solr");
										try {
											Logger.debug(SolrQueueJob.class,"Document(s) to Add/Update: "+addDocs.size());
											for(int server=0; server < serversNumber; server++ ){
												String solrServerUrl = pluginAPI.loadProperty(pluginId, "com.dotcms.solr."+server+".SOLR_SERVER");
												SolrUtil.addToSolrIndex(solrServerUrl, addDocs);
											}
											int addCounter = 0;
											for(Map<String,Object> solrO : solrIdAddDocs){
												solrAPI.deleteElementFromSolrQueueTable(Long.parseLong(solrO.get("id").toString()));//delete from table	
												addCounter++;
											}
											addDocs.clear();
											solrIdAddDocs.clear();
											Logger.debug(SolrQueueJob.class,"Document(s) Added/Updated: "+addCounter);
										}catch(Exception e){
											Logger.debug(SolrQueueJob.class,e.getMessage(),e);
											int addCounter = 0;
											Logger.debug(SolrQueueJob.class,"Document(s) not Added/Updated: "+addDocs.size());
											for(Map<String,Object> solrO : solrIdAddDocs){
												solrAPI.updateElementStatusFromSolrQueueTable(Long.parseLong(solrO.get("id").toString()),new Date(),(Integer.parseInt(solrO.get("num_of_tries").toString())+1), true, "An error occurs trying to add/update this assets in the Solr Index. ERROR: "+e);
												addCounter++;
											}
											addDocs.clear();
											solrIdAddDocs.clear();
										}
									}
								}else{
									solrAPI.updateElementStatusFromSolrQueueTable(Long.parseLong(solr.get("id").toString()),new Date(),(Integer.parseInt(solr.get("num_of_tries").toString())+1), true, "This file asset content:"+(String)solr.get("asset_identifier")+" doesn't exist");
								}
							} else if(Long.parseLong(solr.get("solr_operation").toString()) == SolrAPI.DELETE_SOLR_ELEMENT){
								/* delete element from index*/
								String id = (String)solr.get("asset_identifier");

								/*variables  to send docs in groups per request in solr*/
								deleteDocs.add(id);
								solrIdDeleteDocs.add(solr);

								if(deleteDocs.size() == documentsPerRequest){
									Logger.debug(SolrQueueJob.class,"Sending Delete Document(s) group request to Solr");
									try {
										Logger.debug(SolrQueueJob.class,"Document(s) to Delete: "+deleteDocs.size());
										for(int server=0; server < serversNumber; server++ ){
											String solrServerUrl = pluginAPI.loadProperty(pluginId, "com.dotcms.solr."+server+".SOLR_SERVER");
											SolrUtil.deleteFromSolrIndexById(solrServerUrl, deleteDocs);
										}
										int deleteCounter = 0;
										for(Map<String,Object> solrO : solrIdDeleteDocs){
											solrAPI.deleteElementFromSolrQueueTable(Long.parseLong(solrO.get("id").toString()));//delete from table
											deleteCounter++;
										}
										deleteDocs.clear();
										solrIdDeleteDocs.clear();
										Logger.debug(SolrQueueJob.class,"Document(s) Deleted: "+deleteCounter);										
									}catch(Exception e){
										Logger.debug(SolrQueueJob.class,e.getMessage(),e);
										int deleteCounter = 0;
										Logger.debug(SolrQueueJob.class,"Document(s) not Deleted: "+deleteDocs.size());
										for(Map<String,Object> solrO : solrIdDeleteDocs){
											solrAPI.updateElementStatusFromSolrQueueTable(Long.parseLong(solrO.get("id").toString()),new Date(),(Integer.parseInt(solrO.get("num_of_tries").toString())+1), true, "An error occurs trying to delete this assets in the Solr Index. ERROR: "+e);
											deleteCounter++;
										}
										deleteDocs.clear();
										solrIdDeleteDocs.clear();
									}
								}
							}							
						}catch(Exception b){
							Logger.debug(SolrQueueJob.class,b.getMessage(),b);
							solrAPI.updateElementStatusFromSolrQueueTable(Long.parseLong(solr.get("id").toString()),new Date(),(Integer.parseInt(solr.get("num_of_tries").toString())+1), true, "An error occurs trying to process this assets in the Solr Index. ERROR: "+b);
						}						
					}	

					if(addDocs.size() > 0 ){
						/* Add or update index element*/
						Logger.debug(SolrQueueJob.class,"Sending Add/Update Document(s) group request to Solr");
						try {
							Logger.debug(SolrQueueJob.class,"Document(s) to Add/Update: "+addDocs.size());
							for(int server=0; server < serversNumber; server++ ){
								String solrServerUrl = pluginAPI.loadProperty(pluginId, "com.dotcms.solr."+server+".SOLR_SERVER");
								SolrUtil.addToSolrIndex(solrServerUrl, addDocs);
							}
							int addCounter = 0;
							for(Map<String,Object> solrO : solrIdAddDocs){
								solrAPI.deleteElementFromSolrQueueTable(Long.parseLong(solrO.get("id").toString()));//delete from table	
								addCounter++;
							}
							addDocs.clear();
							solrIdAddDocs.clear();
							Logger.debug(SolrQueueJob.class,"Document(s) Added/Updated: "+addCounter);
						}catch(Exception e){
							Logger.debug(SolrQueueJob.class,e.getMessage(),e);
							int addCounter = 0;
							Logger.debug(SolrQueueJob.class,"Document(s) not Added/Updated: "+addDocs.size());
							for(Map<String,Object> solrO : solrIdAddDocs){
								solrAPI.updateElementStatusFromSolrQueueTable(Long.parseLong(solrO.get("id").toString()),new Date(),(Integer.parseInt(solrO.get("num_of_tries").toString())+1), true, "An error occurs trying to add/update this assets in the Solr Index. ERROR: "+e);
								addCounter++;
							}
							addDocs.clear();
							solrIdAddDocs.clear();
						}
					}
					if(deleteDocs.size() > 0){
						Logger.debug(SolrQueueJob.class,"Sending Delete Document(s) group request to Solr");
						try {
							Logger.debug(SolrQueueJob.class,"Document(s) to Delete: "+deleteDocs.size());
							for(int server=0; server < serversNumber; server++ ){
								String solrServerUrl = pluginAPI.loadProperty(pluginId, "com.dotcms.solr."+server+".SOLR_SERVER");
								SolrUtil.deleteFromSolrIndexById(solrServerUrl, deleteDocs);
							}
							int deleteCounter = 0;
							for(Map<String,Object> solrO : solrIdDeleteDocs){
								solrAPI.deleteElementFromSolrQueueTable(Long.parseLong(solrO.get("id").toString()));//delete from table
								deleteCounter++;
							}
							deleteDocs.clear();
							solrIdDeleteDocs.clear();
							Logger.debug(SolrQueueJob.class,"Document(s) Deleted: "+deleteCounter);										
						}catch(Exception e){
							Logger.debug(SolrQueueJob.class,e.getMessage(),e);
							int deleteCounter = 0;
							Logger.debug(SolrQueueJob.class,"Document(s) not Deleted: "+deleteDocs.size());
							for(Map<String,Object> solrO : solrIdDeleteDocs){
								solrAPI.updateElementStatusFromSolrQueueTable(Long.parseLong(solrO.get("id").toString()),new Date(),(Integer.parseInt(solrO.get("num_of_tries").toString())+1), true, "An error occurs trying to delete this assets in the Solr Index. ERROR: "+e);
								deleteCounter++;
							}
							deleteDocs.clear();
							solrIdDeleteDocs.clear();
						}
					}

				}
			}
			Logger.info(SolrQueueJob.class, "Finished Solr Queue Job");
		} catch (NumberFormatException e) {
			Logger.error(SolrQueueJob.class,e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(SolrQueueJob.class,e.getMessage(),e);
		} catch (DotSolrException e) {
			Logger.error(SolrQueueJob.class,e.getMessage(),e);
		} catch (Exception e) {
			Logger.error(SolrQueueJob.class,e.getMessage(),e);
		}

	}
}
