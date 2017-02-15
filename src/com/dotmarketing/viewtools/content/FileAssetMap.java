package com.dotmarketing.viewtools.content;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.apache.commons.lang.builder.ToStringBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;

public class FileAssetMap extends FileAsset {
    private static final long serialVersionUID = -3798679965316360641L;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getUri() throws DotDataException {
        return super.getURI();
    }

    /**
     * Create a new instance of FileAssetMap using a contentlet
     * @param c
     * @return FileAssetMap new instance
     * @throws Exception
     */
    public static FileAssetMap of(Contentlet c) throws Exception {
        FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(c);

        FileAssetMap fam = new FileAssetMap();
        fam.setHost(fa.getHost());
        fam.setBinary(FileAssetAPI.BINARY_FIELD, fa.getFileAsset());
        BeanUtils.copyProperties(fam, fa);

        return fam;
    }
    
    
    
    
    public String getShortyUrl() {

        if (getFileAsset() != null
            && getFileAsset().exists()
            && getFileAsset().getName() != null) {

            String shorty = APILocator.getShortyAPI().shortify(getIdentifier());
            return "/dA/"+shorty+"/" + getFileAsset().getName();
        } else {
            return null;
        }
    }

    public String getShorty() {

        return APILocator.getShortyAPI().shortify(getIdentifier());
    }
    
    public String getShortyUrlInode() {

        if (getFileAsset() != null
            && getFileAsset().exists()
            && getFileAsset().getName() != null) {

            String shorty = APILocator.getShortyAPI().shortify(getInode());
            return "/dA/"+shorty+"/" + getFileAsset().getName();
        } else {
            return null;
        }
    }
    
}
