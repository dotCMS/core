package com.dotcms.content.elasticsearch.business;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.util.NumberUtils;

import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.content.business.DotMappingException;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.KeyValueFieldUtil;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.NumberUtil;
import com.dotmarketing.util.UtilMethods;

public class ESMappingAPIImpl implements ContentMappingAPI {

	static ObjectMapper mapper = null;

	public ESMappingAPIImpl() {
		if (mapper == null) {
			synchronized (this.getClass().getName()) {
				if (mapper == null) {
					mapper = new ObjectMapper();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					mapper.setDateFormat(df);
				}
			}
		}
	}

	/*
	public String buildMapping(Structure struct) {

		Map<String, Object> type = new HashMap<String, Object>();
		Map<String, Object> props = new HashMap<String, Object>();
		Map<String, Object> fields = getDefaultContentletFields();

		// default mapping properties
		Map m = new HashMap();
		m.put("enabled", true);
		props.put("_source", m);

		// Indexed Fields
		try {
			List<Field> conFields = struct.getFields();
			for (Field f : conFields) {
				if (f.isIndexed()) {
					fields.put(f.getVelocityVarName(), getFieldJson(f));

				}

			}
			props.put("properties", fields);
			type.put(struct.getVelocityVarName(), props);
			return mapper.defaultPrettyPrintingWriter().writeValueAsString(type);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
		return null;
	}*/

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
			Structure st=StructureCache.getStructureByInode(con.getStructureInode());
			
			m.put("title", con.getTitle());
			m.put("structureName", st.getVelocityVarName());
            m.put("structureType", st.getStructureType() + ""); 
            m.put("inode", con.getInode());
            m.put("type", "content");
            m.put("modDate", new SimpleDateFormat("yyyyMMddHHmmss").format(con.getModDate()));
            m.put("owner", ident.getOwner() == null ? "0" : ident.getOwner());
            m.put("modUser", con.getModUser());
            m.put("live", Boolean.toString(con.isLive()));
            m.put("working", Boolean.toString(con.isWorking()));
            m.put("locked", Boolean.toString(con.isLocked()));
            m.put("deleted", Boolean.toString(con.isArchived()));
            m.put("languageId", Long.toString(con.getLanguageId()));
            m.put("identifier", ident.getId());
            m.put("conHost", ident.getHostId());
            m.put("conFolder", con.getFolder());
			
            Map<String,String> mlowered=new HashMap<String,String>();
            for(Entry<String,String> entry : m.entrySet()){
                mlowered.put(entry.getKey().toLowerCase(), entry.getValue().toLowerCase());
                mlowered.put(entry.getKey().toLowerCase() + "_dotraw", entry.getValue().toLowerCase());
            }
            
			String x = mapper.writeValueAsString(mlowered);
			return x;
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotMappingException(e.getMessage());
		}
	}

	/*
	public Contentlet toContentlet(String json) throws DotMappingException {

		try {
			JsonNode node = mapper.readValue((String) json, JsonNode.class);
			node = node.path("_source");
			Map<String, Object> map = mapper.readValue(node, HashMap.class);
			return toContentlet(map);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage());
			throw new DotMappingException(e.getMessage());
		}

	}*/

	/*
	public Contentlet toContentlet(Map<String, Object> map) throws DotMappingException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		Contentlet c = new Contentlet();
		for (Map.Entry ent : map.entrySet()) {
			String key = (String) ent.getKey();
			Object obj = ent.getValue();
			if (ent.getValue() instanceof String) {
				String val = (String) ent.getValue();
				if (val.length() == 20 && val.endsWith("Z")) {
					try {

						Date d = df.parse(val);
						obj = d;
					} catch (Exception e) {

					}
				}

			}
			c.setProperty(key, obj);
		}
		return c;
	}*/

	public Object toMappedObj(Contentlet con) throws DotMappingException {
		return toJson(con);
	}

	@SuppressWarnings("unchecked")
	protected void loadCategories(Contentlet con, Map<String,String> m) throws DotDataException {
	    String categoriesSQL = "select category.category_velocity_var_name as cat_velocity_var "+
                " from  category join tree on (tree.parent = category.inode) join contentlet c on (c.inode = tree.child) " +
                " where c.inode = ?";
	    
        DotConnect db = new DotConnect();
        db.setSQL(categoriesSQL);
        db.addParam(con.getInode());
        List<HashMap<String, String>> categoriesResults = db.loadResults();
        StringBuilder categories=new StringBuilder();
        boolean first=true;
        for (HashMap<String, String> crow : categoriesResults) {
            try {
                String categoryId= crow.get("cat_velocity_var");
                if(UtilMethods.isSet(categoryId)) {
                    if(!first)
                        categories.append(" ");
                    else
                        first=false;
                    categories.append(categoryId);
                }
            } catch (Exception e) {
            }
        }
        
        m.put("categories", categories.toString());
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
	
	@SuppressWarnings("unchecked")
	protected void loadFields(Contentlet con, Map<String,String> m) throws DotDataException {
	    FieldAPI fAPI=APILocator.getFieldAPI();
	    List<Field> fields = FieldsCache.getFieldsByStructureInode(con.getStructureInode());
	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	    SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
	    DecimalFormat numFormatter = new DecimalFormat("0000000000000000000.000000000000000000");
	    Structure st=con.getStructure();
        for (Field f : fields) {
            if (f.getFieldType().equals(Field.FieldType.BINARY.toString())
                    || f.getFieldContentlet() != null && f.getFieldContentlet().startsWith("system_field")) {
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
                else if (f.getFieldType().equals("date") || f.getFieldType().equals("date_time")) {
                    try {
                        String dateString = dateFormat.format(valueObj);
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), dateString);
                    }
                    catch(Exception ex) {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(),"");
                    }
                } else if (f.getFieldType().equals("category")) {
                    m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(),m.get("categories"));
                } else if (f.getFieldType().equals("checkbox") || f.getFieldType().equals("multi_select")) {
                    if (f.getFieldContentlet().startsWith("bool")) {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), valueObj.toString());
                    } else {
                        m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), UtilMethods.listToString(valueObj.toString()));
                    }
                } else if (f.getFieldType().equals("key_value")){
                    m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), (String)valueObj);
                    Map<String,Object> keyValueMap = KeyValueFieldUtil.JSONValueToHashMap((String)valueObj);
                    if(keyValueMap!=null && !keyValueMap.isEmpty())
                        for(String key : keyValueMap.keySet())
                            m.put(st.getVelocityVarName() + "." + f.getVelocityVarName() + "." + key, (String)keyValueMap.get(key));
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
