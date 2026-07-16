/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockServletPathRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.mock.response.MockHttpCaptureResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publishing.*;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.FileCreationException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.servlets.BinaryExporterServlet;
import com.dotmarketing.servlets.ShortyServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.WebKeys;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
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

    private PublisherConfig config;

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void setConfig(PublisherConfig config) {
        this.config = config;
    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }

    @Override
    public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {

        if (this.config.getOperation() == Operation.UNPUBLISH) {
            return;
        }

        // Build List of ContentAssets
        Map<String, BinFileExportStruc> bins = new HashMap<>();
        try {
            // fire up our binary servlet
            this.shortyServlet.init();
            this.binaryServlet.init();
            // list the files
            Collection<File> files = output.getFiles(getFileFilter());

            for(File f : files){
                if(f.isDirectory()){
                    continue;
                }
                else if(f.getAbsolutePath().endsWith(URLMapBundler.FILE_ASSET_EXTENSION)){
                    try{
                        bins.putAll(processURLMapPage(f, output));
                    }
                    catch(Exception e){
                      Logger.error(this.getClass(), e.getMessage(),e);
                    }
                }
                else if(f.getAbsolutePath().endsWith(HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION)){
                  try{
                      bins.putAll(processHTMLPage(f, output));
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
                writeBinFile(output, binFile);
            } catch (FileCreationException e) {
                Logger.warn(ShortyBundler.class, () -> e.getMessage());
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
     * @param bundleOutput
     * @return
     * @throws DotPublishingException
     */
    private Map<String, BinFileExportStruc>  processURLMapPage(final File file,
                                                               final BundleOutput bundleOutput)
            throws DotPublishingException {

        Map<String, BinFileExportStruc> bins = new HashMap<>();


        String docId = null;
        Set<String> binaryUrls = new HashSet<>();
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

                String staticFile = File.separator
                        + (live ? "live" : "working") + File.separator
                        + h.getHostname() + File.separator + wrap.getContent().getLanguageId()
                        + url.replace("/", File.separator);
                try {
                  Host host= APILocator.getHostAPI().find(wrap.getId().getHostId(), APILocator.getUserAPI().getSystemUser(), false);
                  

                    bins.put(h.getIdentifier()+url,new BinFileExportStruc(new File(staticFile), url, wrap.getContent(), host));



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
     * @param bundleOutput
     * @return
     * @throws DotPublishingException
     */
    private  Map<String, BinFileExportStruc>   processHTMLPage(
            final File file, BundleOutput bundleOutput) throws DotPublishingException {
        String docId = null;
        Map<String, BinFileExportStruc> bins = new HashMap<>();

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

            Set<String> binaryUrls = new HashSet<>();

            String line = null;
            BufferedReader fs = new BufferedReader(new FileReader(htmlFile));
            while(( line = fs.readLine()) != null){
                binaryUrls.addAll(parseLine(line));
            }
            fs.close();

            for(String url : binaryUrls){

                String staticFile = File.separator
                        + (live ? "live" : "working") + File.separator
                        + h.getHostname() + File.separator + wrap.getAsset().getLanguageId()
                        + url.replaceAll("/", File.separator);
                try {

                    bins.put(h.getIdentifier() + url + String.valueOf(wrap.getAsset().getLanguageId()),new BinFileExportStruc(new File(staticFile), url, (Contentlet) wrap.getAsset(), host));
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
     * @param bundleOutput
     * @param binFile
     * @throws IOException
     * @throws ServletException
     * @throws DotSecurityException 
     * @throws DotDataException 
     */
    private void writeBinFile(final BundleOutput bundleOutput, final BinFileExportStruc binFile)
            throws IOException, ServletException, DotDataException, DotSecurityException{

        if(binFile.getUri() ==null || binFile.getBinFile() ==null || binFile.getBinFile().exists() ){
            return;
        }
        Host h = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);

        String uri = binFile.getUri().split("\\?")[0];



        
        HttpServletRequest request = new FakeHttpRequest(binFile.host.getHostname(), binFile.uri).request();
        
        MockAttributeRequest mock = new MockAttributeRequest(request);
        mock.setAttribute(WebKeys.HTMLPAGE_LANGUAGE,binFile.getLanguage());
        mock.setAttribute(WebKeys.CURRENT_HOST,binFile.host);

        request = mock.request();
        
        
        
        
        HttpServletResponse response = new MockHttpResponse(new BaseResponse().response());
       
        this.shortyServlet.service(request, response);

        final String path  = (String) request.getAttribute(ShortyServlet.SHORTY_SERVLET_FORWARD_PATH);

        if (path != null) {
            // write the dir for the new file
            File dir = new File(binFile.getBinFile().getAbsolutePath().substring(0,
                    binFile.getBinFile().getAbsolutePath().lastIndexOf(File.separator)));
            dir.mkdirs();
            dir.mkdir();


            request = new FakeHttpRequest(binFile.host.getHostname(), path).request();
            request = new MockServletPathRequest(request, "/contentAsset").request();

            //Every Static Push call to the BinaryServlet must be executed using system user instead of anonymous
            request.setAttribute(com.liferay.portal.util.WebKeys.USER, APILocator.getUserAPI().getSystemUser());
            try (final OutputStream outputStream = bundleOutput.addFile(binFile.binFile)) {

                response = new MockHttpCaptureResponse(response, outputStream).response();

                this.binaryServlet.doGet(request, response);
            }
        } else {
            Logger.warn(ShortyBundler.class,
                    String.format(
                            "Contentlet not exists or not have LIVE version: %s, Used in: %s",
                            binFile.uri,
                            binFile.getContentlet().getIdentifier()
                    )
            );
        }
    }

    private List<String> parseLine(String line){
        List<String> ret = new ArrayList<>();



        List<RegExMatch> l = RegEX.find(line, SHORTY_BUNDLER_PATTERN);


        for(RegExMatch rem : l){

            Logger.debug(this.getClass(), "*** * **line:" + line);
            Logger.info(this.getClass(), "*** * found :" + rem.getMatch());
            ret.add(rem.getMatch());
        }
        return ret;

    }





    private  Map<String, String[]> getUrlParameters(String uri) throws UnsupportedEncodingException {
        Map<String, String[]> params = new HashMap<>();
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
         final Host host;
         final Contentlet contentlet;


        public BinFileExportStruc(final File binFile, final String uri, final Contentlet asset, final Host host) {
            this.binFile = binFile;
            this.uri = uri;
            this.contentlet = asset;
            this.host = host;
        }
        public File getBinFile() {
            return binFile;
        }

        public String getUri() {
            return uri;
        }

        public String getLanguage() {
            return String.valueOf(contentlet.getLanguageId());
        }

        public Contentlet getContentlet() {
            return contentlet;
        }
    }





}
