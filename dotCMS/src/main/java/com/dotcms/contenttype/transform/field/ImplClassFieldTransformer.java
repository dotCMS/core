package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;


public class ImplClassFieldTransformer implements FieldTransformer {

  final List<Field> genericFields;

  public ImplClassFieldTransformer(final Field f) {
    this.genericFields = ImmutableList.of(f);
  }

  public ImplClassFieldTransformer(final List<Field> fields) {
    this.genericFields = ImmutableList.copyOf(fields);
  }

  @Override
  public Field from() throws DotStateException {
    if (this.genericFields.size() == 0) {
        throw new DotStateException("There are no fields to transform!");
    }
    final Field field = impleClass(this.genericFields.get(0));
    return field;

  }

  private static Field impleClass(final Field genericField) {
    FieldBuilder builder;
    try {
      builder = FieldBuilder.builder(genericField);
      return builder.build();
    } catch (final Exception e) {
      String errorMsg;
      if (null != genericField) {
        final String fieldType = UtilMethods.isSet(genericField.typeName()) ? genericField.typeName() :
                "unknown/invalid";
        errorMsg = String.format("Unable to load data for Field '%s' of type '%s' with ID [%s]: %s", genericField
                .name(), fieldType, genericField.id(), e.getMessage());
      } else {
        errorMsg = "The specified field is null: " + e.getMessage();
      }
      Logger.error(ImplClassFieldTransformer.class, errorMsg);
      throw new DotStateException(errorMsg, e);
    }
  }

  @Override
  public List<Field> asList() throws DotStateException {

    final List<Field> list = new ArrayList<>();
    for (final Field field : this.genericFields) {
      list.add(impleClass(field));
    }

    return ImmutableList.copyOf(list);

  }
}
