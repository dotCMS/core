package com.dotcms.contenttype.transform.contenttype;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

public class DbContentTypeTransformer implements ToContentTypeTransformer{
	final List<ContentType> list;
	
	
	public DbContentTypeTransformer(Map<String, Object> map){
		list = ImmutableList.of(transform(map));
	}
	
	public DbContentTypeTransformer(List<Map<String, Object>> initList){
		List<ContentType> newList = new ArrayList<ContentType>();
		for(Map<String, Object> map : initList){
			newList.add(transform(map));
		}
		list= ImmutableList.copyOf(newList);
	}
	
	
	private ContentType transform(final Map<String, Object> map) throws DotStateException {

		final ContentType type = new ContentType() {
			static final long serialVersionUID = 1L;
			@Override
			public String velocityVarName() {
				return (String) map.get("velocity_var_name");
			}

			@Override
			public String urlMapPattern() {
				return !UtilMethods.isSet((String) map.get("url_map_pattern")) ? null : (String) map.get("url_map_pattern");
			}


			@Override
			public String publishDateVar() {
				return (String) map.get("publish_date_var");
			}

			@Override
			public String pagedetail() {
				return !UtilMethods.isSet((String) map.get("page_detail")) ? null : (String) map.get("page_detail");
			}

			@Override
			public String owner() {
				return (String) map.get("owner");
			}

			@Override
			public String name() {
				return (String) map.get("name");
			}

			@Override
			public String inode() {
				return (String) map.get("inode");
			}

			@Override
			public String host() {
				return (String) map.get("host");
			}

			@Override
			public String folder() {
				return (String) map.get("folder");
			}

			@Override
			public String expireDateVar() {
				return (String) map.get("expire_date_var");
			}

			@Override
			public String description() {
				return (String) map.get("description");
			}

			@Override
			public boolean fixed() {
				return (Boolean) map.get("fixed");
			}

			@Override
			public boolean system() {
				return (Boolean) map.get("system");
			}

			@Override
			public boolean defaultStructure() {
				return (Boolean) map.get("default_structure");
			}

			@Override
			public Date modDate() {
				return (Date) map.get("mod_date");
			}
			@Override
			public Date iDate() {
				return (Date) map.get("idate");
			}
			@Override
			public BaseContentTypes baseType() {
				return BaseContentTypes.getBaseContentType((Integer) map.get("structuretype"));
			}


		};
		

		return new ImplClassContentTypeTransformer(type).from();
		
	}
	
	

	
	



	@Override
	public ContentType from() throws DotStateException {
		return this.list.get(0);
	}


	@Override
	public List<ContentType> asList() throws DotStateException {
		return this.list;
	}
}

/**
 * Fields in the db inode owner idate type inode name description
 * default_structure page_detail structuretype system fixed velocity_var_name
 * url_map_pattern host folder expire_date_var publish_date_var mod_date
 **/
