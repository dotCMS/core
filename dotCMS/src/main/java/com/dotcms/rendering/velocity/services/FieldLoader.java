package com.dotcms.rendering.velocity.services;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

/**
 * @author Jason Tesser
 * @since 1.6.5
 *
 */
public class FieldLoader implements DotLoader {

	public static final String FIELD_CONSTANT="FIELD_CONSTANT";
	public InputStream buildVelocity(String fieldInode, String contentInode, PageMode mode,String filePath) throws DotDataException, DotSecurityException {

		Field field = APILocator.getContentTypeFieldAPI().find(fieldInode);
		if(!UtilMethods.isSet(field)){
			Logger.warn(this.getClass(),"Field not found.  Unable to load velocity code");
			return new ByteArrayInputStream("".toString().getBytes());
		}

		String contFieldValue = "";
		
		
		
		if(field instanceof ConstantField || field instanceof HiddenField){
			contFieldValue = field.values() == null ? "" : field.values();
		}else{
	        ContentletAPI conAPI = APILocator.getContentletAPI();
	        Contentlet content = conAPI.find(contentInode, APILocator.getUserAPI().getSystemUser(), true);
	        if(!UtilMethods.isSet(content)){
	            Logger.warn(this.getClass(),"Content not found.  Unable to load velocity code");
	            return new ByteArrayInputStream("".getBytes());
	        }
	        Object contFieldValueObject = conAPI.getFieldValue(content, field);
			contFieldValue = contFieldValueObject == null ? "" : contFieldValueObject.toString();
		}
		
		if(contFieldValue != null && contFieldValue.endsWith("#")){
			contFieldValue = contFieldValue.substring(0, contFieldValue.length()-1);
			contFieldValue += "$esc.h";
		}
		

        return writeOutVelocity(filePath, contFieldValue);


	}
	

    @Override
    public InputStream writeObject(final VelocityResourceKey key) throws DotDataException, DotSecurityException {
            return this.buildVelocity(key.id2, key.id1, key.mode, key.path);      
    }
    
    public void invalidate(Field field, Optional<Contentlet> con) {
      for(PageMode mode : PageMode.values()) {
        invalidate(field, con, mode);
      }
    }

    public void invalidate(Field field) {
      Optional<Contentlet> con = Optional.empty();
      this.invalidate(field, con);
    }


    public void invalidate(Object obj, Optional<Contentlet> con, PageMode mode) {
      VelocityResourceKey key = new VelocityResourceKey((Field) obj, con, mode);
      CacheLocator.getVeloctyResourceCache().remove(key );
    }

    @Override
    public void invalidate(Object obj, PageMode mode) {
      this.invalidate(obj,Optional.empty(), mode);
      
    }
}
