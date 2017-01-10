/**
 * 
 */
package com.dotmarketing.viewtools.content;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.apache.commons.lang.builder.ToStringBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
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
	private static final long serialVersionUID = -5505628597857997014L;

	private String uri;

	/**
	 * Create a new instance of FileMap using a IFileAsset
	 * @param file
	 * @return FileMap new instance
	 * @throws Exception
	 */
    public static FileMap of(IFileAsset file) throws Exception {
        FileMap fm = new FileMap();
        BeanUtils.copyProperties(fm, file);

        return fm;
    }

	/**
	 * Use to get the URL to hit the dotASSET servlet
	 * @return
	 */
	public String getUri(){
		uri = InodeUtils.isSet(this.getIdentifier()) ? UtilMethods.espaceForVelocity("/contentAsset/raw-data/" + this.getIdentifier() + "/fileAsset") : "";
		return uri;
	}
	
	public String toString() {
		getUri();
		return  ToStringBuilder.reflectionToString(this);
	}
	
}
