package com.dotcms.rendering.velocity.viewtools.content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.util.Constants;
import org.apache.commons.lang.builder.ToStringBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.image.focalpoint.FocalPoint;
import com.dotmarketing.image.focalpoint.FocalPointAPIImpl;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.Lazy;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;

import javax.servlet.http.HttpServletRequest;

/**
 * A helper class to provide an object to return to the dotCMS when a content has a binary field
 * @since 1.9.1.3
 * @author Jason Tesser
 *
 */
public class BinaryMap {

	private final Contentlet content;
	private final Field field;

	private final boolean includeLanguageInLink;
	
	public BinaryMap(Contentlet content, Field field, Context context) {
		this.content = content;
		this.field = field;
		// don't include language in the URL if the request has the request attribute User-Agent set for PP
		HttpServletRequest request = null;
		if (context instanceof ViewContext) {
			final ViewContext viewContext = (ViewContext) context;
			request = viewContext.getRequest();
		}
		if (request == null) {
			request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		}
		if (request != null) {
			final String userAgent = (String) request.getAttribute("User-Agent");
			this.includeLanguageInLink = !(UtilMethods.isSet(userAgent)
					&& userAgent.equalsIgnoreCase(Constants.USER_AGENT_DOTCMS_PUSH_PUBLISH));
		} else {
			this.includeLanguageInLink = true;
		}
	}
	
    public BinaryMap(Contentlet content,
					 com.dotmarketing.portlets.structure.model.Field field, Context context) {
        this(content, new LegacyFieldTransformer(field).from(), context);
    }
    
    Lazy<Metadata> meta = Lazy.of(this::getMetadata);
    
    
    private Metadata getMetadata() {
        try {
            return content.getBinaryMetadata(field.variable());
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "Unable to retrive binary file for content id " + content.getIdentifier()
                            + " field " + field.variable(), e);
            throw new DotRuntimeException(e);
        }

    }
	
	
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).
	       append("name", getName()).
	       append("size", getSize()).
	       append("rawUri", getRawUri()).
	       append("resizeUri", getResizeUri()).
	       append("thumbnailUri", getThumbnailUri()).
	       append("file", getFile()).
	       toString();
	}

	/**
	 * The size of the actual raw file
	 * @return the size of the file
	 */
	public String getSize() {
		if(meta.get() != null) {
			return UtilMethods.prettyByteify(meta.get().getLength());
		}
		return null;
	}

	/**
	 * The name of the file
	 * @return the name
	 */
	public String getName() {
	    if(meta.get() != null) {
			return meta.get().getName();
		}
		return "";
	}
    /**
     * The name of the file
     * @return the name
     */
    public Object get(String key) {

        return getMeta().get(key);

    }
    
    /**
     * The name of the file
     * @return the name
     */
    public Map<String,Serializable> getMeta() {
        if(meta.get() != null) {
            return meta.get().getMap();
        }
        return Map.of();
    }
    
	/**
	 * The rawURI is link to the actual full image
	 * @return the rawUri
	 */
    public String getRawUri() {
        return getName().length() > 0
            ? UtilMethods.espaceForVelocity("/dA/" + content.getIdentifier() + "/"
				+ field.variable() + "/" + getName()
				+ (includeLanguageInLink ? "?language_id=" + content.getLanguageId() : ""))
            : null;
    }

    public String getShortyUrl() {

        if(meta.get() != null) {
            return "/dA/"+getShorty()+"/"+field.variable()+"/" + getName()
					+ (includeLanguageInLink ? "?language_id=" + content.getLanguageId() : "");
        } else {
	        return null;
        }
    }

    public String getShorty() {

        return APILocator.getShortyAPI().shortify(content.getIdentifier());
    }
	
    public String getShortyUrlInode() {

        if(meta.get() != null) {
            String shorty = APILocator.getShortyAPI().shortify(content.getInode());
            return "/dA/"+shorty+"/"+field.variable()+"/" + getName();
        } else {
            return null;
        }
    }
	
	/**
	 * This is a convince method/helper method to build the URI for the resize exporter.  
	 * You may pass any of the parameters as null, or 0 
	 * <br/>
	 * Either width or height are required, if one of those is not passed then the image will be resized proportionally to preserve its aspect ratio,  
	 * otherwise both parameters will be used the image could look stretched.
	 * @return the resizeUri
	 */
	public String getResizeUri() {
	    if(getName().length()==0) return null;
	    final String imageId =  UtilMethods.isSet(content.getIdentifier()) ? content.getIdentifier() : content.getInode();
		return "/dA/"+imageId+"/"+field.variable()+"/"; 

	}
	
	/**
	 * Either width or height are required, if one of those is not passed then the image will be resized proportionally to preserve its aspect ratio,  
	 * otherwise both parameters will be used the image could look stretched.
	 * 
	 * @param width Width to resize to
	 * @param height Height to resize to
	 */
	public String getResizeUri(Integer width, Integer height){
	    if(getName().length()==0) return "";
	    StringBuilder uri=new StringBuilder();
	    uri.append(getResizeUri());
	    if(width!=null && width>0) {
			uri.append(width).append("w");
		}
		if(width!=null && width>0 && height!=null && height>0) {
			uri.append("/");
		}
	    if(height!=null && height>0) {
			uri.append(height).append("h");
		}
	    return uri.toString();
	}
	
	/**
	 * Either w (width) or h (height) are required, if one of those is not passed then the image will be resized proportionally to preserve its aspect ratio, and the
	 * background color parameter will not be used
	 * If both width and height are passed, then the background color parameter could be used to preserver the aspect ratio and generate bands to respect
	 * the passed width and height, if no background color is passed then white will be used by default.
	 */
	public String getThumbnailUri() {
	    if(getName().length()==0) return "";
	    
	    final String imageId =  UtilMethods.isSet(content.getIdentifier()) ? content.getIdentifier() : content.getInode();
        return "/contentAsset/image/"+imageId+"/"+field.variable()+"/filter/Thumbnail"; 

	}

	/**
	 * This is a convince method/helper method to build the URI for the thumbnail exporter.  
	 * You may pass any of the parameters as null, empty or 0 in the case of ints.
	 * <br/>
	 * Either w (width) or h (height) are required, if one of those is not passed then the image will be resized proportionally to preserve its aspect ratio, and the
	 * background color parameter will not be used
	 * If both width and height are passed, then the background color parameter could be used to preserver the aspect ratio and generate bands to respect
	 * the passed width and height, if no background color is passed then white will be used by default.
	 * 
	 * @param width Width to resize to
	 * @param height Height to resize to
	 * @param background RGB so ie.. WHITE is 255255255
	 * @return
	 */
	public String getThumbnailUri(Integer width, Integer height, String background){
	    if(getName().length()==0) {
	        return "";
	    }
        StringBuilder uri=new StringBuilder();
        uri.append(getThumbnailUri());
        if(width!=null && width>0)
            uri.append("/thumbnail_w/").append(width);
        if(height!=null && height>0)
            uri.append("/thumbnail_h/").append(height);
        if(background!=null)
            uri.append("/thumbnail_bg/").append(background);
        return uri.toString();
	}
	
	/**
	 * This is the underneath Java File.  Be careful when working with this object as you can manipulate it.
	 * @return the file
	 */
	@JsonIgnore
	public File getFile() {
		return Sneaky.sneak(()->content.getBinary(field.variable()));
	}

    public int getHeight() {
        if(meta.get() ==null || !meta.get().isImage()) {
            return 0;
        }
        if (meta.get().getHeight() > 0) {
            return meta.get().getHeight();
        }

        return ImageFilterAPI.apiInstance.apply().getWidthHeight(getFile()).height;
    }
    
    public float getFpx() {
        if(meta.get() ==null || !meta.get().isImage()) {
            return 0;
        }
        Optional<FocalPoint> optPoint = new FocalPointAPIImpl().readFocalPoint(content.getInode(), field.variable());
        
        if(optPoint.isEmpty()) {
            return 0;
        }
        return optPoint.get().x;
    }
    
    public float getFpy() {
        if(meta.get() ==null || !meta.get().isImage()) {
            return 0;
        }
        Optional<FocalPoint> optPoint = new FocalPointAPIImpl().readFocalPoint(content.getInode(), field.variable());
        
        if(optPoint.isEmpty()) {
            return 0;
        }
        return optPoint.get().y;
    }
    
    public String getFocalPoint() {
        return getFpx() + "," + getFpy();
    }
    
    @SuppressWarnings("java:S1845")
    public String getFocalpoint() {
        return getFocalPoint();
    }
    
    
    public int getWidth() {
        if(meta.get() ==null || !meta.get().isImage()) {
            return 0;
        }
        if (meta.get().getWidth() > 0) {
            return meta.get().getWidth();
        }

        return ImageFilterAPI.apiInstance.apply().getWidthHeight(getFile()).width;
        

    }

}