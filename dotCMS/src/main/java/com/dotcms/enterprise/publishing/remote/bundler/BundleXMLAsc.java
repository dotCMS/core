package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.io.FileFilter;
import java.security.MessageDigest;

import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotmarketing.util.Logger;

public class BundleXMLAsc implements IBundler {

    
    @Override
    public String getName() {
        return "Bundle XML asc";
    }

    @Override
    public void setConfig(PublisherConfig pc) {
    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }


    @Override
    public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException {
        /*
        File bundleXMLasc = new File(bundleRoot,"bundle.xml.asc");
        File bundleXML = new File(bundleRoot,"bundle.xml");
        try {
            byte[] data=FileUtils.readFileToByteArray(bundleXML);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5=md.digest(data);
           // FileUtils.writeByteArrayToFile(bundleXMLasc, LicenseManager.getInstance().createBundleXMLasc(md5));
        }
        catch(Exception ex) {
            throw new DotBundleException(ex.getMessage(), ex);
        }
        */
        Logger.debug(this.getClass(), "No longer Implemented");
    }

    @Override
    public FileFilter getFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().equals("bundle.xml.asc");
            }
        };
    }

}
