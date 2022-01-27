package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.system.CategoryFieldType;
import com.dotmarketing.portlets.categories.model.Category;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableCategoryField.class)
@JsonDeserialize(as = ImmutableCategoryField.class)
@Value.Immutable
public abstract class CategoryField extends Field {

	private static final long serialVersionUID = 1L;

	


  @Override
	public Class type() {
		return CategoryField.class;
	}

	@Value.Default
	@Override
	public boolean indexed() {
		return true;
	};
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	}
	@Override
	public final List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.SYSTEM);
	}
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
				ContentTypeFieldProperties.CATEGORIES, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.SEARCHABLE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value) {

		if (value instanceof List<?>) {
			final List<?> list = (List<?>) value;
			if (!list.isEmpty()) {
				//This might get a collection of Categories
				if (list.get(0) instanceof Category) {
					final List<Category> categories = (List<Category>) list;
					return Optional.of(CategoryFieldType.builder()
							.value(categories.stream().map(Category::getCategoryVelocityVarName).collect(
									Collectors.toList())));
				}
				//Or a collection of Strings which would contain the Category-Name
				return Optional.of(CategoryFieldType.builder().value((List<String>) value));
			}
		}

		return Optional.empty();
	}

}
