package com.dotcms.util.deserializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.junit.Test;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.json.JSONException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ImmutableTypeAdapterTest extends UnitTestBase {
	
    @Test
    public void test() throws ParseException, JSONException {

    	final GsonBuilder gsonBuilder = new GsonBuilder();

        // Immutables.io: Type Adapter registration
    	final ImmutableTypeAdapter typeAdapter = new ImmutableTypeAdapter<>(gsonBuilder);
        gsonBuilder.registerTypeAdapter(ContentType.class, typeAdapter);
        gsonBuilder.registerTypeAdapter(ImmutableSimpleContentType.class, typeAdapter);

    	final Gson gson = gsonBuilder.create();


    	// Dummy Content-Type
		ContentType initialContentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
				.description("description1").folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
				.name("ContentTypeDefault1").owner("owner").variable("velocityVarNameDefault1").build();

		assertTrue(initialContentType instanceof ImmutableSimpleContentType);


		// Test Serialization
    	String json = gson.toJson(initialContentType);

        assertNotNull(json);


        // Test Deserialization
        ContentType deserializedContentType = gson.fromJson(json, ContentType.class);

		assertTrue(deserializedContentType instanceof ImmutableSimpleContentType);

		assertEquals("description2", deserializedContentType.description());
		assertEquals(FolderAPI.SYSTEM_FOLDER, deserializedContentType.folder());
		assertEquals(Host.SYSTEM_HOST, deserializedContentType.host());
		assertEquals("ContentTypeDefault2", deserializedContentType.name());
		assertEquals("owner", deserializedContentType.owner());
		assertEquals("velocityVarNameDefault2", deserializedContentType.variable());
    }
}
