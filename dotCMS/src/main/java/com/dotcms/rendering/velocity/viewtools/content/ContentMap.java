/**
 *
 */
package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.contenttype.util.KeyValueFieldUtil;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.ContentsWebAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;

import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

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
public class ContentMap implements Serializable{

	private Contentlet content;
	private ContentletAPI conAPI;
	private PermissionAPI perAPI;


	private Map<String, Object> fieldValueMap;
	private User user;
	private boolean EDIT_OR_PREVIEW_MODE;
	private Host host;
	private Context context;
    public ContentMap(Contentlet content, User user, PageMode mode, Host host, Context context) {
        this( content,  user, !mode.showLive,  host,  context) ;
    }
	public ContentMap(Contentlet content, User user, boolean EDIT_OR_PREVIEW_MODE, Host host, Context context) {
        this.content = content;
        this.conAPI = APILocator.getContentletAPI();
        this.perAPI = APILocator.getPermissionAPI();
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

	
	private Object get(final String fieldVariableName, boolean parseVelocity) {
		try {
			final boolean respectFrontEndRoles = PageMode.get(Try.of(()->(HttpServletRequest)context.get("request")).getOrNull()).respectAnonPerms;
			Object ret = null;
			final Field f = content.getContentType().fieldMap().get(fieldVariableName);
			if(f==null){
				if("host".equalsIgnoreCase(fieldVariableName)){
					try{
						return new ContentMap(conAPI.findContentletByIdentifier( content.getHost() ,!EDIT_OR_PREVIEW_MODE, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, true ),user,EDIT_OR_PREVIEW_MODE,host,context);
					}catch (IndexOutOfBoundsException e) {
						Logger.debug(this, "Unable to get host on content");
						return null;
					}
				}else if("title".equalsIgnoreCase(fieldVariableName)){
					ret =  content.getTitle();
				}else if("structure".equalsIgnoreCase(fieldVariableName)) {
					return content.getStructure();
        }else if("contenttype".equalsIgnoreCase(fieldVariableName)) {
          return content.getContentType();
				//http://jira.dotmarketing.net/browse/DOTCMS-6033
				}else if(fieldVariableName.contains("FileURI")){
					Field f2 =  content.getContentType().fieldMap().get(fieldVariableName.replace("FileURI", ""));
					if(f2!=null && (f2 instanceof FileField || f2 instanceof ImageField)){
						String fid = (String)conAPI.getFieldValue(content, f2);
						if(!UtilMethods.isSet(fid)){
							return null;
						}
						Identifier i = APILocator.getIdentifierAPI().find(fid);

						if(i!=null && InodeUtils.isSet(i.getId()) && i.getAssetType().equals("contentlet")){
							return i.getPath();
						}
					}
					return null;

				}
				return content.getMap().get(fieldVariableName);
			}
			
			if(f instanceof ConstantField) {
			  return f.values();
			}else if(f instanceof CategoryField){
				return perAPI.filterCollection(new ArrayList<Category>((Set<Category>)
						conAPI.getFieldValue(content, new LegacyFieldTransformer(f).from(),
								this.user, respectFrontEndRoles)), PermissionAPI.PERMISSION_USE, true, user);
			}else if(f instanceof ImageField || f instanceof FileField){
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
				ContentletVersionInfo cvi =  APILocator.getVersionableAPI().getContentletVersionInfo(i.getId(), content.getLanguageId());
				if(cvi == null) {
				    final long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
				    if(content.getLanguageId() != defaultLanguageId && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE",true)){
				        cvi =  APILocator.getVersionableAPI().getContentletVersionInfo(i.getId(), defaultLanguageId);
				    }
				}

				String inode =  (EDIT_OR_PREVIEW_MODE) ? cvi.getWorkingInode()  : cvi.getLiveInode();
				Contentlet fileAsset  =  APILocator.getContentletAPI().find(inode, user!=null?user:APILocator.getUserAPI().getAnonymousUser(), true);
					
				if(fileAsset != null && UtilMethods.isSet(fileAsset.getInode())){
	                FileAssetMap fam = FileAssetMap.of(fileAsset);
                    // Store file asset map into fieldValueMap
                    addFieldValue(f, fam);
                    return fam;
				  }
					
				
			}else if(f instanceof BinaryField){
                // Check if fileAsset or binaryMap is in fieldValueMap hashmap
                Object fieldvalue = retriveFieldValue(f);
                if (fieldvalue != null) {
                    return fieldvalue;
                }

                // Field value is not present in fieldValueMap hashmap
                if (BaseContentType.FILEASSET.equals(content.getContentType().baseType())
                        && "fileasset".equalsIgnoreCase(f.variable())) {
                    // http://jira.dotmarketing.net/browse/DOTCMS-7406
                    FileAssetMap fam = FileAssetMap.of(content);

                    // Store file asset into fieldValueMap
                    addFieldValue(f, fam);
                    return fam;
                } else {
                    BinaryMap bm = new BinaryMap(content, f);

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
					Logger.debug(this, "The URL can't be get from an empty identifier, the page might not exists on the identifier table.");
				}
				return null;
			}else if(f instanceof TagField){

				//Search for the list of tags related to this contentlet
				List<Tag> foundTags = APILocator.getTagAPI().getTagsByInode(content.getInode());
				return new TagList(foundTags);

			}else if(f instanceof HostFolderField){
				if(FolderAPI.SYSTEM_FOLDER.equals(content.getFolder())){
					try{
						return new ContentMap(conAPI.findContentletByIdentifier( content.getHost() ,!EDIT_OR_PREVIEW_MODE, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, true ),user,EDIT_OR_PREVIEW_MODE,host,context);
					}catch (IndexOutOfBoundsException e) {
						Logger.debug(this, "Unable to get host on content");
						return null;
					}
				}else{
					return APILocator.getFolderAPI().find(content.getFolder(), user, true);
				}
			}else if(f instanceof SelectField){
				return new SelectMap(f, content);
			}else if(f instanceof RadioField){
				return new RadioMap(f, content);
			}else if(f instanceof MultiSelectField){
				return new MultiSelectMap(f, content);
			}else if(f instanceof CheckboxField){
				return new CheckboxMap(f, content);
			}else if(f instanceof KeyValueField){
			    return Try.of(() -> new JSONObject(conAPI.getFieldValue(content, f))).getOrNull();
			} else if(f instanceof RelationshipField){
				return getRelationshipInfo(f);
			}

			//ret could have been set by title
			if(ret == null){
				ret = conAPI.getFieldValue(content, f);
			}

			//handle Velocity Code
			if(parseVelocity && ret != null && (f == null || f instanceof TextField || f instanceof TextAreaField  || f instanceof CustomField || f instanceof WysiwygField) && (ret.toString().contains("#") || ret.toString().contains("$"))){
				VelocityEngine ve = VelocityUtil.getEngine();
				Template template = null;
				StringWriter sw = new StringWriter();

				template = ve.getTemplate((EDIT_OR_PREVIEW_MODE ? PageMode.PREVIEW_MODE.name():PageMode.LIVE.name()) + File.separator + content.getInode() + File.separator + f.id() + "." + VelocityType.FIELD.fileExtension);
				template.merge(context, sw);
				ret = sw.toString();
			}
			return ret;
		} catch (Exception e) {
			Logger.warn(ContentMap.class,"Unable to retrive Field or Content: " + fieldVariableName + " "+ e.getMessage());
			Logger.debug(ContentMap.class,"Unable to retrive Field or Content: " + fieldVariableName + " "+ e.getMessage(),e);
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
		}
		else if (records.doesAllowOnlyOne()){
			return new ContentMap(records.getRecords().get(0),user,
					EDIT_OR_PREVIEW_MODE, host, context);
		} else{
			return perAPI.filterCollection(records.getRecords(),
					PermissionAPI.PERMISSION_USE, true, user).stream()
					.map(contentlet -> new ContentMap((Contentlet) contentlet, user,
							EDIT_OR_PREVIEW_MODE, host, context)).collect(Collectors.toList());
		}


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
        Field tryField=content.getContentType().fields(BinaryField.class).stream().findFirst().orElse(null);
        StringBuilder sb = new StringBuilder("/dA/").append(APILocator.getShortyAPI().shortify(idInode));
        if(tryField!=null){
          java.io.File f = content.getBinary(tryField.variable());
          if(f !=null && f.exists()){
            sb.append("/").append(f.getName()) ;
          }
        }
        return sb.toString();
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
            fieldValueMap = new HashMap<String, Object>();
        }
        return fieldValueMap.get(field.variable());
    }

    /**
     * Add field value object to the map
     * @param field
     * @param value
     */
    private void addFieldValue(Field field, Object value) {
        if (fieldValueMap == null) {
            // Lazy init
            fieldValueMap = new HashMap<String, Object>();
        }
        fieldValueMap.put(field.variable(), value);
    }


	public boolean isLive() throws Exception {
	    return content.isLive();
	}
	public boolean isWorking() throws Exception {
	    return content.isWorking();
	}	

	public String toString() {
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
