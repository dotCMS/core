package com.dotcms.contenttype.transform.contenttype;

import java.util.List;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;

public class ImplClassContentTypeTransformer implements ContentTypeTransformer{
	final List<ContentType> list;
	
	
	public ImplClassContentTypeTransformer(ContentType ct){
		this.list = ImmutableList.of(transformToSubclass(ct));
	}
	


	private static ContentType transformToSubclass(ContentType type) throws DotStateException{
		final BaseContentType TYPE = type.baseType();
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
				throw new DotStateException("invalid content type");
		}
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
