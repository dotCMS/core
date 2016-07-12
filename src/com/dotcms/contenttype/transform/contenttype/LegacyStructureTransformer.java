package com.dotcms.contenttype.transform.contenttype;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.structure.model.Structure;

public class LegacyStructureTransformer implements ToContentTypeTransformer {
	final List<ContentType> list;

	public LegacyStructureTransformer(Structure struct) {
		list = ImmutableList.of(transform(struct));
	}

	public LegacyStructureTransformer(List<Structure> initList) {
		
		List<ContentType> newList = new ArrayList<ContentType>();
		for (Structure struct : initList) {
			newList.add(transform(struct));
		}
		list = ImmutableList.copyOf(newList);
	}

	@SuppressWarnings("static-method")
	private ContentType transform(final Structure struct) throws DotStateException {

		final ContentType type = new ContentType() {
			static final long serialVersionUID = 1L;

			@Override
			public String velocityVarName() {
				return struct.getVelocityVarName();
			}

			@Override
			public String urlMapPattern() {
				return struct.getUrlMapPattern();
			}

			@Override
			public String publishDateVar() {
				return struct.getPublishDateVar();
			}

			@Override
			public String pagedetail() {
				return struct.getPagedetail();
			}

			@Override
			public String owner() {
				return struct.getOwner();
			}

			@Override
			public String name() {
				return struct.getName();
			}

			@Override
			public String inode() {
				return struct.getInode();
			}

			@Override
			public String host() {
				return struct.getHost();
			}

			@Override
			public String folder() {
				return struct.getFolder();
			}

			@Override
			public String expireDateVar() {
				return struct.getExpireDateVar();
			}

			@Override
			public String description() {
				return struct.getDescription();
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
			public boolean defaultStructure() {
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
			public BaseContentTypes baseType() {
				return BaseContentTypes.getBaseContentType(struct.getStructureType());
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
