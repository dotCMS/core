package com.dotcms.contenttype.transform.contenttype;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.enterprise.license.LicenseManager;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UtilMethods;

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
		if(LicenseManager.getInstance().isCommunity()) {
		    newList.removeIf(t->(t.baseType()==BaseContentType.FORM || t.baseType() == BaseContentType.PERSONA));
		}
		this.list= ImmutableList.copyOf(newList);
	}
	
	private static ContentType transform(final Map<String, Object> map) throws DotStateException {

		BaseContentType base =  BaseContentType.getBaseContentType(DbConnectionFactory.getInt(map.get("structuretype").toString()));

		final ContentType type = new ContentType() {

			static final long serialVersionUID = 1L;

			@Override
			public String variable() {
				return (String) map.get("velocity_var_name");
			}

			@Override
			public String urlMapPattern() {
	             String ret = (String) map.get("url_map_pattern");
	             return (UrlMapable.class.isAssignableFrom(base.immutableClass()) && UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String publishDateVar() {
	            String ret = (String) map.get("publish_date_var");
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String detailPage() {
			    String ret = (String) map.get("page_detail");
				return (UrlMapable.class.isAssignableFrom(base.immutableClass()) && UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String owner() {
                String ret = (String) map.get("owner");
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String name() {
                String ret = (String) map.get("name");
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String id() {
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
                String ret = (String) map.get("expire_date_var");
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String description() {
                String ret = (String) map.get("description");
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public boolean fixed() {
				return DbConnectionFactory.isDBTrue(map.get("fixed").toString());
			}

			@Override
			public boolean system() {
				return DbConnectionFactory.isDBTrue(map.get("system").toString());
			}

			@Override
			public boolean defaultType() {
				return DbConnectionFactory.isDBTrue(map.get("default_structure").toString());
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