package com.dotcms.contenttype.transform.contenttype;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.structure.model.Structure;

public class ToStructureTransformer  {
	final List<Structure> list;

	public ToStructureTransformer(ContentType type) {
		this.list = ImmutableList.of(transform(type));
	}

	public ToStructureTransformer(List<ContentType> initList) {
		
		List<Structure> newList = new ArrayList<Structure>();
		for (ContentType type : initList) {
			newList.add(transform(type));
		}
		this.list = ImmutableList.copyOf(newList);
	}

	@SuppressWarnings("static-method")
	private Structure transform(final ContentType type) throws DotStateException {

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


	public Structure from() throws DotStateException {
		return this.list.get(0);
	}

	public List<Structure> asList() throws DotStateException {
		return this.list;
	}
}

/**
 * Fields in the db inode owner idate type inode name description
 * default_structure page_detail structuretype system fixed velocity_var_name
 * url_map_pattern host folder expire_date_var publish_date_var mod_date
 **/
