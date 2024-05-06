package com.dotcms.contenttype.transform.contenttype;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Try;
import org.postgresql.util.PGobject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This transformer takes a Map with attributes of a one or more Content Types, and transforms them
 * into the {@link ContentType} objects. Such data Maps usually come directly from queries made to
 * the database.
 *
 * @author Will Ezell
 * @since Jun 29th, 2016
 */
public class DbContentTypeTransformer implements ContentTypeTransformer{
	final List<ContentType> list;

	public DbContentTypeTransformer(Map<String, Object> map){
		this.list = ImmutableList.of(transform(map));
	}
	
	public DbContentTypeTransformer(List<Map<String, Object>> initList){
		List<ContentType> newList = new ArrayList<>();
		for(Map<String, Object> map : initList){
			newList.add(transform(map));
		}
		if(LicenseManager.getInstance().isCommunity()) {
		    newList.removeIf(t->(t.baseType()==BaseContentType.FORM || t.baseType() == BaseContentType.PERSONA));
		}
		this.list= ImmutableList.copyOf(newList);
	}
	
	/**
	 * Transforms a map into a ContentType object.
	 *
	 * @param map The map containing the Content Type's attributes.
	 *
	 * @return A ContentType object.
	 *
	 * @throws DotStateException The Content Type's base type is not supported.
	 */
	private static ContentType transform(final Map<String, Object> map) throws DotStateException {
		final BaseContentType base =  BaseContentType.getBaseContentType(DbConnectionFactory.getInt(map.get(ContentTypeFactory.STRUCTURE_TYPE_COLUMN).toString()));
		final ContentType type = new ContentType() {

			private final ObjectMapper jsonMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

			private static final long serialVersionUID = 1L;

			@Override
			public String variable() {
				return (String) map.get(ContentTypeFactory.VELOCITY_VAR_NAME_COLUMN);
			}

			@Override
			public String urlMapPattern() {
	             String ret = (String) map.get(ContentTypeFactory.URL_MAP_PATTERN_COLUMN);
	             return (UrlMapable.class.isAssignableFrom(base.immutableClass()) && UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String publishDateVar() {
	            String ret = (String) map.get(ContentTypeFactory.PUBLISH_DATE_VAR_COLUMN);
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String detailPage() {
			    String ret = (String) map.get(ContentTypeFactory.PAGE_DETAIL_COLUMN);
				return (UrlMapable.class.isAssignableFrom(base.immutableClass()) && UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String owner() {
                String ret = (String) map.get("owner");
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String name() {
                String ret = (String) map.get(ContentTypeFactory.NAME_COLUMN);
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String id() {
				return (String) map.get(ContentTypeFactory.INODE_COLUMN);
			}

			@Override
			public String host() {
				return (String) map.get(ContentTypeFactory.HOST_COLUMN);
			}

			@Override
			public String folder() {
				return (String) map.get(ContentTypeFactory.FOLDER_COLUMN);
			}

			@Override
			public String expireDateVar() {
                String ret = (String) map.get(ContentTypeFactory.EXPIRE_DATE_VAR_COLUMN);
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public String description() {
                String ret = (String) map.get(ContentTypeFactory.DESCRIPTION_COLUMN);
                return ( UtilMethods.isSet(ret))  ? ret : null;
			}

			@Override
			public boolean fixed() {
				return DbConnectionFactory.isDBTrue(map.get(ContentTypeFactory.FIXED_COLUMN).toString());
			}

			@Override
			public boolean system() {
				return DbConnectionFactory.isDBTrue(map.get(ContentTypeFactory.SYSTEM_COLUMN).toString());
			}

			@Override
			public boolean defaultType() {
				return DbConnectionFactory.isDBTrue(map.get(ContentTypeFactory.DEFAULT_STRUCTURE_COLUMN).toString());
			}

			@Override
			public Date modDate() {
				return convertSQLDate((Date) map.get(ContentTypeFactory.MOD_DATE_COLUMN));
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
			public String icon() {
				final String icon = (String) map.get(ContentTypeFactory.ICON_COLUMN);
				return ( UtilMethods.isSet(icon))  ? icon : BaseContentType.iconFallbackMap.get(base);
			}

			@Override
			public int sortOrder() {
				return UtilMethods.isSet(map.get(ContentTypeFactory.SORT_ORDER_COLUMN)) ?
						DbConnectionFactory.getInt(map.get("sort_order").toString()) : 0;
			}

			@Override
			public boolean markedForDeletion() {
				return Try.of(()->DbConnectionFactory.isDBTrue(map.get(ContentTypeFactory.MARKED_FOR_DELETION_COLUMN).toString())).getOrElse(false);
			}

			@Override
			@SuppressWarnings("unchecked")
			public Map<String, Object> metadata() {
				return Try.of(() -> jsonMapper.readValue(((PGobject) map.get(ContentTypeFactory.METADATA_COLUMN)).getValue(), Map.class)).getOrElse(new HashMap<>());
			}

			private Date convertSQLDate(final Date d){
				final Date javaDate = new Date();
				if (d != null) {
					javaDate.setTime(d.getTime());
				}
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