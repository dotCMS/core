package com.dotcms.contenttype.transform.contenttype;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.structure.model.Structure;

public class StructureTransformer extends FromStructureTransformer implements StructureTransformerIf  {
	final List<Structure> structList;

	public StructureTransformer(ContentType type) {
		super(type);
		this.structList = ImmutableList.of(transformToStruct(type));
	}

	public StructureTransformer(List<ContentType> initList) {
		super(initList,true);
		this.structList=transformToStruct(initList);
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
		struct.setDefaultStructure(type.defaultStructure());
		struct.setDescription(type.description());
		struct.setDetailPage(type.pagedetail());
		struct.setInode(type.inode());
		struct.setExpireDateVar(type.expireDateVar());
		struct.setFixed(type.fixed());
		struct.setFolder(type.folder());
		struct.setHost(type.host());
		struct.setiDate(type.iDate());
		struct.setIdentifier(type.inode());
		struct.setModDate(type.modDate());
		struct.setName(type.name());
		struct.setOwner(type.owner());
		struct.setPagedetail(type.pagedetail());
		struct.setPublishDateVar(type.publishDateVar());
		struct.setStructureType(type.baseType().getType());
		struct.setSystem(type.system());
		struct.setType(type.type());
		struct.setUrlMapPattern(type.urlMapPattern());
		struct.setVelocityVarName(type.velocityVarName());
		return struct;

	}

	@Override
	public Structure asStructure() throws DotStateException {
		return this.structList.get(0);
	}
	@Override
	public List<Structure> asStructureList() throws DotStateException {
		return this.structList;
	}
}

/**
 * Fields in the db inode owner idate type inode name description
 * default_structure page_detail structuretype system fixed velocity_var_name
 * url_map_pattern host folder expire_date_var publish_date_var mod_date
 **/
