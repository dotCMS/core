package com.dotcms.content.elasticsearch.business;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;

import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.KeyValueFieldUtil;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.NumberUtil;
import com.dotmarketing.util.ThreadSafeSimpleDateFormat;
import com.dotmarketing.util.UtilMethods;

public class ESMappingAPIImpl implements ContentMappingAPI {

	static ObjectMapper mapper = null;

	public ESMappingAPIImpl() {
		if (mapper == null) {
			synchronized (this.getClass().getName()) {
				if (mapper == null) {
					mapper = new ObjectMapper();
					ThreadSafeSimpleDateFormat df = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					mapper.setDateFormat(df);
				}
			}
		}
	}
	
	/**
	 * This method takes a mapping string, a type and puts it as the mapping
	 * @param index
	 * @param type
	 * @param mapping
	 * @return
	 * @throws ElasticSearchException
	 * @throws IOException
	 */
    public  boolean putMapping(String indexName, String type, String mapping) throws ElasticSearchException, IOException{
    	
    	ListenableActionFuture<PutMappingResponse> lis = new ESClient().getClient().admin().indices().preparePutMapping().setIndices(indexName).setType(type).setSource(mapping).execute();
    	return lis.actionGet().acknowledged();
    }
    
	/**
	 * This method takes a mapping string, a type and puts it as the mapping
	 * @param index
	 * @param type
	 * @param mapping
	 * @return
	 * @throws ElasticSearchException
	 * @throws IOException
	 */
    public  boolean putMapping(String indexName, String type, String mapping, String settings) throws ElasticSearchException, IOException{
    	ListenableActionFuture<PutMappingResponse> lis = new ESClient().getClient().admin().indices().preparePutMapping().setIndices(indexName).setType(type).setSource(mapping).execute();
    	return lis.actionGet().acknowledged();
    }
    
    public  boolean setSettings(String indexName,   String settings) throws ElasticSearchException, IOException{
    	new ESClient().getClient().admin().indices().prepareUpdateSettings().setSettings(settings).setIndices( indexName).execute().actionGet();
    	return true;
    }
    
    
    
    /**
     * Gets the mapping params for an index and type
     * @param index
     * @param type
     * @return
     * @throws ElasticSearchException
     * @throws IOException
     */
    public  String getMapping(String index, String type) throws ElasticSearchException, IOException{
    	
    	return new ESClient().getClient().admin().cluster().state(new ClusterStateRequest())
        .actionGet().state().metaData().indices()
        .get(index).mapping(type).source().string();
    	
    }
    


	private Map<String, Object> getFieldJson(Field f) throws DotMappingException {
		Map<String, Object> fieldProps = getDefaultFieldMap();
		fieldProps.put("type", getElasticType(f));
		return fieldProps;

	}

	private Map<String, Object> getDefaultFieldMap() {

		Map<String, Object> fieldProps = new HashMap<String, Object>();
		fieldProps.put("store", "no");
		fieldProps.put("include_in_all", false);
		return fieldProps;

	}

	private Map<String, Object> getDefaultContentletFields() {
		Map<String, Object> m = new HashMap<String, Object>();
		Map<String, Object> fields = new HashMap<String, Object>();
		// required fields
		m = getDefaultFieldMap();
		m.put("type", "string");
		fields.put("identifier", m);

		m = getDefaultFieldMap();
		m.put("type", "string");
		fields.put("inode", m);

		m = getDefaultFieldMap();
		m.put("type", "string");
		fields.put("modUser", m);

		m = getDefaultFieldMap();
		m.put("type", "date");
		fields.put("modDate", m);

		m = getDefaultFieldMap();
		m.put("type", "string");
		fields.put("host", m);

		m = getDefaultFieldMap();
		m.put("type", "string");
		fields.put("stInode", m);

		m = getDefaultFieldMap();
		m.put("type", "string");
		fields.put("folder", m);

		m = getDefaultFieldMap();
		m.put("type", "integer");
		fields.put("languageId", m);

		m = getDefaultFieldMap();
		m.put("type", "string");
		fields.put("owner", m);

		m = getDefaultFieldMap();
		m.put("type", "date");
		fields.put("lastReview", m);

		m = getDefaultFieldMap();
		m.put("type", "date");
		fields.put("nextReview", m);

		m = getDefaultFieldMap();
		m.put("type", "title");
		fields.put("title", m);

		return fields;

	}

	private String getElasticType(Field f) throws DotMappingException {
		if (f.getFieldType().equals(Field.FieldType.TAG.toString())) {
			return "tag";
		}
		if (f.getFieldContentlet().contains("integer")) {
			return "integer";
		} else if (f.getFieldContentlet().contains("date")) {
			return "date";
		} else if (f.getFieldContentlet().contains("bool")) {
			return "boolean";
		} else if (f.getFieldContentlet().contains("float")) {
			return "float";
		}
		return "string";
		// throw new
		// DotMappingException("unable to find mapping for indexed field " + f);

	}

	@SuppressWarnings("unchecked")
	public String toJson(Contentlet con) throws DotMappingException {

		try {
			Map<String,String> m = new HashMap<String,String>();

			loadCategories(con, m);
			loadFields(con, m);
			loadPermissions(con, m);
			loadRelationshipFields(con, m);

			Identifier ident = APILocator.getIdentifierAPI().find(con);
			ContentletVersionInfo cvi = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), con.getLanguageId());
			Structure st=StructureCache.getStructureByInode(con.getStructureInode());

			m.put("title", con.getTitle());
			m.put("structureName", st.getVelocityVarName());
            m.put("structureType", st.getStructureType() + "");
            m.put("inode", con.getInode());
            m.put("type", "content");
            m.put("modDate", datetimeFormat.format(con.getModDate()));
            m.put("owner", con.getOwner()==null ? "0" : con.getOwner());
            m.put("modUser", con.getModUser());
            m.put("live", Boolean.toString(con.isLive()));
            m.put("working", Boolean.toString(con.isWorking()));
            m.put("locked", Boolean.toString(con.isLocked()));
            m.put("deleted", Boolean.toString(con.isArchived()));
            m.put("languageId", Long.toString(con.getLanguageId()));
            m.put("identifier", ident.getId());
            m.put("conHost", ident.getHostId());
            m.put("conFolder", con.getFolder());
            m.put("parentPath", ident.getParentPath());
            m.put("path", ident.getPath());
            
            m.put("versionTs", datetimeFormat.format(cvi.getVersionTs()));
            String urlMap = null;
            try{
            	urlMap = APILocator.getContentletAPI().getUrlMapForContentlet(con, APILocator.getUserAPI().getSystemUser(), true);
                if(urlMap != null){
                	m.put("urlMap",urlMap );	
                }
            }
            catch(Exception e){
            	Logger.warn(this.getClass(), "Cannot get URLMap for contentlet.id : " + ((ident != null) ? ident.getId() : con) + " , reason: "+e.getMessage());
            	throw new DotRuntimeException(urlMap, e);
            }


            Map<String,Object> mlowered=new HashMap<String,Object>();
            for(Entry<String,String> entry : m.entrySet()){
                mlowered.put(entry.getKey().toLowerCase(), entry.getValue().toLowerCase());
                mlowered.put(entry.getKey().toLowerCase() + "_dotraw", entry.getValue().toLowerCase());
            }
            //mlowered.put("versionts", cvi.getVersionTs());
			String x = mapper.writeValueAsString(mlowered);
			return x;
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotMappingException(e.getMessage(), e);
		}
	}



	public Object toMappedObj(Contentlet con) throws DotMappingException {
		return toJson(con);
	}

	@SuppressWarnings("unchecked")
	protected void loadCategories(Contentlet con, Map<String,String> m) throws DotDataException, DotSecurityException {
	    // first we check if there is a category field in the structure. We don't hit db if not needed
	    boolean thereiscategory=false;
	    Structure st=StructureCache.getStructureByInode(con.getStructureInode());
	    List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());
	    for(Field f : fields)
	        thereiscategory = thereiscategory ||
	         f.getFieldType().equals(FieldType.CATEGORY.toString());

	    String categoriesString="";

	    if(thereiscategory) {
    	    String categoriesSQL = "select category.category_velocity_var_name as cat_velocity_var "+
                    " from  category join tree on (tree.parent = category.inode) join contentlet c on (c.inode = tree.child) " +
                    " where c.inode = ?";
    	    DotConnect db = new DotConnect();
            db.setSQL(categoriesSQL);
            db.addParam(con.getInode());
            ArrayList<String> categories=new ArrayList<String>();
    	    List<HashMap<String, String>> categoriesResults = db.loadResults();
    	    for (HashMap<String, String> crow : categoriesResults)
    	        categories.add(crow.get("cat_velocity_var"));

    	    categoriesString=UtilMethods.join(categories, " ").trim();

	        for(Field f : fields) {
	            if(f.getFieldType().equals(FieldType.CATEGORY.toString())) {
    	            String catString="";
    	            if(!categories.isEmpty()) {
        	            String catId=f.getValues();

        	            // we get all subcategories (recursive)
        	            Category category=APILocator.getCategoryAPI().find(catId, APILocator.getUserAPI().getSystemUser(), false);
        	            List<Category> childrens=APILocator.getCategoryAPI().getAllChildren(
        	                    category, APILocator.getUserAPI().getSystemUser(), false);

        	            // we look for categories that match childrens for the
        	            // categoryId of the field
        	            ArrayList<String> fieldCategories=new ArrayList<String>();
        	            for(String catvelvarname : categories)
        	                for(Category chCat : childrens)
        	                    if(chCat.getCategoryVelocityVarName().equals(catvelvarname))
        	                        fieldCategories.add(catvelvarname);

        	            // after matching them we create the JSON field
        	            if(!fieldCategories.isEmpty())
            	            catString=UtilMethods.join(fieldCategories, " ").trim();
    	            }
    	            m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), catString);
	            }
	        }
	    }

        m.put("categories", categoriesString);
	}

	@SuppressWarnings("unchecked")
	protected void loadPermissions(Contentlet con, Map<String,String> m) throws DotDataException {
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        List<Permission> permissions = permissionAPI.getPermissions(con, false, false, true);
        StringBuilder permissionsSt = new StringBuilder();
        boolean ownerCanRead = false;
        boolean ownerCanWrite = false;
        boolean ownerCanPub = false;
        for (Permission permission : permissions) {
            String str = "P" + permission.getRoleId() + "." + permission.getPermission() + "P ";
            if (permissionsSt.toString().indexOf(str) < 0) {
                permissionsSt.append(str);
            }
            if(APILocator.getRoleAPI().loadCMSOwnerRole().getId().equals(String.valueOf(permission.getRoleId()))){
                if(permission.getPermission() == PERMISSION_READ){
                    ownerCanRead = true;
                }else if(permission.getPermission() == PERMISSION_WRITE){
                    ownerCanRead = true;
                    ownerCanWrite = true;
                }else if(permission.getPermission() == PERMISSION_PUBLISH){
                    ownerCanRead = true;
                    ownerCanWrite = true;
                    ownerCanPub = true;
                }
            }
        }
        m.put("permissions", permissionsSt.toString());
        m.put("ownerCanRead", Boolean.toString(ownerCanRead));
        m.put("ownerCanWrite", Boolean.toString(ownerCanWrite));
        m.put("ownerCanPublish", Boolean.toString(ownerCanPub));
	}

	public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");
	public static final FastDateFormat datetimeFormat = FastDateFormat.getInstance("yyyyMMddHHmmss");
	
	public static final String elasticSearchDateTimeFormatPattern="yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final FastDateFormat elasticSearchDateTimeFormat = FastDateFormat.getInstance(elasticSearchDateTimeFormatPattern);
	
	
	public static final FastDateFormat timeFormat = FastDateFormat.getInstance("HHmmss");
	public static final DecimalFormat numFormatter = new DecimalFormat("0000000000000000000.000000000000000000");
	
	@SuppressWarnings("unchecked")
	protected void loadFields(Contentlet con, Map<String, String> m) throws DotDataException {
	    FieldAPI fAPI=APILocator.getFieldAPI();
	    List<Field> fields = FieldsCache.getFieldsByStructureInode(con.getStructureInode());
	    
	    Structure st=con.getStructure();
        for (Field f : fields) {
            if (f.getFieldType().equals(Field.FieldType.BINARY.toString())
                    || f.getFieldContentlet() != null && f.getFieldContentlet().startsWith("system_field")) {
                continue;
            }
            if(!f.isIndexed()){
            	continue;
            }
            try {
                if(fAPI.isElementConstant(f)){
                    m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), (f.getValues() == null ? "":f.getValues().toString()));
                    continue;
                }

                Object valueObj = con.get(f.getVelocityVarName());
                if(valueObj == null){
                    valueObj = "";
                }
                if (f.getFieldContentlet().startsWith("section_divider")) {
                    valueObj = "";
                }

                if(!UtilMethods.isSet(valueObj)) {
                    m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), "");
                }
                else if(f.getFieldType().equals("time")) {
                    String timeStr=timeFormat.format(valueObj);
                    m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), timeStr);
                }
                else if (f.getFieldType().equals("date")) {
                    try {
                        String dateString = dateFormat.format(valueObj);
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), dateString);
                    }
                    catch(Exception ex) {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(),"");
                    }
                } else if(f.getFieldType().equals("date_time")) {
                    try {
                        String datetimeString = datetimeFormat.format(valueObj);
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), datetimeString);
                    }
                    catch(Exception ex) {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(),"");
                    }
                } else if (f.getFieldType().equals("category")) {
                    // moved the logic to loadCategories
                } else if (f.getFieldType().equals("checkbox") || f.getFieldType().equals("multi_select")) {
                    if (f.getFieldContentlet().startsWith("bool")) {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), valueObj.toString());
                    } else {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), UtilMethods.listToString(valueObj.toString()));
                    }
                } else if (f.getFieldType().equals("key_value")){
                    boolean fileMetadata=f.getVelocityVarName().equals(FileAssetAPI.META_DATA_FIELD) && st.getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET;
                	if(!fileMetadata || LicenseUtil.getLevel()>199) {
                		
                	    m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), (String)valueObj);
                        Map<String,Object> keyValueMap = KeyValueFieldUtil.JSONValueToHashMap((String)valueObj);
                	    
                        Set<String> allowedFields=null;
                        if(fileMetadata) {
                    	    // http://jira.dotmarketing.net/browse/DOTCMS-7243
                    	    List<FieldVariable> fieldVariables=APILocator.getFieldAPI().getFieldVariablesForField(
                                    f.getInode(), APILocator.getUserAPI().getSystemUser(), false);
                            for(FieldVariable fv : fieldVariables) {
                                if(fv.getKey().equals("dotIndexPattern")) {
                                    String[] names=fv.getValue().split(",");
                                    allowedFields=new HashSet<String>();
                                    for(String n : names)
                                        allowedFields.add(n.trim().toLowerCase());
                                }
                            }
                            // aditional fields from the configuration file
                            String configFields=Config.getStringProperty("INDEX_METADATA_FIELDS", "");
                            if(configFields.trim().length()>0) {
                                String[] names=configFields.split(",");
                                if(names.length>0 && allowedFields==null)
                                    allowedFields=new HashSet<String>();
                                for(String n : names)
                                    allowedFields.add(n.trim().toLowerCase());
                            }
                        }
                	    
                		if(keyValueMap!=null && !keyValueMap.isEmpty())
                			for(String key : keyValueMap.keySet())
                			    if(allowedFields==null || allowedFields.contains(key.toLowerCase()))
                			        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName() + "." + key, (String)keyValueMap.get(key));
                	}
                } else {
                    if (f.getFieldContentlet().startsWith("bool")) {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), valueObj.toString());
                    } else if (f.getFieldContentlet().startsWith("float") || f.getFieldContentlet().startsWith("integer")) {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), numFormatter.format(valueObj));
                    } else {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), valueObj.toString());
                    }
                }
            } catch (Exception e) {
                Logger.warn(ESMappingAPIImpl.class, "Error indexing field: " + f.getFieldName()
                        + " of contentlet: " + con.getInode(), e);
                throw new DotDataException(e.getMessage(),e);
            }
        }
	}
	public String toJsonString(Map<String, Object> map) throws JsonGenerationException, JsonMappingException, IOException{
		return mapper.writeValueAsString(map);
	}
	public List<String> dependenciesLeftToReindex(Contentlet con) throws DotStateException, DotDataException, DotSecurityException {
	    List<String> dependenciesToReindex = new ArrayList<String>();

	    ContentletAPI conAPI=APILocator.getContentletAPI();

	    String relatedSQL = "select tree.* from tree where parent = ? or child = ? order by tree_order";
	    DotConnect db = new DotConnect();
	    db.setSQL(relatedSQL);
        db.addParam(con.getIdentifier());
        db.addParam(con.getIdentifier());
        ArrayList<HashMap<String, String>> relatedContentlets = db.loadResults();

        if(relatedContentlets.size()>0) {

            List<Relationship> relationships = RelationshipFactory.getAllRelationshipsByStructure(con.getStructure());

            for(Relationship rel : relationships) {

                List<Contentlet> oldDocs = new ArrayList <Contentlet>();

                String q = "";
                boolean isSameStructRelationship = rel.getParentStructureInode().equalsIgnoreCase(rel.getChildStructureInode());

                if(isSameStructRelationship)
                    q = "+type:content +(" + rel.getRelationTypeValue() + "-parent:" + con.getIdentifier() + " " +
                        rel.getRelationTypeValue() + "-child:" + con.getIdentifier() + ") ";
                else
                    q = "+type:content +" + rel.getRelationTypeValue() + ":" + con.getIdentifier();

                oldDocs  = conAPI.search(q, -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);

                List<String> oldRelatedIds = new ArrayList<String>();
                if(oldDocs.size() > 0) {
                    for(Contentlet oldDoc : oldDocs) {
                        oldRelatedIds.add(oldDoc.getIdentifier());
                    }
                }

                List<String> newRelatedIds = new ArrayList<String>();
                for(HashMap<String, String> relatedEntry : relatedContentlets) {
                    String childId = relatedEntry.get("child");
                    String parentId = relatedEntry.get("parent");
                    if(relatedEntry.get("relation_type").equals(rel.getRelationTypeValue())) {
                        if(con.getIdentifier().equalsIgnoreCase(childId)) {
                            newRelatedIds.add(parentId);
                            oldRelatedIds.remove(parentId);
                        } else {
                            newRelatedIds.add(childId);
                            oldRelatedIds.remove(childId);
                        }
                    }
                }

                //Taking the disjunction of both collections will give the old list of dependencies that need to be removed from the
                //re-indexation and the list of new dependencies no re-indexed yet
                dependenciesToReindex.addAll(CollectionUtils.disjunction(oldRelatedIds, newRelatedIds));
            }
        }
        return dependenciesToReindex;
	}

	protected void loadRelationshipFields(Contentlet con, Map<String,String> m) throws DotStateException, DotDataException {
	    List<Relationship> relationships = RelationshipFactory.getAllRelationshipsByStructure(con.getStructure());

	    if(relationships==null || relationships.isEmpty())
	        // if no relationships then nothing to do here
	        return;

	    Identifier identifier = APILocator.getIdentifierAPI().find(con);

	    String relatedSQL = "select tree.* from tree where parent = ? or child = ? order by tree_order";
        DotConnect db = new DotConnect();
        db.setSQL(relatedSQL);
        db.addParam(identifier.getInode());
        db.addParam(identifier.getInode());
        ArrayList<HashMap<String, String>> relatedContentlets = db.loadResults();

        for(Relationship rel : relationships) {

            boolean isSameStructRelationship = rel.getParentStructureInode().equalsIgnoreCase(rel.getChildStructureInode());
            String propName = rel.getRelationTypeValue();
            String propValues = "";

            for(HashMap<String, String> relatedEntry : relatedContentlets) {
                String childId = relatedEntry.get("child");
                String parentId = relatedEntry.get("parent");
                Long order = Long.parseLong(relatedEntry.get("tree_order"));
                if(isSameStructRelationship) {
                    propName = identifier.getInode().equals(parentId)?rel.getRelationTypeValue() + "-child":rel.getRelationTypeValue() + "-parent";
                }
                if(relatedEntry.get("relation_type").equals(rel.getRelationTypeValue())) {
                    if(identifier.getInode().equalsIgnoreCase(childId)) {
                        //Index property value
                        //Index property name if the content is on a same structure (same structure child and parent) kind of relationships
                        //The we append "-child" at the end of the property to distinguish when indexing the children or the parents
                        //side of the relationship
                        //checking if there already values saved on the relationship property, if it's true then
                        //appending the new values to the property
                        if(m.get(propName) != null) {
                            propValues = m.get(propName) + parentId + " ";
                            m.remove(propName);
                        } else {
                            propValues = parentId + " ";
                        }
                        m.put(propName, propValues);
                        //Adding the order of the content to be able to search order by the relationship implicit tree order
                        m.put(rel.getRelationTypeValue() + "-" + parentId + "-order", NumberUtil.pad(order));
                    } else {
                        //Index property value
                        //Index property name if the content is on a same structure (same structure child and parent) kind of relationships
                        //The we append "-child" at the end of the property to distinguish when indexing the children or the parents
                        //side of the relationship
                        if(m.get(propName) != null) {
                            propValues = m.get(propName) + childId + " ";
                            m.remove(propName);
                        } else {
                            propValues = childId + " ";
                        }
                        m.put(propName, propValues);
                        //Adding the order of the content to be able to search order by the relationship implicit tree order
                        m.put(rel.getRelationTypeValue() + "-" + childId + "-order", NumberUtil.pad(order));
                    }
                }
            }
        }

	}
	

}
