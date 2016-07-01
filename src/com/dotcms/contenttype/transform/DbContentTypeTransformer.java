package com.dotcms.contenttype.transform;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.field.Field;
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

public class DbContentTypeTransformer {

	public static ContentType transform(final Map<String, Object> map) throws DotDataException {

		int dbType = (int) map.get("structuretype");
		final BaseContentTypes TYPE = BaseContentTypes.values()[dbType];
		final ContentType type = new ContentType() {
			static final long serialVersionUID = 1L;
			@Override
			public String velocityVarName() {
				return String.valueOf(map.get("velocity_var_name"));
			}

			@Override
			public String urlMapPattern() {
				return String.valueOf(map.get("url_map_pattern"));
			}


			@Override
			public String publishDateVar() {
				return String.valueOf(map.get("publish_date_var"));
			}

			@Override
			public String pagedetail() {
				return String.valueOf(map.get("page_detail"));
			}

			@Override
			public String owner() {
				return String.valueOf(map.get("owner"));
			}

			@Override
			public String name() {
				return String.valueOf(map.get("name"));
			}

			@Override
			public String inode() {
				return String.valueOf(map.get("inode"));
			}

			@Override
			public String host() {
				return String.valueOf(map.get("host"));
			}

			@Override
			public String folder() {
				return String.valueOf(map.get("folder"));
			}

			@Override
			public String expireDateVar() {
				return String.valueOf(map.get("expire_date_var"));
			}

			@Override
			public String description() {
				return String.valueOf(map.get("description"));
			}

			@Override
			public boolean fixed() {
				return Boolean.getBoolean(String.valueOf(map.get("fixed")));
			}

			@Override
			public boolean system() {
				return Boolean.getBoolean(String.valueOf(map.get("system")));
			}

			@Override
			public boolean defaultStructure() {
				return Boolean.getBoolean(String.valueOf(map.get("default_structure")));
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
				return BaseContentTypes.NONE;
			}


		};
		
		List<Field> l = type.requiredFields();
		

		switch (TYPE) {
			case NONE:
				throw new DotDataException("invalid content type - base type=none");
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
			builder.add(transform(map));
		}
		return builder.build();
	}
}

/**
 * Fields in the db inode owner idate type inode name description
 * default_structure page_detail structuretype system fixed velocity_var_name
 * url_map_pattern host folder expire_date_var publish_date_var mod_date
 **/
