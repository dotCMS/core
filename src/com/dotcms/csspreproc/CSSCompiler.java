package com.dotcms.csspreproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.FileUtil;

abstract class CSSCompiler {
    protected final List<String> allImportedURI=new ArrayList<String>();
    protected byte[] output;
    protected final String inputURI;
    protected final Host inputHost;
    protected final boolean inputLive;
    
    public CSSCompiler(Host host, String uri, boolean live) {
        inputHost = host;
        inputURI = uri;
        inputLive = live;
    }
    
    public abstract void compile() throws DotSecurityException, DotStateException, DotDataException, IOException;
    
    protected List<String> getImportedUris(File file) throws IOException {
        List<String> imported=new ArrayList<String>();
        
        // get the pattern
        BufferedReader patternReader=new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("importpattern.txt")));
        String pattern=patternReader.readLine();
        patternReader.close();
        
        // collect the file names
        Pattern cssImport=Pattern.compile(pattern);
        BufferedReader bin=new BufferedReader(new FileReader(file));
        String line;
        while((line=bin.readLine())!=null) {
            Matcher matcher = cssImport.matcher(line);
            while(matcher.find()) {
                imported.add(matcher.group("filename"));
            }
        }
        
        bin.close();
        return imported;
    }
    
    protected List<String> filterImportedUris(String baseUri, List<String> uris) {
        List<String> filtered = new ArrayList<String>(uris.size());
        List<String> baseParts=new ArrayList<String>(Arrays.<String>asList(baseUri.split("/")));
        baseParts.remove(baseParts.size()-1);
        
        for(String uri : uris) {
            if(uri.startsWith("/")) {
                filtered.add(uri);
            }
            else {
                // relative path processing
                LinkedList<String> xparts=new LinkedList<String>(baseParts);
                for(String part : uri.split("/")) {
                    if(part.equals("..") && !baseParts.isEmpty())
                        xparts.removeLast(); // back to parent directory
                    else if(!part.equals(".") && part.length()>0) 
                        xparts.addLast(part);// follow relative path
                }
                StringBuilder uriStr=new StringBuilder();
                for(String part : xparts) {
                    if(part.length()>0) {
                        uriStr.append("/").append(part);
                    }
                }
                if(uriStr.length()>0) {
                    filtered.add(uriStr.toString());
                }
            }
        }
        
        return filtered;
    }
    
    protected abstract String addExtensionIfNeeded(String url);
    protected abstract String getDefaultExtension();
    
    protected File createCompileDir(Host currentHost, String uri, boolean live) throws IOException, DotStateException, DotDataException, DotSecurityException {
        File compDir = new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() 
                + File.separator + "css_compile_space"
                + File.separator + UUIDGenerator.generateUuid());
        compDir.mkdirs();
        LinkedList<String> uriq = new LinkedList<String>();
        uriq.add(uri);
        while(!uriq.isEmpty()) {
            String u = addExtensionIfNeeded(uriq.removeFirst());
            String ucopy = u;
            // check if it is a path to other host
            Host host;
            if(u.startsWith("//")) {
                String hostName = u.substring(2, u.indexOf('/', 2));
                host = APILocator.getHostAPI().findByName(hostName, APILocator.getUserAPI().getSystemUser(), false);
                u = u.substring(u.indexOf('/',2)+1);
            }
            else {
                host = currentHost;
            }   
            
            Identifier ident = APILocator.getIdentifierAPI().find(host, u);
            if(ident!=null && InodeUtils.isSet(ident.getId())) {
                Contentlet file = APILocator.getContentletAPI().findContentletByIdentifier(ident.getId(), live, 
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
                if(file!=null && InodeUtils.isSet(file.getInode())) {
                    File ff = file.getBinary(FileAssetAPI.BINARY_FIELD);
                    if(ff!=null && ff.isFile()) {
                        
                        // destfile: /../../../tmpdir/host/path/to/asset.extension
                        File dest = new File(compDir.getAbsolutePath() + File.separator + 
                                host.getHostname() + u);
                        dest.getParentFile().mkdirs();
                        FileUtil.copyFile(ff, dest);
                        
                        if(!ucopy.equals(uri))
                            allImportedURI.add(ucopy);
                        
                        uriq.addAll(
                                filterImportedUris(uri,
                                        getImportedUris(ff)));
                    }
                }
            }
        }
        
        return compDir;
    }
}
