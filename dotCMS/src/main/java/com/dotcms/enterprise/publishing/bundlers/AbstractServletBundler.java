package com.dotcms.enterprise.publishing.bundlers;

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
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
/**
 * This class is a bundler for images and files that are statically displayed on
 * HTMLPages and URLMaps, including images that have been run through the resize
 * filter
 * @author will
 *
 */
public abstract class AbstractServletBundler implements IBundler {

	String pattern = null;
	HttpServlet servlet = null;
	HttpServletRequest request = null;
	HttpServletResponse response = null;
	private PublisherConfig config = null;
    private HostAPI hostAPI = APILocator.getHostAPI();


	protected AbstractServletBundler( String pattern, HttpServlet servlet ) {
		this.pattern = pattern;
		this.servlet = servlet;
	}


	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		this.config = pc;
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException {




		// Build List of ContentAssets
		Map<String, BinFileExportStruc> bins = new HashMap<String, AbstractServletBundler.BinFileExportStruc>();
		try {
			// fire up our binary servlet
			this.servlet.init();

			// list the files
			List<File> files = FileUtil.listFilesRecursively(bundleRoot, getFileFilter());

			for(File f : files){
				if(f.isDirectory()){
					continue;
				}else if(f.getAbsolutePath().endsWith(URLMapBundler.FILE_ASSET_EXTENSION)){
					try{
						bins.putAll(processURLMapPage(f, bundleRoot));
					}
					catch(Exception e){
						Logger.error(this.getClass(), e.getMessage(), e);
					}
				}
				else if(f.getAbsolutePath().endsWith(HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION)){
                    try{
                        bins.putAll(processHTMLPageAsContent(f, bundleRoot));
                    }
                    catch(Exception e){
                        Logger.error(this.getClass(), e.getMessage(), e);
                    }
                }
			}
		} catch (Exception e) {
			throw new DotBundleException(e.getMessage(), e);
		}







		/* begin writing the files.  To do this, we create mock
		 * requests and responses, and capture the output from
		 * our servlets.
		 */

		request = Mockito.mock(HttpServletRequest.class);
		response = Mockito.mock(HttpServletResponse.class);




		for(String url : bins.keySet()){
			BinFileExportStruc binFile = bins.get(url);
			try {
				writeBinFile(binFile);
			} catch (Exception e) {
				Logger.error(AbstractServletBundler.class,e.getMessage(), e);
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

			String line;
			BufferedReader fs = new BufferedReader(new FileReader(urlMapFile));
			while(( line = fs.readLine()) != null){
				binaryUrls.addAll(parseLine(line));
			}
			fs.close();

			for(String url : binaryUrls){

                if (hostAPI.findSystemHost().equals(h)){
                    for (Host host : config.getHosts()) {
                        if (!hostAPI.findSystemHost().equals(host)){
                            bins.putAll(getURLMapFilesByHost(bundleRoot, wrap, live, host, url));
                        }
                    }
                } else {
                    bins.putAll(getURLMapFilesByHost(bundleRoot, wrap, live, h, url));
                }
			}

		} catch (Exception e) {
			Logger.error(this.getClass(), "site search  failed: " + docId + " error" + e.getMessage(), e);
		}
		finally{
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.debug(AbstractServletBundler.class,e.getMessage(),e);
			}
		}
		return bins;
	}

    private Map<String, BinFileExportStruc> getURLMapFilesByHost(File bundleRoot, URLMapWrapper wrap, boolean live, Host h, String url)
        throws DotHibernateException {
        String staticFile = bundleRoot.getPath() + File.separator
                + (live ? "live" : "working") + File.separator
                + h.getHostname() + File.separator + wrap.getContent().getLanguageId()
                + url.replace("/", File.separator);
        try {

            Map<String, BinFileExportStruc> map = new HashMap<>();
            map.put(h.getIdentifier()+wrap.getContent().getLanguageId()+url,new BinFileExportStruc(new File(staticFile), url, h, String.valueOf(wrap.getContent().getLanguageId())));
            return map;

        } catch (Exception e) {
            Logger.error(AbstractServletBundler.class,e.getMessage(), e);
        }
        finally{
            HibernateUtil.closeSession();
        }
        return new HashMap<>();
    }

    private  Map<String, BinFileExportStruc> processHTMLPageAsContent(File file, File bundleRoot) throws DotPublishingException {
        String docId = null;
        Map<String, BinFileExportStruc> bins = new HashMap<>();

        try {
            HTMLPageAsContentWrapper wrap = (HTMLPageAsContentWrapper) BundlerUtil.xmlToObject(file);
            if (wrap == null){
                return bins;
            }
            IHTMLPage page = wrap.getAsset();

            boolean live = (page.getInode().equals(wrap.getInfo().getLiveInode() )) ;
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
                    + h.getHostname() + File.separator + page.getLanguageId()
                    + url.replaceAll("/", File.separator);
                try {

                    bins.put(h.getIdentifier()+page.getLanguageId()+url,new BinFileExportStruc(new File(staticFile), url, h, String.valueOf(page.getLanguageId())));
                } catch (Exception e) {
                    Logger.error(AbstractServletBundler.class,"error writing binary file:" + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), "site search  failed: " + docId + " error" + e.getMessage(), e);
        }
        finally{
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.debug(AbstractServletBundler.class,e.getMessage(),e);
            }
        }
        return bins;

    }

	@Override
	public FileFilter getFileFilter() {
		return new FileFilter() {
			public boolean accept(File ff) {
				return (ff.isDirectory()
                        || ff.getName().endsWith(URLMapBundler.FILE_ASSET_EXTENSION)
                        || ff.getName().endsWith(HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION));
			}
		};
	}


	/**
	 * this method takes a URI as a String from the dotCMS and then writes the output to
	 * the file specified on the destPath
	 * @param binFile
	 * @throws IOException
	 * @throws ServletException
	 */
	private void writeBinFile(BinFileExportStruc binFile) throws IOException, ServletException{

		if(binFile.getUri() ==null || binFile.getBinFile() ==null || binFile.getBinFile().exists() ){
			return;
		}


		String uri = binFile.getUri().split("\\?")[0];



		// write the dir for the new file
		File dir = new File(binFile.getBinFile().getAbsolutePath().substring(0, binFile.getBinFile().getAbsolutePath().lastIndexOf(File.separator)));
		dir.mkdirs();
		dir.mkdir();

		String servletPath = binFile.getUri().substring(0,binFile.getUri().indexOf("/",1));
		// Fake an HTTPServletRequest and response
		Mockito.when(request.getServletPath()).thenReturn(servletPath);
		Mockito.when(request.getRequestURI()).thenReturn(uri);
		Mockito.when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(binFile.getHost());
		Mockito.when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(binFile.getLanguage());
		ServletOutputStream out = new MockServletOutputStream(binFile.getBinFile());
		Mockito.when(response.getOutputStream()).thenReturn((ServletOutputStream) out);

		Mockito.when(request.getParameterMap()).thenReturn(getUrlParameters(uri));


		try {
			Method doGetMethod = this.servlet.getClass().getDeclaredMethod("doGet", HttpServletRequest.class, HttpServletResponse.class);
			doGetMethod.invoke(this.servlet, request, response);
		} catch (Exception e) {
			Logger.error(AbstractServletBundler.class, "error invoking servlet" + e.getMessage(), e);
		}
	}

	private List<String> parseLine(String line){
		List<String> ret = new ArrayList<String>();



		List<RegExMatch> l = RegEX.find(line, this.pattern);


		for(RegExMatch rem : l){

			Logger.debug(this.getClass(), "*** * **line:" + line);
			Logger.debug(this.getClass(), "*** * found :" + rem.getMatch());
			ret.add(rem.getMatch());
		}
		return ret;

	}




	/**
	 * A mock Outputstream that will write the output of
	 * a servlet to a Outputstream
	 * @author will
	 *
	 */
	private class MockServletOutputStream extends ServletOutputStream{

		public MockServletOutputStream(File destPath) throws IOException{
			fos = Files.newOutputStream(Paths.get(destPath.getAbsolutePath().split("\\?")[0]));
		}

		OutputStream fos = null;

		@Override
		public void close() throws IOException {
			super.close();
			fos.close();

		}
		@Override
		public void flush() throws IOException {
			super.flush();
			fos.flush();

		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			fos.write(b, off, len);
		}

		@Override
		public void write(int b) throws IOException {
			fos.write(b);
		}

		@Override
		public  boolean isReady() { return false; }

		@Override
		public void setWriteListener(WriteListener writeListener) {

		}
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
		private File binFile;
		private String uri;
		private Host host;
		private String language;

		public BinFileExportStruc(File binFile, String uri, Host host, String language) {
			this.binFile = binFile;
			this.uri = uri;
			this.host = host;
			this.language = language;
		}

		public File getBinFile() {
			return binFile;
		}

		public String getUri() {
			return uri;
		}

		public Host getHost() {
			return host;
		}

		public String getLanguage() {
			return language;
		}
	}





}