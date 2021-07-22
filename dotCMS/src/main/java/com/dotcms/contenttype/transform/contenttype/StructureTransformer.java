package com.dotcms.contenttype.transform.contenttype;

import com.dotmarketing.db.DbConnectionFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class StructureTransformer implements ContentTypeTransformer  {
	final List<Structure> structList;
	final List<ContentType> cTypeList;
	public StructureTransformer(ContentType type) {
		this(ImmutableList.of(type));

	}
	public StructureTransformer(Structure type) {
		this(ImmutableList.of(type));
	}
	public StructureTransformer(List<? extends ContentTypeIf> types) {
		List<Structure> strucs = new ArrayList<>(types.size());
		List<ContentType> cTypes = new ArrayList<>(types.size());
		for(ContentTypeIf type : types){
			if(type instanceof Structure){
				strucs.add((Structure)type);
				cTypes.add(transformToContentType((Structure)type));
			}else{
				strucs.add(transformToStruct((ContentType)type));
				cTypes.add((ContentType)type);
			}
			
		}

		this.cTypeList = ImmutableList.copyOf(cTypes);
		this.structList = ImmutableList.copyOf(strucs);
	}


	private static List<Structure> transformToStruct(final List<ContentType> types) throws DotStateException {
		List<Structure> newList = new ArrayList<Structure>();
		for (ContentType type : types) {
			newList.add(transformToStruct(type));
		}
		return ImmutableList.copyOf(newList);
		
	}
	@SuppressWarnings("static-method")
	private static Structure transformToStruct(final ContentType type) throws DotStateException {

		final Structure struct = new Structure();
		struct.setDefaultStructure(type.defaultType());
		struct.setDescription(type.description());
		struct.setDetailPage(type.detailPage());
		struct.setInode(type.inode());
		struct.setFixed(type.fixed());
		struct.setFolder(type.folder());
		struct.setHost(type.host());
		struct.setiDate(type.iDate());
		struct.setIdentifier(type.inode());
		struct.setModDate(type.modDate());
		struct.setName(type.name());
		struct.setOwner(type.owner());
		struct.setPublishDateVar(type.publishDateVar());
		struct.setExpireDateVar(type.expireDateVar());
		struct.setStructureType(type.baseType().getType());
		struct.setSystem(type.system());
		struct.setUrlMapPattern(type.urlMapPattern());
		struct.setVelocityVarName(type.variable());
		struct.setIcon(type.icon());
		struct.setSortOrder(type.sortOrder());
		return struct;

	}


	public Structure asStructure() throws DotStateException {
		return this.structList.get(0);
	}

	public List<Structure> asStructureList() throws DotStateException {
		return this.structList;
	}
	
	

	@SuppressWarnings("static-method")
	private ContentType transformToContentType(final Structure struct) throws DotStateException {

		
		BaseContentType base =  BaseContentType.getBaseContentType(struct.getStructureType());

		final ContentType type = new ContentType() {
			static final long serialVersionUID = 1L;

			@Override
			public String variable() {
				return struct.getVelocityVarName();
			}

			@Override
			public String urlMapPattern() {
				return (UrlMapable.class.isAssignableFrom(base.immutableClass()) && UtilMethods.isSet(struct.getUrlMapPattern())) 
				        ? struct.getUrlMapPattern() 
				                : null;
	
			}

			@Override
			public String publishDateVar() {
				return UtilMethods.isSet(struct.getPublishDateVar()) ? struct.getPublishDateVar(): null;
			}

			@Override
			public String detailPage() {
				return (UrlMapable.class.isAssignableFrom(base.immutableClass()) && UtilMethods.isSet(struct.getPagedetail())) 
				        ? struct.getPagedetail() : null;
			}

			@Override
			public String owner() {
			    return UtilMethods.isSet(struct.getOwner()) ? struct.getOwner(): null;

			}

			@Override
			public String name() {
				return struct.getName();
			}

			@Override
			public String id() {
				return UtilMethods.isSet(struct.getInode()) ? struct.getInode() : null;
			}

			@Override
			public String host() {
				return UtilMethods.isSet(struct.getHost()) ? struct.getHost() : Host.SYSTEM_HOST;
			}

			@Override
			public String folder() {
				return UtilMethods.isSet(struct.getFolder()) ? struct.getFolder() : Folder.SYSTEM_FOLDER;
			}

			@Override
			public String expireDateVar() {
			     return UtilMethods.isSet(struct.getExpireDateVar()) ? struct.getExpireDateVar(): null;
	
			}

			@Override
			public String description() {
			    return UtilMethods.isSet(struct.getDescription()) ? struct.getDescription(): null;

			}

			@Override
			public boolean fixed() {
				return struct.isFixed();
			}

			@Override
			public boolean system() {
				return struct.isSystem();
			}

			@Override
			public boolean defaultType() {
				return struct.isDefaultStructure();
			}

			@Override
			public Date modDate() {
				return struct.getModDate();
			}

			@Override
			public Date iDate() {
				return struct.getIDate();
			}

			@Override
			public String icon() {
				return UtilMethods.isSet(struct.getIcon()) ? struct.getIcon() : BaseContentType.iconFallbackMap.get(base);
			}

			@Override
			public int sortOrder() {
				return UtilMethods.isSet(struct.getSortOrder())  ? struct.getSortOrder() : 0;
			}

			@Override
			public BaseContentType baseType() {
				return BaseContentType.getBaseContentType(struct.getStructureType());
			}

		};

		return new ImplClassContentTypeTransformer(type).from();

	}

	@Override
	public ContentType from() throws DotStateException {
		return this.cTypeList.get(0);
	}

	@Override
	public List<ContentType> asList() throws DotStateException {
		return this.cTypeList;
	}
}

/**
 * Fields in the db inode owner idate type inode name description
 * default_structure page_detail structuretype system fixed velocity_var_name
 * url_map_pattern host folder expire_date_var publish_date_var mod_date
 **/
