package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockServletPathRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.mock.response.MockHttpCaptureResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.servlets.BinaryExporterServlet;
import com.dotmarketing.servlets.ShortyServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.FileUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;





/**
 * This class is a bundler for images and files that are statically displayed on
 * HTMLPages and URLMaps, including images that have been run through the resize
 * filter
 * @author will
 *
 */
public class ShortyBundler implements IBundler {



    String SHORTY_BUNDLER_PATTERN = Config.getStringProperty("SHORTY_BUNDLER_PATTERN","\\/dA\\/[a-zA-Z0-9]{8,}[^\"' ]*");

    ShortyServlet shortyServlet = new ShortyServlet();
    BinaryExporterServlet binaryServlet = new BinaryExporterServlet();


    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void setConfig(PublisherConfig pc) {

    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }

    @Override
    public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException {




        // Build List of ContentAssets
        Map<String, BinFileExportStruc> bins = new HashMap<String, ShortyBundler.BinFileExportStruc>();
        try {
            // fire up our binary servlet
            this.shortyServlet.init();
            this.binaryServlet.init();
            // list the files
            List<File> files = FileUtil.listFilesRecursively(bundleRoot, getFileFilter());

            for(File f : files){
                if(f.isDirectory()){
                    continue;
                }
                else if(f.getAbsolutePath().endsWith(URLMapBundler.FILE_ASSET_EXTENSION)){
                    try{
                        bins.putAll(processURLMapPage(f, bundleRoot));
                    }
                    catch(Exception e){
                      Logger.error(this.getClass(), e.getMessage(),e);
                    }
                }
                else if(f.getAbsolutePath().endsWith(HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION)){
                  try{
                      bins.putAll(processHTMLPage(f, bundleRoot));
                  }
                  catch(Exception e){
                    Logger.error(this.getClass(), e.getMessage(),e);
                  }
              }
            }
        } catch (Exception e) {
            throw new DotBundleException(e.getMessage(), e);
        }



        for(String url : bins.keySet()){
            BinFileExportStruc binFile = bins.get(url);
            try {
                writeBinFile(binFile);
            } catch (Exception e) {
                Logger.error(ShortyBundler.class,e.getMessage(),e);
            } 
            finally{
                try {
                    HibernateUtil.closeSession();
                } catch (DotHibernateException e) {

                }
            }

        }




    }

    /**
     * This method reads in the content of HTML pages and
     * checks to see if they have any matching URLs.  If so,
     * the method will return the match
     * @param file
     * @param bundleRoot
     * @return
     * @throws DotPublishingException
     */
    private Map<String, BinFileExportStruc>  processURLMapPage(File file, File bundleRoot) throws DotPublishingException {

        Map<String, BinFileExportStruc> bins = new HashMap<String, ShortyBundler.BinFileExportStruc>();


        String docId = null;
        Set<String> binaryUrls = new HashSet<String>();
        try {
            URLMapWrapper wrap = (URLMapWrapper) BundlerUtil.xmlToObject(file);
            if (wrap == null){
                return bins;
            }
            boolean live = (wrap.getContent().getInode().equals(wrap.getInfo().getLiveInode() )) ;
            if(!live) return bins;
            File urlMapFile = new File(file.getAbsolutePath().replaceAll(URLMapBundler.FILE_ASSET_EXTENSION, ""));
            if(!urlMapFile.exists()) return bins;
            Host h = APILocator.getHostAPI().find(wrap.getId().getHostId(), APILocator.getUserAPI().getSystemUser(), true);



            String line = null;
            BufferedReader fs = new BufferedReader(new FileReader(urlMapFile));
            while(( line = fs.readLine()) != null){
                binaryUrls.addAll(parseLine(line));
            }
            fs.close();

            for(String url : binaryUrls){

                String staticFile = bundleRoot.getPath() + File.separator
                        + (live ? "live" : "working") + File.separator
                        + h.getHostname() + File.separator + wrap.getContent().getLanguageId()
                        + url.replace("/", File.separator);
                try {
                  Host host= APILocator.getHostAPI().find(wrap.getId().getHostId(), APILocator.getUserAPI().getSystemUser(), false);
                  

                    bins.put(h.getIdentifier()+url,new BinFileExportStruc(new File(staticFile), url, String.valueOf(wrap.getContent().getLanguageId()), host));



                } catch (Exception e) {
                    Logger.error(ShortyBundler.class,e.getMessage(),e);
                }
                finally{
                    HibernateUtil.closeSession();
                }

            }



        } catch (Exception e) {
            Logger.error(this.getClass(), "site search  failed: " + docId + " error" + e.getMessage(),e);
        }
        finally{
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.debug(ShortyBundler.class,e.getMessage(),e);
            }
        }
        return bins;
    }


    /**
     * This method finds all the HTML pages that h
     * @param file
     * @param bundleRoot
     * @return
     * @throws DotPublishingException
     */
    private  Map<String, BinFileExportStruc>   processHTMLPage(File file, File bundleRoot) throws DotPublishingException {
        String docId = null;
        Map<String, BinFileExportStruc> bins = new HashMap<String, ShortyBundler.BinFileExportStruc>();

        try {
            HTMLPageAsContentWrapper wrap = (HTMLPageAsContentWrapper) BundlerUtil.xmlToObject(file);
            
            Host host= APILocator.getHostAPI().find(wrap.getId().getHostId(), APILocator.getUserAPI().getSystemUser(), false);
            
            
            if (wrap == null){
                return bins;
            }
            boolean live = (wrap.getAsset().getInode().equals(wrap.getInfo().getLiveInode() )) ;
            if(!live) return bins;
            File htmlFile = new File(file.getAbsolutePath().replaceAll(HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION, ""));
            if(!htmlFile.exists()) return bins;
            Host h = APILocator.getHostAPI().find(wrap.getId().getHostId(), APILocator.getUserAPI().getSystemUser(), true);

            Set<String> binaryUrls = new HashSet<String>();

            String line = null;
            BufferedReader fs = new BufferedReader(new FileReader(htmlFile));
            while(( line = fs.readLine()) != null){
                binaryUrls.addAll(parseLine(line));
            }
            fs.close();

            for(String url : binaryUrls){

                String staticFile = bundleRoot.getPath() + File.separator
                        + (live ? "live" : "working") + File.separator
                        + h.getHostname() + File.separator + wrap.getAsset().getLanguageId()
                        + url.replaceAll("/", File.separator);
                try {

                    bins.put(h.getIdentifier() + url + String.valueOf(wrap.getAsset().getLanguageId()),new BinFileExportStruc(new File(staticFile), url, String.valueOf(wrap.getAsset().getLanguageId()), host));
                } catch (Exception e) {
                    Logger.error(ShortyBundler.class,"error writing binary file:" + e.getMessage(),e);
                }
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), "site search  failed: " + docId + " error" + e.getMessage(),e);
        }
        finally{
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.debug(ShortyBundler.class,e.getMessage(),e);
            }
        }
        return bins;

    }




    @Override
    public FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File ff) {
                return (ff.isDirectory()  || ff.getName().endsWith(HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION)  || ff.getName().endsWith(URLMapBundler.FILE_ASSET_EXTENSION));
            }
        };
    }


    /**
     * this method takes a URI as a String from the dotCMS and then writes the output to
     * the file specified on the destPath
     * @param uri
     * @param destPath
     * @throws IOException
     * @throws ServletException
     * @throws DotSecurityException 
     * @throws DotDataException 
     */
    private void writeBinFile(BinFileExportStruc binFile) throws IOException, ServletException, DotDataException, DotSecurityException{

        if(binFile.getUri() ==null || binFile.getBinFile() ==null || binFile.getBinFile().exists() ){
            return;
        }
        Host h = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);

        String uri = binFile.getUri().split("\\?")[0];



        // write the dir for the new file
        File dir = new File(binFile.getBinFile().getAbsolutePath().substring(0, binFile.getBinFile().getAbsolutePath().lastIndexOf(File.separator)));
        dir.mkdirs();
        dir.mkdir();

        
        HttpServletRequest request = new MockHttpRequest(binFile.host.getHostname(), binFile.uri).request();
        
        MockAttributeRequest mock = new MockAttributeRequest(request);
        mock.setAttribute(WebKeys.HTMLPAGE_LANGUAGE,binFile.getLanguage());
        mock.setAttribute(WebKeys.CURRENT_HOST,binFile.host);

        request = mock.request();
        
        
        
        
        HttpServletResponse response = new MockHttpResponse(new BaseResponse().response());
       
        this.shortyServlet.service(request, response);


        String path  = (String) request.getAttribute(ShortyServlet.SHORTY_SERVLET_FORWARD_PATH);
        request = new MockHttpRequest(binFile.host.getHostname(), path).request();
        request = new MockServletPathRequest(request, "/contentAsset").request();
        
        
        
        
        response = new MockHttpCaptureResponse(response, binFile.binFile).response();
        

        this.binaryServlet.doGet(request, response);
        
        
    }

    private List<String> parseLine(String line){
        List<String> ret = new ArrayList<String>();



        List<RegExMatch> l = RegEX.find(line, SHORTY_BUNDLER_PATTERN);


        for(RegExMatch rem : l){

            Logger.debug(this.getClass(), "*** * **line:" + line);
            Logger.info(this.getClass(), "*** * found :" + rem.getMatch());
            ret.add(rem.getMatch());
        }
        return ret;

    }





    private  Map<String, String[]> getUrlParameters(String uri) throws UnsupportedEncodingException {
        Map<String, String[]> params = new HashMap<String, String[]>();
        for (String param : uri.split("&")) {
            String pair[] = param.split("=");
            String key = URLDecoder.decode(pair[0], "UTF-8");
            String[] value = {""};
            if (pair.length > 1) {
                value = new String[]{URLDecoder.decode(pair[1], "UTF-8")};
            }
            params.put(new String(key), value);
        }
        return params;
    }





    private class BinFileExportStruc{
         final File binFile;
         final String uri;
         final String language;
         final Host host;



        public BinFileExportStruc(File binFile, String uri, String language, Host host) {
            this.binFile = binFile;
            this.uri = uri;
            this.language = language;
            this.host = host;
        }
        public File getBinFile() {
            return binFile;
        }

        public String getUri() {
            return uri;
        }

        public String getLanguage() {
            return language;
        }



    }





}