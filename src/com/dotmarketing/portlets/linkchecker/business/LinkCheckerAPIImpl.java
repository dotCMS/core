package com.dotmarketing.portlets.linkchecker.business;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.linkchecker.util.ProxyManager;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.UtilMethods;

public class LinkCheckerAPIImpl implements LinkCheckerAPI {
    
    private static String ANCHOR = "a";  
    private static String HREF = "href";
    private static String TITLE = "title";
    private static String HTTPS = "https";
    private static String HTTP = "http";
    private static String PARAGRAPH = "#";
    
    private LinkCheckerFactory linkFactory=FactoryLocator.getLinkCheckerFactory(); 
    
    @SuppressWarnings("deprecation")
    private static void loadProxy(HttpClient client){
        if(ProxyManager.INSTANCE.isLoaded()){
            if(ProxyManager.INSTANCE.getConnection().isProxy()){
                client.getHostConfiguration().setProxy(ProxyManager.INSTANCE.getConnection().getProxyHost(), ProxyManager.INSTANCE.getConnection().getProxyPort());
                if(ProxyManager.INSTANCE.getConnection().isProxyRequiredAuth()){
                    HttpState state = new HttpState();
                    state.setProxyCredentials(null, null,
                            new UsernamePasswordCredentials(ProxyManager.INSTANCE.getConnection().getProxyUsername(), ProxyManager.INSTANCE.getConnection().getProxyPassword()));
                    client.setState(state);
                }
            }
        }       
    }
    
    public static URL getURLByString(String href){
        try {
            java.net.URL url = new java.net.URL(href);
            URL urlBean = new URL();
            if(url.getProtocol().equals(HTTPS))
                urlBean.setHttps(true);
            else
                urlBean.setHttps(false);
            urlBean.setHostname(url.getHost());         
            urlBean.setPort(url.getPort()<0?80:url.getPort());
            urlBean.setPath(url.getPath());
            if(url.getQuery()!=null){
                urlBean.setWithParameter(true);
                String[] query_string = null;
                if(url.getQuery().split("[&amp;]").length>0)
                    query_string = url.getQuery().split("[&amp;]");
                else
                    query_string = url.getQuery().split("[&]");
                NameValuePair[] params = new NameValuePair[query_string.length];
                for(int i=0; i<query_string.length; i++){
                    String[] parametro_arr = query_string[i].split("[=]");
                    params[i] = new NameValuePair(parametro_arr[0], parametro_arr[1]);
                }
                urlBean.setQueryString(params);             
            }
            return urlBean;
        } catch (MalformedURLException e) {
            return null;
        }
    }
    
    @Override
    public List<InvalidLink> findInvalidLinks(String htmltext) throws DotDataException, DotSecurityException {
        List<Anchor> anchorList = new ArrayList<Anchor>();
        Document doc = Jsoup.parse(htmltext);
        Elements links = doc.select(ANCHOR);
        for(Element link:links){
            String href = link.attr(HREF);
            Anchor a = new Anchor();
            if(href.startsWith(HTTP) || href.startsWith(HTTPS)){ //external link                
                a.setExternalLink(getURLByString(href));
                a.setTitle(link.attr(TITLE));
                a.setInternalLink(null);
                a.setInternal(false);   
                anchorList.add(a);
            }else if(!(href.startsWith(PARAGRAPH))){ //internal link                
                a.setExternalLink(null);
                a.setTitle(link.attr(TITLE));
                if(href.indexOf('?')>0)
                    a.setInternalLink(href.substring(0,href.indexOf('?')));
                else
                    a.setInternalLink(href);
                a.setInternal(true);
                anchorList.add(a);
            }
            
        }
        List<Host> hosts=APILocator.getHostAPI().findAll(APILocator.getUserAPI().getSystemUser(), false);
        List<InvalidLink> result = new ArrayList<InvalidLink>();
        for(Anchor a : anchorList){
            if(a.getExternalLink()!=null && (!a.isInternal())) { //external link
                HttpClient client = new HttpClient();
                loadProxy(client);
                HttpMethod method = new GetMethod(a.getExternalLink().absoluteURL());
                if(a.getExternalLink().isWithParameter())
                    method.setQueryString(a.getExternalLink().getQueryString());
                int statusCode = -1;
                try{
                    statusCode = client.executeMethod(method);
                } catch(Exception e){ }
                
                if(statusCode!=200){
                    InvalidLink c = new InvalidLink();
                    c.setUrl(a.getExternalLink().absoluteURL());
                    c.setStatusCode(statusCode);
                    c.setTitle(a.getTitle());
                    result.add(c);
                }
            }else {  //internal link.
                boolean found=false;
                if(!CMSFilter.excludeURI(a.getInternalLink())) {
                    for(Host h : hosts){
                        Identifier id = APILocator.getIdentifierAPI().find(h, a.getInternalLink());
                        if(id!=null && UtilMethods.isSet(id.getId())) {
                            found = true; break;
                        }
                    }
                    if(!found) {
                        InvalidLink c = new InvalidLink();
                        c.setUrl(a.getInternalLink());
                        c.setTitle(a.getTitle());
                        result.add(c);
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    public void saveInvalidLinks(Contentlet contentlet, Field field, List<InvalidLink> links) throws DotDataException, DotSecurityException {
        linkFactory.save(contentlet.getInode(), field.getInode(), links);
    }
    
    public void deleteInvalidLinks(Contentlet contentlet) throws DotDataException, DotSecurityException {
        linkFactory.deleteByInode(contentlet.getInode());
    }
    
    public List<InvalidLink> findByInode(String inode) throws DotDataException {
        return linkFactory.findByInode(inode);
    }
    
    public List<InvalidLink> findAll(int offset, int pageSize) throws DotDataException {
        return linkFactory.findAll(offset, pageSize);
    }
    
    public int findAllCount() throws DotDataException {
        return linkFactory.findAllCount();
    }
    
    protected static class Anchor {
        
        private URL externalLink;
        private String title;
        private String internalLink;
        private boolean isInternal;
        
        public URL getExternalLink() {
            return externalLink;
        }
        
        public void setExternalLink(URL href) {
            this.externalLink = href;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }

        public String getInternalLink() {
            return internalLink;
        }

        public void setInternalLink(String internalLink) {
            this.internalLink = internalLink;
        }

        public boolean isInternal() {
            return isInternal;
        }

        public void setInternal(boolean isInternal) {
            this.isInternal = isInternal;
        }
        
    }
    
    protected static class URL {
        
        private String hostname;
        private Integer port;
        private boolean https;
        private String path;
        private boolean withParameter;
        private NameValuePair[] queryString;
        
        public String getHostname() {
            return hostname;
        }
        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
        public Integer getPort() {
            return port;
        }
        public void setPort(Integer port) {
            this.port = port;
        }
        public boolean isHttps() {
            return https;
        }
        public void setHttps(boolean https) {
            this.https = https;
        }
        public String getPath() {
            return path;
        }
        public void setPath(String path) {
            this.path = path;
        }
        public boolean isWithParameter() {
            return withParameter;
        }
        public void setWithParameter(boolean withParameter) {
            this.withParameter = withParameter;
        }
        public NameValuePair[] getQueryString() {
            return queryString;
        }
        public void setQueryString(NameValuePair[] queryString) {
            this.queryString = queryString;
        }   
        
        public String completeURL(){
            StringBuilder sb = new StringBuilder(500);
            sb.append(https?"https://":"http://");
            sb.append(hostname);
            sb.append(port!=80?":"+port:"");
            sb.append(path);
            if(withParameter){
                sb.append("?");
                for(int i=0; i<queryString.length; i++){
                    sb.append(queryString[i].getName());
                    sb.append("=");
                    sb.append(queryString[i].getValue());
                    if(queryString.length-i>1)
                        sb.append("&");
                }
            }
            return sb.toString();
        }
        
        public String absoluteURL(){
            StringBuilder sb = new StringBuilder(500);
            sb.append(https?"https://":"http://");
            sb.append(hostname);
            sb.append(port!=80?":"+port:"");
            sb.append(path);
            return sb.toString();
        }
    }

}
