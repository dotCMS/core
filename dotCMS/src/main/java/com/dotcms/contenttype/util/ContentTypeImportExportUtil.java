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
import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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

    public void exportContentTypes(File directory) throws IOException, DotDataException, DotSecurityException {

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

    private void streamingJsonExport(File file, int run) throws DotDataException, DotSecurityException, IOException {
        
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            JsonGenerator jg = mapper.getJsonFactory().createGenerator(out, JsonEncoding.UTF8);
            jg.writeStartArray();
            for (int i = 0; i < 1000; i++) {
                int offset = limit * i;
                List<ContentType> exporting = tapi.search(null, "mod_date", limit, offset);
                for (ContentType contentType : exporting) {

            		List<Field> fields = contentType.fields();

            		List<FieldVariable> fieldVariables=new ArrayList<FieldVariable>();
                    for(Field ff : fields) {
                        fieldVariables.addAll(ff.fieldVariables());
                    }

                	mapper.writeValue(jg, new ContentTypeWrapper(contentType,fields,fieldVariables));
                }
            }
            jg.writeEndArray();
            jg.flush();
        }
    }


    private void streamingJsonImport(File file) throws DotDataException, IOException {
    	ContentTypeWrapper contentTypeWrapper = null;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {


            JsonFactory jsonFactory = new JsonFactory();
            JsonParser parser = jsonFactory.createJsonParser(in);
            JsonToken token = parser.nextToken();

            if (token == JsonToken.START_ARRAY) {
                while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
                	contentTypeWrapper = mapper.readValue(parser, ContentTypeWrapper.class);

    	        	ContentType contentType = contentTypeWrapper.getContentType();
    	        	List<Field> fields = contentTypeWrapper.getFields();
    	        	List<FieldVariable> fieldVariables = contentTypeWrapper.getFieldVariables();

        	    	contentType.constructWithFields(fields);

        	    	tapi.save(contentType);

                    for(FieldVariable fieldVariable : fieldVariables) {
                    	fapi.save(fieldVariable, APILocator.systemUser());
                    }

                    contentTypeWrapper = null;
                }
            }
        } catch (Exception e) {
            throw new DotStateException("failed importing:" + contentTypeWrapper, e);
        }
    }

    public static class ContentTypeWrapper {
    	private ContentType contentType;
    	private List<Field> fields;
    	private List<FieldVariable> fieldVariables;
    	
        public ContentTypeWrapper() {}
    	
    	public ContentTypeWrapper(ContentType contentType, List<Field> fields, List<FieldVariable> variables) {
    		this.contentType = contentType;
    		this.fields = fields;
    		this.fieldVariables = variables;
    	}

        public ContentType getContentType() {
    		return contentType;
    	}
    	public void setContentType(ContentType contentType) {
    		this.contentType = contentType;
    	}

    	public List<Field> getFields() {
    		return fields;
    	}
    	public void setFields(List<Field> fields) {
    		this.fields = fields;
    	}
    	
    	public List<FieldVariable> getFieldVariables() {
            return fieldVariables;
        }
        public void setFieldVariables(List<FieldVariable> fieldVariables) {
            this.fieldVariables = fieldVariables;
        }
   }
}
