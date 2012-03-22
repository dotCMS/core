/**
 * 
 */
package com.dotmarketing.viewtools.content;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;

/**
 * A wrapper to help provide methods on the front-end of Velocity 
 * @author Jason Tesser
 * @since 1.9.1.3
 * 
 */
public class FileMap extends File {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5505628597857997014L;

	private String uri;
	
	/**
	 * Use to get the URL to hit the dotASSET servlet
	 * @return
	 */
	public String getUri(){
		uri = InodeUtils.isSet(this.getIdentifier()) ? UtilMethods.espaceForVelocity("/dotAsset/" + this.getIdentifier() + "." + this.getExtension()) : "";
		return uri;
	}
	
	public String toString() {
		getUri();
		return  ToStringBuilder.reflectionToString(this);
	}
	
}
