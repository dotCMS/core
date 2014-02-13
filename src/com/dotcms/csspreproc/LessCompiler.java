package com.dotcms.csspreproc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import com.dotcms.repackage.guava_11_0_1.com.google.common.base.Joiner;
import com.dotcms.repackage.lesscss_1_5_1_snapshot.org.lesscss.LessSource;
import com.dotcms.repackage.lesscss_1_5_1_snapshot.org.lesscss.Resource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

public class LessCompiler extends CSSCompiler {

    public LessCompiler(Host host, String uri, boolean live) {
        super(host, uri, live);
    }

    @Override
    public void compile() throws DotSecurityException, DotStateException, DotDataException, IOException {
        com.dotcms.repackage.lesscss_1_5_1_snapshot.org.lesscss.LessCompiler compiler = new com.dotcms.repackage.lesscss_1_5_1_snapshot.org.lesscss.LessCompiler();
        compiler.setCompress(inputLive);
        try {
            // replace the extension .css with .scss
            String lessUri = inputURI.substring(0, inputURI.lastIndexOf('.')) + ".less";
            
            Identifier ident = APILocator.getIdentifierAPI().find(inputHost, lessUri);
            if(ident!=null && InodeUtils.isSet(ident.getId())) {
                LessSource source = new LessSource(new FileAssetResource(ident,inputLive));
                output = compiler.compile(source).getBytes();
            }
            else {
                throw new IOException("file: "+inputHost.getHostname()+":"+lessUri+" not found");
            }
        }
        catch(Exception ex) {
            Logger.error(this, "couldn't compile less source for "+inputHost.getHostname()+":"+inputURI);
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected String addExtensionIfNeeded(String url) {
        return url.endsWith(".less") ? url : url+".less";
    }

    class FileAssetResource implements Resource {
        protected Identifier identifier;
        protected boolean live;
        
        public FileAssetResource(Identifier identifier, boolean live) {
            this.identifier = identifier;
            this.live = live;
        }
        
        @Override
        public Resource createRelative(String path) throws IOException {
            try {
                LinkedList<String> parts = new LinkedList<String>(Arrays.asList(identifier.getParentPath().split("/")));
                for(String p : path.split("/")) {
                    if(p.equals("..") && !parts.isEmpty()) parts.removeLast();
                    else if(p.length()>0 && !p.equals(".")) parts.addLast(p);
                }
                String absPath = Joiner.on("/").join(parts);
                
                Identifier ident = APILocator.getIdentifierAPI().find(
                        APILocator.getHostAPI().find(identifier.getHostId(),APILocator.getUserAPI().getSystemUser(),false), absPath);
                if(ident!=null && InodeUtils.isSet(ident.getId())) {
                    allImportedURI.add(absPath);
                }
                return new FileAssetResource(ident,live);
            }
            catch(Exception ex) {
                Logger.warn(this, "couldn't get relative asset path: "+path+" from ident: "+identifier.getId()+" live? "+live);
                return new FileAssetResource(new Identifier(), live);
            }
        }

        @Override
        public boolean exists() {
            try {
                return identifier!=null && InodeUtils.isSet(identifier.getId()) && getFileAsset()!=null;
            }
            catch(Exception ex) {
                Logger.warn(this, "call to exists() failed. Ident "+identifier.getId()+" live? "+live,ex);
                return false;
            }
        }
        
        protected FileAsset getFileAsset() {
            try {
                Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(identifier.getId(), live, 
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(),false);
                return cont!=null && InodeUtils.isSet(cont.getInode()) ? 
                        APILocator.getFileAssetAPI().fromContentlet(cont) : null;
            }
            catch(Exception ex) {
                Logger.warn(this, "couldn't get file asset with identifier: "+identifier.getId()+" live? "+live,ex);
                return null;
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            FileAsset file = getFileAsset();
            return file!=null ? file.getFileInputStream() : new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public String getName() {
            return identifier.getAssetName();
        }

        @Override
        public long lastModified() {
            FileAsset file = getFileAsset();
            return file!=null ? file.getModDate().getTime() : new Date().getTime();
        }
        
    }

    @Override
    protected String getDefaultExtension() {
        return "less";
    }
}
