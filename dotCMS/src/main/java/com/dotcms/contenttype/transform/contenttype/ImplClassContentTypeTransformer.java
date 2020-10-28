package com.dotcms.contenttype.transform.contenttype;

import java.util.List;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableDotAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutableKeyValueContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;

/**
 * Implementation class for the {@link ContentTypeTransformer}.
 * 
 * @author Will Ezell
 * @since 4.1.0
 * @since Oct 17, 2016
 *
 */
public class ImplClassContentTypeTransformer implements ContentTypeTransformer{
	final List<ContentType> list;
	
	/**
	 * Creates an instance of the {@link ContentTypeTransformer}.
	 * 
	 * @param ct
	 *            - The Content Type that will be transformed into the
	 *            respective sub-class.
	 */
	public ImplClassContentTypeTransformer(ContentType ct){
		this.list = ImmutableList.of(transformToSubclass(ct));
	}

	/**
	 * Returns the appropriate sub-class of a Content Type based on its type.
	 * The different kinds of sub-classes are specified by the Enum
	 * {@link BaseContentType}.
	 * 
	 * @param type
	 *            - The Content Type to transform.
	 * @return The specific Content Type sub-class based on its type.
	 * @throws DotStateException
	 *             The specified Content Type could not be determined.
	 */
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
			case VANITY_URL:
				return ImmutableVanityUrlContentType.builder().from(type).build();
			case KEY_VALUE:
				return ImmutableKeyValueContentType.builder().from(type).build();
			case DOTASSET:
				return ImmutableDotAssetContentType.builder().from(type).build();
			default:
				throw new DotStateException("Invalid content type.");
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
