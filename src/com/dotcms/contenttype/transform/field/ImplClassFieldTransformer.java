package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotmarketing.business.DotStateException;
import com.google.common.collect.ImmutableList;

public class ImplClassFieldTransformer implements FieldTransformer{
	
	final List<Field> genericFields ;
	
	public ImplClassFieldTransformer(Field f){
		this.genericFields=ImmutableList.of(f);
	}
	
	public ImplClassFieldTransformer(final List<Field> fields){
		this.genericFields=ImmutableList.copyOf(fields);
	}
	
	@Override
	public Field from() throws DotStateException {
		if(this.genericFields.size()==0) throw new DotStateException("0 results");
		return  impleClass(this.genericFields.get(0));


	}
	
	private static Field impleClass(final Field genericField){
		try {
			FieldBuilder builder = FieldBuilder.builder(genericField);
			return builder.from(genericField).build();
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
