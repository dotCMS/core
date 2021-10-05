package com.dotcms.contenttype.model.field;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.HostFolderType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.folders.model.Folder;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Optional;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

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
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.SEARCHABLE);
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Host-Folder";
	}

	@Override
	public Optional<FieldValue<?>> fieldValue(final Object value) {
		if (value instanceof String) {
			return Optional.of(HostFolderType.of(value.toString()));
		}
		if (value instanceof Host) {
			final Host host = (Host) value;
			return Optional.of(HostFolderType.of(host.getIdentifier()));
		}

		if (value instanceof Folder) {
			final Folder folder = (Folder) value;
			return Optional.of(HostFolderType.of(folder.getIdentifier()));
		}
		return Optional.empty();
	}
}
