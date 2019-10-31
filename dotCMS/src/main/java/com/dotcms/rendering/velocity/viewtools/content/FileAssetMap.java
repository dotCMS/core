package com.dotcms.rendering.velocity.viewtools.content;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import org.apache.commons.lang.builder.ToStringBuilder;

public class FileAssetMap extends FileAsset {
    private static final long serialVersionUID = -3798679965316360641L;

    private static final String URL_MASK = "/dA/%s/%s";

    /**
     * Default constructor
     */
    public FileAssetMap() {
    }

    /**
     * copy constructor
     * @param contentlet
     * @throws Exception
     */
    public FileAssetMap(final FileAsset contentlet) throws Exception {
        super(contentlet);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getUri() throws DotDataException {
        return super.getURI();
    }

    /**
     * Create a new instance of FileAssetMap using a contentlet
     * @param contentlet
     * @return FileAssetMap new instance
     * @throws Exception
     */
    public static FileAssetMap of(final Contentlet contentlet) throws Exception {
        final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
        return APILocator.getFileAssetAPI().fromFileAsset(fileAsset);
    }

    public String getShortyUrl() {

        if (getFileAsset() != null) {
            String shorty = APILocator.getShortyAPI().shortify(getIdentifier());
            return String.format(URL_MASK, shorty, getFileAsset().getName());
        } else {
            return null;
        }
    }

    public String getShorty() {

        return APILocator.getShortyAPI().shortify(getIdentifier());
    }
    
    public String getShortyUrlInode() {

        if (getFileAsset() != null) {
            String shorty = APILocator.getShortyAPI().shortify(getInode());
            return String.format(URL_MASK, shorty, getFileAsset().getName());
        } else {
            return null;
        }
    }
    
}
