package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableHostFolderField.class)
@JsonDeserialize(as = ImmutableHostFolderField.class)
@Value.Immutable
public abstract class HostFolderField extends Field implements OnePerContentType {


	@Value.Default
	@Override
	public boolean indexed() {
		return true;
	};

	private static final long serialVersionUID = 1L;

	@Override
	public  Class type() {
		return  HostFolderField.class;
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	};
	@Override
	public final List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.SYSTEM, DataTypes.TEXT);
	}
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.LABEL, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.VALUE, ContentTypeFieldProperties.TEXT_AREA_VALUES,
				ContentTypeFieldProperties.DISPLAY_TYPE, ContentTypeFieldProperties.DEFAULT_TEXT,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.USER_SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED, ContentTypeFieldProperties.DATA_TYPE, ContentTypeFieldProperties.RADIO_TEXT,
				ContentTypeFieldProperties.RADIO_BOOL, ContentTypeFieldProperties.RADIO_DECIMAL,
				ContentTypeFieldProperties.RADIO_NUMBER);
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Host-Folder";
	}
}
