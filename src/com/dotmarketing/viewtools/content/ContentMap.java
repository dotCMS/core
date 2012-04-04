/**
 * 
 */
package com.dotmarketing.viewtools.content;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.KeyValueFieldUtil;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.viewtools.ContentsWebAPI;
import com.liferay.portal.model.User;

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
public class ContentMap {

	private static final FileAPI fileAPI = APILocator.getFileAPI();
	private Contentlet content;
	private ContentletAPI conAPI;
	private PermissionAPI perAPI;
	private List<Field> fields;
	private Map<String,Field> fieldMap;
	private User user;
	private boolean EDIT_OR_PREVIEW_MODE;
	private Host host;
	private Structure structure;
	private String title;
	private Context context;
	
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
	 * FILE/IMAGE FIELDS: You can get File/Image fields as well. $con.myimage or $con.myfile. It will return a FileMap object which wraps the actual File object from dotCMS.It adds the uri as a variable.
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
		try {
			Object ret = null;
			Field f = retriveField(fieldVariableName);
			if(f==null){
				if(fieldVariableName.equalsIgnoreCase("host")){
					try{
						return new ContentMap(conAPI.search("+type:content +live:true +deleted:false +identifier:" + content.getHost() , 1, -1, "modDate", user, true).get(0), user, EDIT_OR_PREVIEW_MODE, host,context);
					}catch (IndexOutOfBoundsException e) {
						Logger.debug(this, "Unable to get host on content");
						return null;
					}
				}else if(fieldVariableName.equalsIgnoreCase("title")){
					ret =  getContentletsTitle();
				}else if(fieldVariableName.equalsIgnoreCase("structure")){
					return getStructure();
				//http://jira.dotmarketing.net/browse/DOTCMS-6033	
				}else if(fieldVariableName.contains("FileURI")){
					f = retriveField(fieldVariableName.replaceAll("FileURI", ""));
					if(f!=null && f.getFieldType().equals(Field.FieldType.FILE.toString()) 
							|| f.getFieldType().equals(Field.FieldType.IMAGE.toString())){
						String fid = (String)conAPI.getFieldValue(content, f);
						if(!UtilMethods.isSet(fid)){
							return null;
						}
						Identifier i = APILocator.getIdentifierAPI().find(fid);
						IFileAsset file = null;
						String p = "";
						if (EDIT_OR_PREVIEW_MODE){
							p = WorkingCache.getPathFromCache(i.getURI(),InodeUtils.isSet(i.getHostId())?i.getHostId():host.getIdentifier());
						}else{
							p = LiveCache.getPathFromCache(i.getURI(),InodeUtils.isSet(i.getHostId())?i.getHostId():host.getIdentifier());
						}
						p = p.substring(5, p.lastIndexOf("."));
						if(i!=null && InodeUtils.isSet(i.getId()) && i.getAssetType().equals("contentlet")){
							return i.getPath();
						}
						file = fileAPI.find(p,user,false);
						if(file != null && UtilMethods.isSet(file.getInode())){
							return ((File)file).getURI();
						}else{
							return null;
						}
					}else{
						return null;
					}	
				}else{
					return content.getMap().get(fieldVariableName);
				}
			}
			if(f != null && f.getFieldType().equals(Field.FieldType.CATEGORY.toString())){
				return perAPI.filterCollection(new ArrayList<Category>((Set<Category>)conAPI.getFieldValue(content, f)), PermissionAPI.PERMISSION_USE, true, user);
			}else if(f != null && (f.getFieldType().equals(Field.FieldType.FILE.toString()) || f.getFieldType().equals(Field.FieldType.IMAGE.toString()))){
				String fid = (String)conAPI.getFieldValue(content, f);
				if(!UtilMethods.isSet(fid)){
					return null;
				}
				Identifier i = APILocator.getIdentifierAPI().find(fid);
				FileMap fm = new FileMap();
				IFileAsset file = null;
				if (EDIT_OR_PREVIEW_MODE){
					String p = WorkingCache.getPathFromCache(i.getURI(), InodeUtils.isSet(i.getHostId())?i.getHostId():host.getIdentifier());
					p = p.substring(5, p.lastIndexOf("."));
					if(i!=null && InodeUtils.isSet(i.getId()) && i.getAssetType().equals("contentlet")){
						Contentlet fileAsset  = APILocator.getContentletAPI().find(p.substring(0, p.indexOf("\\")), user!=null?user:APILocator.getUserAPI().getAnonymousUser(), false);
						if(fileAsset != null && UtilMethods.isSet(fileAsset.getInode())){
							return new ContentMap(fileAsset, user, EDIT_OR_PREVIEW_MODE,host,context);
						}
					}else{
						file = fileAPI.find(p,user,true);
					}
//					file = (File)APILocator.getVersionableAPI().findWorkingVersion(i, File.class);
				}else{
					String p = LiveCache.getPathFromCache(i.getURI(),InodeUtils.isSet(i.getHostId())?i.getHostId():host.getIdentifier());
					p = p.substring(5, p.lastIndexOf("."));
					if(i!=null && InodeUtils.isSet(i.getId()) && i.getAssetType().equals("contentlet")){
						Contentlet fileAsset  = APILocator.getContentletAPI().find(p.substring(0, p.indexOf("\\")), user!=null?user:APILocator.getUserAPI().getAnonymousUser(), false);
						if(fileAsset != null && UtilMethods.isSet(fileAsset.getInode())){
							return new ContentMap(fileAsset, user, EDIT_OR_PREVIEW_MODE,host,context);
						}
					}else{
						file = fileAPI.find(p,user,true);
					}
//					file = (File) APILocator.getVersionableAPI().findLiveVersion(i, File.class);
				}
				if(file != null && UtilMethods.isSet(file.getInode())){
					BeanUtils.copyProperties(fm, file);
					return fm;
				}else{
					return null;
				}
			}else if(f != null && f.getFieldType().equals(Field.FieldType.BINARY.toString())){
				BinaryMap bm = new BinaryMap(content,f);
				return bm;
			}else if(f != null && f.getFieldType().equals(Field.FieldType.TAG.toString())){
				return new TagList((String)conAPI.getFieldValue(content, f));
			}else if(f != null && f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
				if(FolderAPI.SYSTEM_FOLDER.equals(content.getFolder())){
					try{
						return new ContentMap(conAPI.search("+type:content +live:true +deleted:false +identifier:" + content.getHost(),1,-1,"modDate",user,true).get(0),user, EDIT_OR_PREVIEW_MODE, host, context);
					}catch (IndexOutOfBoundsException e) {
						Logger.debug(this, "Unable to get host on content");
						return null;
					}
				}else{
				     return CacheLocator.getFolderCache().getFolder(content.getFolder());
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
				Map<String,Object> keyValueMap = KeyValueFieldUtil.JSONValueToHashMap((String)conAPI.getFieldValue(content, f));
				Map<String,Object> retMap = new java.util.HashMap<String,Object>();
				for(String key :keyValueMap.keySet()){
					retMap.put(key.replaceAll("\\W",""), keyValueMap.get(key));
				}
				return retMap;
			}
			
			//ret could have been set by title
			if(ret == null){
				ret = conAPI.getFieldValue(content, f);
			}
			
			//handle Velicty Code
			if(ret != null && (f == null || f.getFieldType().equals(Field.FieldType.TEXT.toString()) || f.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || f.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString()) || f.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) && (ret.toString().contains("#") || ret.toString().contains("$"))){
				VelocityEngine ve = VelocityUtil.getEngine();
				Template template = null;
				StringWriter sw = new StringWriter();
				template = ve.getTemplate((EDIT_OR_PREVIEW_MODE ? "working/":"live/") + content.getInode() + "_" + f.getInode() + "." + Config.getStringProperty("VELOCITY_FIELD_EXTENSION"));
				template.merge(context, sw);
				ret = sw.toString();
			}
			return ret;
		} catch (Exception e) {
			Logger.error(ContentMap.class,"Unable to retrive Field or Content: " + e.getMessage(),e);
			return null;
		}
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
	
	public Structure getStructure() {
		structure = content.getStructure();
		return structure;
	}
	
	public String getContentletsTitle() {
		title = content.getTitle();
		return title;
	}
	
	public String toString() {
		getContentletsTitle();
		getStructure();
		return ToStringBuilder.reflectionToString(this);
	}
	
}
