package com.dotcms.contenttype.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.JsonHelper;
import com.dotcms.contenttype.transform.SerialWrapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.liferay.portal.model.User;

public class ContentTypeImportExportUtil {


    // how many contenttypes at one time
    final int batch = 100;
    ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    ContentTypeAPI tapi = APILocator.getContentTypeAPI(APILocator.systemUser(), true);
    FieldAPI fapi = APILocator.getContentTypeFieldAPI();
    final int limit = 1000;
    public static final String CONTENT_TYPE_FILE_EXTENSION="-contenttypes.json";

    public void exportContentTypes(File directory) throws IOException, DotDataException {

        File parent = (directory.isDirectory()) ? directory : directory.getParentFile();
        int count = tapi.count();
        int runs  =count / limit;
        for (int i = 0; i <= count / limit; i++) {
            File file = new File(parent, "dotCMSContentTypes-" + i + CONTENT_TYPE_FILE_EXTENSION);
            streamingJsonExport(file, i);
        }

    }

    public void importContentTypes(File fileOrDirectory) throws IOException, DotDataException {

        if(!fileOrDirectory.isDirectory()){
            streamingJsonImport(fileOrDirectory);
        }else{
            String[] files =fileOrDirectory.list(new FilenameFilter() {
                
                @Override
                public boolean accept(File dir, String name) {
                    return (name.endsWith(CONTENT_TYPE_FILE_EXTENSION));
                }
            });

            for (String fileStr : files) {
                File file = new File(fileOrDirectory,fileStr);
                streamingJsonImport(file);
            }
        }

    }

    private void streamingJsonExport(File file, int run) throws DotDataException, IOException {
        
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            JsonGenerator jg = mapper.getJsonFactory().createGenerator(out, JsonEncoding.UTF8);
            jg.writeStartArray();
            for (int i = 0; i < 1000; i++) {
                int offset = limit * i;
                List<ContentType> exporting = tapi.search(null, "mod_date", limit, offset);
                for (ContentType type : exporting) {
                    mapper.writeValue(jg, new SerialWrapper<>(type, type.getClass()));

                    for (Field field : type.fields()) {
                        mapper.writeValue(jg, new SerialWrapper<>(field, field.getClass()));

                        for (FieldVariable var : field.fieldVariables()) {
                            mapper.writeValue(jg, new SerialWrapper<>(var, var.getClass()));

                        }
                    }
                }
            }
            jg.writeEndArray();
            jg.flush();
        }
    }


    private void streamingJsonImport(File file) throws DotDataException, IOException {
        Object obj = null;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {


            JsonFactory jsonFactory = new JsonFactory();
            JsonParser parser = jsonFactory.createJsonParser(in);
            User user = APILocator.systemUser();
            JsonToken token = parser.nextToken();
            StringBuilder sb = new StringBuilder(token.asString());

            while ((token = parser.nextToken()) != JsonToken.END_OBJECT && token != JsonToken.END_ARRAY) {
                TreeNode node = mapper.readTree(parser);
                String json = node.toString();

                Class clazz = JsonHelper.resolveClass(json);
                obj = mapper.readValue(json, clazz);
                if (obj instanceof ContentType) {
                    tapi.save((ContentType) obj);
                } else if (obj instanceof Field) {
                    fapi.save((Field) obj, user);
                } else {
                    fapi.save((FieldVariable) obj, user);
                }
                obj=null;
            }
        } catch (Exception e) {
            throw new DotStateException("failed importing:" + obj, e);
        }
    }

    /*
     * 
     * JsonGenerator jg = mapper.getJsonFactory().createGenerator(out, JsonEncoding.UTF8);
     * 
     * for (int i = 0; i < 1000; i++) { int offset = limit * i; List<ContentType> exporting =
     * APILocator.getContentTypeAPI2().find(null, "mod_date", limit, offset, "desc",
     * APILocator.systemUser(), false); for (ContentType type : exporting) { mapper.writeValue(jg,
     * new SerialWrapper<>(type, type.getClass())); for (Field field : type.fields()) {
     * mapper.writeValue(jg, new SerialWrapper<>(field, field.getClass())); for (FieldVariable var :
     * field.fieldVariables()) { mapper.writeValue(jg, new SerialWrapper<>(var, var.getClass())); }
     * } } }
     */



}
