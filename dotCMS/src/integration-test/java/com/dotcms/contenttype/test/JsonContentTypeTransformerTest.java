package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.Test;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotmarketing.util.Logger;

public class JsonContentTypeTransformerTest extends ContentTypeBaseTest {

	@Test
	public void testContentTypeSerialization() throws Exception {
		List<ContentType> types = contentTypeFactory.findAll();
		for (ContentType type : types) {
			ContentType type2 = null;
			String json = null;
			try {
				json = new JsonContentTypeTransformer(type).jsonObject().toString();
				type2 = new JsonContentTypeTransformer(json).from();

				assertThat("ContentType == ContentType2", type.equals(type2));
			} catch (Throwable t) {
				System.out.println(type);
				System.out.println(type2);
				System.out.println(json);
				throw t;
			}
		}
	}

	@Test
	public void testContentTypeArraySerialization() throws Exception {
		List<ContentType> types = contentTypeFactory.findAll();

		List<ContentType> types2 = null;
		String json = null;
		try {
			json = new JsonContentTypeTransformer(types).jsonArray().toString();
			types2 = new JsonContentTypeTransformer(json).asList();

			for (int i = 0; i < types.size(); i++) {
				assertThat("ContentType == ContentType2", types.get(i).equals(types2.get(i)));
			}
		} catch (Throwable t) {
			System.out.println(types);
			System.out.println(types2);
			System.out.println(json);
			throw t;
		}
	}

	@Test
	public void testFieldSerialization() throws Exception {
		List<ContentType> types = contentTypeFactory.findAll();
		for (ContentType type : types) {
			Field field2 = null;
			String json = null;
			for (Field field : type.fields()) {

				try {
					json = new JsonFieldTransformer(field).jsonObject().toString();

					field2 = new JsonFieldTransformer(json).from();


					assertThat("Field1 == json Field2", field.equals(field2));
				} catch (Throwable t) {
					Logger.error(this.getClass(), "Asdasdsa", t);
					System.out.println(json);
					System.out.println(field);
					System.out.println(field2);
					System.out.println(t);
					throw t;
				}
			}
		}
	}

	@Test
	public void testFieldArraySerialization() throws Exception {
		List<ContentType> types = contentTypeFactory.findAll();

		List<Field> fields = null;
		List<Field> fields2 = null;
		String json = null;
		try {
			for (ContentType type : types) {
				fields = type.fields();
				json = new JsonFieldTransformer(type.fields()).jsonArray().toString();
				fields2 = new JsonFieldTransformer(json).asList();

				for (int i = 0; i < type.fields().size(); i++) {
					assertThat("Field1 == Field2", fields.get(i).equals(type.fields().get(i)));
				}
				for (int i = 0; i < type.fields().size(); i++) {
					assertThat("Field1 == Field2", fields2.get(i).equals(type.fields().get(i)));
				}
			}
		} catch (Throwable t) {
			System.out.println(fields);
			System.out.println(json);
			throw t;
		}
	}
}
