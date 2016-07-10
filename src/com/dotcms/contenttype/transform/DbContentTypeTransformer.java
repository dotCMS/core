package com.dotcms.contenttype.transform;

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
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

public class DbContentTypeTransformer {

	public static ContentType transform(final Map<String, Object> map) throws DotDataException {

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
		

		return transformToSubclass(type);
		
	}
	
	
	public static ContentType transformToSubclass(ContentType type) throws DotDataException{
		final BaseContentTypes TYPE = type.baseType();
		switch (TYPE) {
			case CONTENT:
				return ImmutableSimpleContentType.builder().from(type).build();
			case WIDGET:
				return ImmutableWidgetContentType.builder().from(type).build();
			case FORM:
				return ImmutableFormContentType.builder().from(type).build();
			case FILEASSET:
				return ImmutableFileAssetContentType.builder().from(type).build();
			case HTMLPAGE:
				return ImmutablePageContentType.builder().from(type).build();
			case PERSONA:
				return ImmutablePersonaContentType.builder().from(type).build();
			default:
				throw new DotDataException("invalid content type");
		}
	}
	
	
	public static List<ContentType> transform(final List<Map<String, Object>> list) throws DotDataException {
		ImmutableList.Builder<ContentType> builder = ImmutableList.builder();
		for (Map<String, Object> map : list) {
			
			try{
				builder.add(transform(map));
			}	
			catch(Exception e){
				System.out.println(map);
				throw e;
			}
			

		}
		return builder.build();
	}
}

/**
 * Fields in the db inode owner idate type inode name description
 * default_structure page_detail structuretype system fixed velocity_var_name
 * url_map_pattern host folder expire_date_var publish_date_var mod_date
 **/
