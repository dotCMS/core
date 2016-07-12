package com.dotcms.contenttype.model.type;

import java.util.ArrayList;
import java.util.List;

import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;

@Value.Immutable
public abstract class FileAssetContentType extends ContentType implements UrlMapable{



	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentTypes baseType() {
		return  BaseContentTypes.FILEASSET;
	}


	public  List<Field> requiredFields(){
		List<Field> fields = new ArrayList<Field>();

		return ImmutableList.copyOf(fields);
	}
	
	public abstract static class Builder implements ContentTypeBuilder {}

}
