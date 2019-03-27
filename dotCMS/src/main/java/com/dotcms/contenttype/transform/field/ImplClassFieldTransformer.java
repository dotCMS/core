package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;


public class ImplClassFieldTransformer implements FieldTransformer {

  final List<Field> genericFields;

  public ImplClassFieldTransformer(Field f) {
    this.genericFields = ImmutableList.of(f);
  }

  public ImplClassFieldTransformer(final List<Field> fields) {
    this.genericFields = ImmutableList.copyOf(fields);
  }

  @Override
  public Field from() throws DotStateException {
    if (this.genericFields.size() == 0)
      throw new DotStateException("0 results");
    Field field = impleClass(this.genericFields.get(0));
    return field;

  }

  private static Field impleClass(final Field genericField) {
    FieldBuilder builder = null;
    try {
      builder = FieldBuilder.builder(genericField);
      return builder.build();
    } catch (Exception e) {
      throw new DotStateException(e.getMessage(), e);
    }
  }

  @Override
  public List<Field> asList() throws DotStateException {

    List<Field> list = new ArrayList<Field>();
    for (Field field : this.genericFields) {
      list.add(impleClass(field));
    }

    return ImmutableList.copyOf(list);

  }
}
