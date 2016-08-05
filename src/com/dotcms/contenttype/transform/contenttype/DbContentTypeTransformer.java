package com.dotcms.contenttype.transform.contenttype;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;

public class DbContentTypeTransformer implements ContentTypeTransformer{
	final List<ContentType> list;
	
	
	public DbContentTypeTransformer(Map<String, Object> map){
		this.list = ImmutableList.of(transform(map));
	}
	
	public DbContentTypeTransformer(List<Map<String, Object>> initList){
		List<ContentType> newList = new ArrayList<ContentType>();
		for(Map<String, Object> map : initList){
			newList.add(transform(map));
		}
		this.list= ImmutableList.copyOf(newList);
	}
	
	
	private static ContentType transform(final Map<String, Object> map) throws DotStateException {
		BaseContentType base =  BaseContentType.getBaseContentType((Integer) map.get("structuretype"));
		final ContentType type = new ContentType() {
			static final long serialVersionUID = 1L;
			@Override
			public String velocityVarName() {
				return (String) map.get("velocity_var_name");
			}

			@Override
			public String urlMapPattern() {
				return (UrlMapable.class.isAssignableFrom(base.immutableClass())) ? (String) map.get("url_map_pattern") : null;
				
			}


			@Override
			public String publishDateVar() {
				return (String) map.get("publish_date_var");
			}

			@Override
			public String detailPage() {
				return (UrlMapable.class.isAssignableFrom(base.immutableClass())) ? (String) map.get("page_detail") : null;
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
				return convertSQLDate((Date) map.get("mod_date"));

				
			}
			@Override
			public Date iDate() {
				return convertSQLDate((Date) map.get("idate"));
			}
			@Override
			public BaseContentType baseType() {
				return base;
			}

			@Override
			public List<Field> fields() {
				return ImmutableList.of();
			}
			
			
			private Date convertSQLDate(Date d){
				Date javaDate = new Date();
				if(d!=null) javaDate.setTime(d.getTime());
				return javaDate;
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
