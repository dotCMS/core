package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.contenttype.business.DotAssetAPI;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.ContentsWebAPI;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.KeyValueFieldUtil;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * The purpose of this object is to provide an easy way on the frontend of dotCMS
 * to get at fields, categories, permissions and other related things surrounding content.
 * From Velocity you should be able to do things like $content.myField where myfield is the
 * velocity variable of a field on the content.
 *
 * On a technical note maintainers of this class need to ensure that Velocity's introspector's datacache
 * doesn't grow out of control when pulling saying thousands of these objects. We have seen
 * Velocity not do well when storing many categories in it's context.
 * @author Jason Tesser
 * @since 1.9.3
 */
public class ContentMap implements Serializable {

	private Contentlet content;
	private ContentletAPI conAPI;
	private PermissionAPI perAPI;
	private List<Field> fields;
	private Map<String,Field> fieldMap;
	private Map<String, Object> fieldValueMap;
	private User user;
	private boolean EDIT_OR_PREVIEW_MODE;
	private Host host;
	private Structure structure;
	private String title;
	private Context context;
    public ContentMap(Contentlet content, User user, PageMode mode, Host host, Context context) {
        this( content,  user, !mode.showLive,  host,  context) ;
    }
	public ContentMap(Contentlet content, User user, boolean EDIT_OR_PREVIEW_MODE, Host host, Context context) {
        this.content = content;
        this.conAPI = APILocator.getContentletAPI();
        this.perAPI = APILocator.getPermissionAPI();
        this.fields = FieldsCache.getFieldsByStructureInode(content.getStructureInode());
        this.user = user;
        this.EDIT_OR_PREVIEW_MODE = EDIT_OR_PREVIEW_MODE;
        this.host = host;
        this.context = context;
	}

	/**
	 * Use to get a value of the field on a content returned from the ContentTool Viewtool
	 * This method gets called automatically when you place a "." after the contentmap object in Velocity<br/>
	 * EXAMPLE : $mycontent.headline will call this method and return the value for the headline field of a piece of content.<br/>
	 * NOTE: This is the last thing that gets called meaning if you do $mycontent.urlMap it will call the actual getUrlMap because that
	 * method exists. This is case sensitive and uses standard Java bean reflection. For those not familiar here take note that the way to
	 * call the getUrlMap is $mycontent.urlMap the get is removed and the next letter us lowered.<br/>
	 *
	 * Notes and Examples on Field Types <br/>
	 * CATEGORY FIELDS : The category field is a heavier pull.  It is retrieved lazily meaning not until you say $mycon.mycatfield will it get retrieved.
	 * It is not a bad performance but certainly slower then not displaying the category fields. Searching for categories doesn't effect the speed at all
	 * it is only displaying them that will. The value returned to Velocity are the actual Category Objects. You get an ArrayList of them<br/>
	 * <br/>
	 * FILE/IMAGE FIELDS: You can get File/Image fields as well. $con.myimage or $con.myfile. It will return a FileAssetMap object which wraps the actual File object from dotCMS.It adds the uri as a variable.
	 * All the objects have toString implemented on them which means you can spit it out in velocity and see what it available to you.<br/>
	 * <br/>
	 * BINARY FIELDS : You can also get at binary field types. $mycon.myBinaryField This return the BinaryMap object to you.<br/>
	 * TAG FIELDS : You get a TagList which is an arrayList that lets you get at the raw tag value. Meaning a comma separated list of values. <br />
	 * HOST FIELDS OR HOST : Will return a ContentMap of the host or for the Folder the actual Folder <br/>
	 * MULTI SELECT FIELDS : Returns MultiSelectMap which provides you Lists for the Options Values and Labels as well as a List of the Selected Values for this Content<br/>
	 * SELECT FIELDS : Returns SelectMap which provides you Lists for the Options Values and Labels as well as the Selected Value for this Content<br/>
	 * RADIO FIELDS : Returns RadioMap which provides you Lists for the Options Values and Labels as well as the Selected Value for this Content<br/>
	 * @param fieldVariableName The velocity Variable name from the structure.
	 * @return
	 */

	public Object get(String fieldVariableName) {
		return get(fieldVariableName, true);
	}

	/**
	 * Use to get an unparsed value of the field on a content returned from the ContentTool Viewtool, even if it contains velocity code
	 * @param fieldVariableName The velocity Variable name from the structure.
	 * @return
	 */

	public Object getRaw(String fieldVariableName) {
		return get(fieldVariableName, false);
	}

	/**
	 * Recovery the field variables for a content type field (if the field already exists, otherwise returns an empty collection)
	 * @param fieldVariableName String field var name
	 * @return Map
	 */
	public Object getFieldVariables(final String fieldVariableName) {

		final  Map<String, com.dotcms.contenttype.model.field.Field> fieldMap =
				this.content.getContentType().fieldMap();

		if (fieldMap.containsKey(fieldVariableName)) {

			return fieldMap.get(fieldVariableName).fieldVariablesMap();
		}

		return Collections.emptyMap();
	}

		/**
    	 * Recovery the field variables as a json object
    	 * @param fieldVariableName String field var name
    	 * @return Map
    	 */
    	public Object getFieldVariablesJson(final String fieldVariableName) {

    		final  Map<String, FieldVariable> fieldMap =
    				(Map<String, FieldVariable>) this.getFieldVariables(fieldVariableName);

    		final JSONObject jsonObject = new JSONObject();

    		for (final Map.Entry<String, FieldVariable> fieldKey : fieldMap.entrySet()) {

    			final JSONObject jsonObjectFieldVariable = new JSONObject();

    			jsonObjectFieldVariable.put("value", fieldKey.getValue().value());
    			jsonObjectFieldVariable.put("fieldId", fieldKey.getValue().fieldId());
    			jsonObjectFieldVariable.put("key", fieldKey.getValue().key());
    			jsonObjectFieldVariable.put("id", fieldKey.getValue().id());
    			jsonObjectFieldVariable.put("modDate", fieldKey.getValue().modDate());
    			jsonObjectFieldVariable.put("userId", fieldKey.getValue().userId());
    			jsonObjectFieldVariable.put("name", fieldKey.getValue().name());
    			jsonObject.put(fieldKey.getKey(), jsonObjectFieldVariable);
    		}

    		return jsonObject;
    	}

	/**
	 * Returns the value of the specified field on this content returned by the {@link ContentTool} ViewTool. This method
	 * allows you to choose whether the value must have its Velocity code parsed or not.
	 *
	 * @param fieldVariableName The Velocity Variable Name for the specified field.
	 * @param parseVelocity     If potential Velocity code must be parsed, set this to {@code true}.
	 *
	 * @return The value of the specified contentlet field.
	 */
	private Object get(final String fieldVariableName, final Boolean parseVelocity) {
		try {
			final boolean respectFrontEndRoles = PageMode.get(Try.of(()->(HttpServletRequest)context.get("request")).getOrNull()).respectAnonPerms;
			Object ret = null;
			Field f = retriveField(fieldVariableName);
			if(f==null){
				if("host".equalsIgnoreCase(fieldVariableName)){
					try{
						return new ContentMap(conAPI.findContentletByIdentifier( content.getHost() ,!EDIT_OR_PREVIEW_MODE, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, true ),user,EDIT_OR_PREVIEW_MODE,host,context);
					} catch (final IndexOutOfBoundsException e) {
						Logger.debug(this, String.format("Unable to get the Site object from content with ID '%s'",
								this.content.getIdentifier()));
						return null;
					}
				}else if("title".equalsIgnoreCase(fieldVariableName)){
					ret =  getContentletsTitle();
				}else if("structure".equalsIgnoreCase(fieldVariableName) || "contenttype".equalsIgnoreCase(fieldVariableName)){
					return getStructure();
				//http://jira.dotmarketing.net/browse/DOTCMS-6033
				}else if(fieldVariableName.contains("FileURI")){
					f = retriveField(fieldVariableName.replaceAll("FileURI", ""));
					if(f!=null && (f.getFieldType()!= null && f.getFieldType().equals(Field.FieldType.FILE.toString())
							|| f.getFieldType().equals(Field.FieldType.IMAGE.toString()))){
						String fid = (String)conAPI.getFieldValue(content, f);
						if(!UtilMethods.isSet(fid)){
							return null;
						}
						Identifier i = APILocator.getIdentifierAPI().find(fid);

						if(i!=null && InodeUtils.isSet(i.getId()) && i.getAssetType().equals("contentlet")){
							return i.getPath();
						}
					}
					return null;

				}else{
					return content.getMap().get(fieldVariableName);
				}
			}
			if(f != null && f.getFieldType().equals(Field.FieldType.CATEGORY.toString())){
				return 
						conAPI.getFieldValue(content, new LegacyFieldTransformer(f).from(),
								this.user, respectFrontEndRoles);
			}else if(f != null && (f.getFieldType().equals(Field.FieldType.FILE.toString()) || f.getFieldType().equals(Field.FieldType.IMAGE.toString()))){
                // Check if image or file is in fieldValueMap hashmap
                Object fieldvalue = retriveFieldValue(f);
                if (fieldvalue != null) {
                    return fieldvalue;
                }

			    final String fid = (String)conAPI.getFieldValue(content, f);
				if(!UtilMethods.isSet(fid)){
					return null;
				}
				Identifier i = APILocator.getIdentifierAPI().find(fid);
				Optional<ContentletVersionInfo> cvi =  APILocator.getVersionableAPI().getContentletVersionInfo(i.getId(), content.getLanguageId());
				if(cvi.isEmpty()) {
				    final long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
				    if(content.getLanguageId() != defaultLanguageId && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE",true)){
				        cvi =  APILocator.getVersionableAPI().getContentletVersionInfo(i.getId(), defaultLanguageId);
				    }
				}

				if(cvi.isEmpty()) {
					return null;
				}

                String inode = EDIT_OR_PREVIEW_MODE ? cvi.get().getWorkingInode() : cvi.get().getLiveInode();
                Contentlet asset = APILocator.getContentletAPI().find(inode,
                                user != null ? user : APILocator.getUserAPI().getAnonymousUser(), true);

                if (asset == null || UtilMethods.isEmpty(asset.getInode())) {
                    return null;
                }
                if (asset.isFileAsset()) {
                    FileAssetMap fam = FileAssetMap.of(asset);
                    // Store file asset map into fieldValueMap
                    addFieldValue(f, fam);
                    return fam;
                }
                if (asset.isDotAsset()) {
                    BinaryMap binmap = new BinaryMap(asset,
							asset.getContentType().fieldMap().get("asset"), context);
                    // Store file asset map into fieldValueMap
                    addFieldValue(f, binmap);
                    return binmap;
                }
			}else if(f != null && f.getFieldType().equals(Field.FieldType.BINARY.toString())){
                // Check if fileAsset or binaryMap is in fieldValueMap hashmap
                Object fieldvalue = retriveFieldValue(f);
                if (fieldvalue != null) {
                    return fieldvalue;
                }

                // Field value is not present in fieldValueMap hashmap
                if (BaseContentType.FILEASSET.equals(content.getContentType().baseType())
                        && "fileasset".equalsIgnoreCase(f.getVelocityVarName())) {
                    // http://jira.dotmarketing.net/browse/DOTCMS-7406
                    FileAssetMap fam = FileAssetMap.of(content);

                    // Store file asset into fieldValueMap
                    addFieldValue(f, fam);
                    return fam;
                } else {
                    BinaryMap bm = new BinaryMap(content, f, context);

                    // Store file asset into fieldValueMap
                    addFieldValue(f, bm);
                    return bm;
                }
			//if the property being served is URL and the ContentType is a page show URL using the identifier information
			}else if("url".equalsIgnoreCase(fieldVariableName)
			        && BaseContentType.HTMLPAGE.equals(content.getContentType().baseType())){
				Identifier identifier = APILocator.getIdentifierAPI().find(content.getIdentifier());
				if(InodeUtils.isSet(identifier.getId())){
					return identifier.getURI();
				}else{
					Logger.debug(this, String.format("Value of URL field '%s' could not be retrieved from page with ID" +
															 " '%s'. It might not exist in the 'identifier' table.",
							fieldVariableName, this.content.getIdentifier()));
				}
				return null;
			}else if(f != null && f.getFieldType().equals(Field.FieldType.TAG.toString())){

				StringBuilder tags = new StringBuilder();

				//Search for the list of tags related to this contentlet
				List<Tag> foundTags = APILocator.getTagAPI().getTagsByInode(content.getInode());
				if ( foundTags != null && !foundTags.isEmpty() ) {

					Iterator<Tag> iterator = foundTags.iterator();
					while ( iterator.hasNext() ) {

						Tag foundTag = iterator.next();
						tags.append(foundTag.getTagName());

						if ( iterator.hasNext() ) {
							tags.append(",");
						}
					}
				}

				return new TagList(tags.toString());
			}else if(f != null && f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
				if(FolderAPI.SYSTEM_FOLDER.equals(content.getFolder())){
					try{
						return new ContentMap(conAPI.findContentletByIdentifier( content.getHost() ,!EDIT_OR_PREVIEW_MODE, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, true ),user,EDIT_OR_PREVIEW_MODE,host,context);
					} catch (final IndexOutOfBoundsException e) {
						Logger.debug(this, String.format("Unable to get the Site object from content with ID '%s'",
								this.content.getIdentifier()));
						return null;
					}
				}else{
					return APILocator.getFolderAPI().find(content.getFolder(), user, true);
				}
			}else if(f != null && f.getFieldType().equals(Field.FieldType.SELECT.toString())){
				return new SelectMap(f, content);
			}else if(f != null && f.getFieldType().equals(Field.FieldType.RADIO.toString())){
				return new RadioMap(f, content);
			}else if(f != null && f.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString())){
				return new MultiSelectMap(f, content);
			}else if(f != null && f.getFieldType().equals(Field.FieldType.CHECKBOX.toString())){
				return new CheckboxMap(f, content);
			}else if(f != null && f.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())){

				Map<String, Object> keyValueMap = new HashMap<>();
				Map<String, Object> retMap = new LinkedHashMap<>();

				final Object object = conAPI.getFieldValue(content, f);

				if(object instanceof Map){
					keyValueMap=(Map)object;
				}

				if (object instanceof String) {
					final String jsonData = (String) object;
					keyValueMap = KeyValueFieldUtil.JSONValueToHashMap(jsonData);
					//needs to be ordered
					retMap = new java.util.LinkedHashMap<String, Object>() {
						@Override
						public String toString() {
							return jsonData;
						}
					};
				}

				for (String key : keyValueMap.keySet()) {
					retMap.put(key, keyValueMap.get(key));
				}
				retMap.put("keys", keyValueMap.keySet());
				retMap.put("map", keyValueMap);
				return retMap;
			} else if(f != null && f.getFieldType().equals(FieldType.RELATIONSHIP.toString())){
				return getRelationshipInfo(f);
			} else if(f != null && f.getFieldType().equals(FieldType.STORY_BLOCK_FIELD.toString())){
				return new StoryBlockMap(f,content, this.context);
			} else if(f != null && f.getFieldType().equals(FieldType.JSON_FIELD.toString())){
				Field finalF = f;
				return Try.of(()->JsonUtil.getJsonFromString(
						(String)conAPI.getFieldValue(content, finalF)))
						.getOrElse(Collections.emptyMap());
			}

			//ret could have been set by title
			if(ret == null){
				ret = conAPI.getFieldValue(content, f);
			}

			// if return value is date, convert to timestamp to be used in velocity
			if (ret instanceof Date && !(ret instanceof Timestamp)) {
				ret = new Timestamp(((Date) ret).getTime());
			}

			//handle Velocity Code
			if(parseVelocity && ret != null && (f == null || f.getFieldType().equals(Field.FieldType.TEXT.toString()) || f.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || f.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString()) || f.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) && (ret.toString().contains("#") || ret.toString().contains("$"))){
				VelocityEngine ve = VelocityUtil.getEngine();
				Template template = null;
				StringWriter sw = new StringWriter();

				template = ve.getTemplate((EDIT_OR_PREVIEW_MODE ? PageMode.PREVIEW_MODE.name():PageMode.LIVE.name()) + File.separator + content.getInode() + File.separator + f.getInode() + "." + VelocityType.FIELD.fileExtension);
				template.merge(context, sw);
				ret = sw.toString();
			}
			return ret;
		} catch (final Exception e) {
			final String errorMsg = String.format("Unable to retrieve Field '%s' from Content with ID '%s': %s",
					fieldVariableName, this.content.getIdentifier(), e.getMessage());
			Logger.warn(ContentMap.class, errorMsg);
			Logger.debug(ContentMap.class, errorMsg, e);
			return null;
		}
	}

	/***
	 * Given a relationship field, returns a ContentMap object if this side of the relationship
	 * allows only one (see {@link ContentletRelationshipRecords#doesAllowOnlyOne()}),
	 * according to the relationship cardinality. Otherwise, a list of ContentMap
	 * objects is returned
	 * @param field
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private Object getRelationshipInfo(final Field field)
			throws DotDataException, DotSecurityException {
		final ContentletRelationships relationships = (ContentletRelationships) conAPI.getFieldValue(content,
				new LegacyFieldTransformer(field).from(), this.user,false);
		final ContentletRelationshipRecords records = relationships.getRelationshipsRecords().get(0);
		if(records.getRecords().isEmpty()) {
		    return null;
		} else if (records.doesAllowOnlyOne()){
			return new ContentMap(records.getRecords().get(0),user, EDIT_OR_PREVIEW_MODE, host, context);
		} else {
			final List<Contentlet> contents = records.getRecords().stream()
					.filter(contentlet -> needLiveVersion(contentlet))
					.map(relatedContent -> EDIT_OR_PREVIEW_MODE ? relatedContent : getLiveVersion(relatedContent))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

			return perAPI.filterCollection(contents, PermissionAPI.PERMISSION_USE, true, user).stream()
					.map(contentlet -> new ContentMap(contentlet, user, EDIT_OR_PREVIEW_MODE, host, context))
					.collect(Collectors.toList());
		}


	}

	private Contentlet getLiveVersion(final Contentlet relatedContent) {
		try {
			if (relatedContent.isLive()) {
				return relatedContent;
			} else {
				try {
					final List<Contentlet> contentlets = APILocator.getContentletAPI()
							.findContentletsByIdentifiers(new String[]{relatedContent.getIdentifier()}, true, relatedContent.getLanguageId(), user, user.isFrontendUser());

					return contentlets != null && !contentlets.isEmpty() ? contentlets.get(0) : null;
				} catch (DotDataException| DotSecurityException e) {
					throw new RuntimeException(e);
				}
			}
		} catch (DotDataException | DotSecurityException e) {
			return null;
		}
	}

	private boolean needLiveVersion(final Contentlet contentlet) {
		return Try.of(() -> EDIT_OR_PREVIEW_MODE || !EDIT_OR_PREVIEW_MODE && contentlet.hasLiveVersion())
				.getOrElse(false);
	}

	/**
    * Returns the returns the identifier based URI for the
    * first doc/file on a piece of content
    * EXAMPLE : $mycontent.shorty
    * @return
    * @throws IOException
    */
    public String getShortyUrl() throws IOException{
        return getShortyUrl(content.getIdentifier());
    }

    /**
    * Returns the valid short version of the
    * identifier
    * @return
    * @throws IOException
    */
    public String getShorty() throws IOException{
        return APILocator.getShortyAPI().shortify(content.getIdentifier());
    }

    /**
    * Returns the valid short version of the
    * inode
    * @return
    * @throws IOException
    */
    public String getShortyInode() throws IOException{
        return APILocator.getShortyAPI().shortify(content.getInode());
    }
    /**
    * Returns the returns the identifier based URI for the
    * first doc/file on a piece of content
    * EXAMPLE : $mycontent.shortyInode
    * @return
    * @throws IOException
    */
    public String getShortyUrlInode() throws IOException{
        return getShortyUrl(content.getInode());
    }



    private String getShortyUrl(final String idInode) throws IOException{
        String tryField=getFileField();
        StringBuilder sb = new StringBuilder("/dA/").append(APILocator.getShortyAPI().shortify(idInode));
        if(tryField!=null){
          java.io.File f = content.getBinary(tryField);
          if(f !=null && f.exists()){
            sb.append("/").append(content.getBinary(tryField).getName()) ;
          }
        }
        return sb.toString();
    }

    private String getFileField() throws IOException{
        for (Field f : FieldsCache.getFieldsByStructureInode(content.getStructureInode())) {
            if ("binary".equals(f.getFieldType())) {
                return f.getVelocityVarName();
            }
        }
        return null;
    }



	/**
	 * Returns the URLMap if it exists for a piece of content. <br/>
	 * EXAMPLE : $mycontent.urlMap OR $mycontent.getUrlMap() both of these work the same.
	 * @return
	 */
	public String getUrlMap(){
		String result = null;
		try {
			result = conAPI.getUrlMapForContentlet(content, user, true);
		} catch (Exception e) {
			Logger.warn(ContentsWebAPI.class, e.toString());
		}
		return result;
	}

	private Field retriveField(String fieldVariableName) throws Exception{
		if(fieldMap == null){
			fieldMap = UtilMethods.convertListToHashMap(fields, "getVelocityVarName", String.class);
		}
		return fieldMap.get(fieldVariableName);
	}

    /**
     * Returns the value object using the velocity var name stored in the Field
     * object
     *
     * @param field
     * @returns field value object (FileAssetMap or BinaryMap)
     */
    private Object retriveFieldValue(Field field) {
        if (fieldValueMap == null) {
            // Lazy init
            fieldValueMap = new HashMap<>();
        }
        return fieldValueMap.get(field.getVelocityVarName());
    }

    /**
     * Add field value object to the map
     * @param field
     * @param value
     */
    private void addFieldValue(Field field, Object value) {
        if (fieldValueMap == null) {
            // Lazy init
            fieldValueMap = new HashMap<>();
        }
        fieldValueMap.put(field.getVelocityVarName(), value);
    }

	public Structure getStructure() {
		structure = content.getStructure();
		return structure;
	}

	public String getContentletsTitle() {
		title = content.getTitle();
		return title;
	}

	public boolean isLive() throws Exception {
	    return content.isLive();
	}
	public boolean isWorking() throws Exception {
	    return content.isWorking();
	}

	public String toString() {
		getContentletsTitle();
		getStructure();
		return ToStringBuilder.reflectionToString(this);
	}

	public Boolean isHTMLPage() {
		return content.isHTMLPage();
	}

	/**
	 * Returns the {@link Contentlet} object this map is associated to.
	 *
	 * @return The {@link Contentlet} object.
	 */
	public Contentlet getContentObject() {
		return this.content;
	}
}
