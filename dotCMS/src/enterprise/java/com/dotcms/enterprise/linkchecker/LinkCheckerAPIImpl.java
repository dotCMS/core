/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.linkchecker;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpClient;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpMethod;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpState;
import com.dotcms.repackage.org.apache.commons.httpclient.NameValuePair;
import com.dotcms.repackage.org.apache.commons.httpclient.UsernamePasswordCredentials;
import com.dotcms.repackage.org.apache.commons.httpclient.methods.GetMethod;
import com.dotcms.repackage.org.apache.commons.httpclient.params.HttpClientParams;
import com.dotcms.repackage.org.apache.commons.httpclient.params.HttpConnectionParams;
import com.dotcms.repackage.org.jsoup.Jsoup;
import com.dotcms.repackage.org.jsoup.nodes.Document;
import com.dotcms.repackage.org.jsoup.nodes.Element;
import com.dotcms.repackage.org.jsoup.select.Elements;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPI;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerFactory;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class LinkCheckerAPIImpl implements LinkCheckerAPI {

    private static final String QUESTION = "?";
//    private static final String MAILTO = "mailto:";
    private static final String ANCHOR = "a";  
    private static final String HREF = "href";
    private static final String TITLE = "title";
    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private static final String PARAGRAPH = "#";
    private static final int DEFAULT_PORT = 80;
    private static final int DEFAULT_TIMEOUT = 2000;
    private static final String UNTITLED = "Untitled";
    private static final String BROKEN_LINKS_UNTITLED = "BROKEN_LINKS_UNTITLED";

    private final LinkCheckerFactory linkFactory = FactoryLocator.getLinkCheckerFactory();
    
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
    
    public static URL getURLByString(final String href) {

        try {

            final java.net.URL url = new java.net.URL(href);
            final URL urlBean = new URL();

            urlBean.setHttps(HTTPS.equals(url.getProtocol()));
            urlBean.setHostname(url.getHost());
            urlBean.setPort(url.getPort()<0? DEFAULT_PORT :url.getPort());
            urlBean.setPath(url.getPath());

            if(url.getQuery()!=null) {

                urlBean.setWithParameter(true);

                if (url.getQuery().contains("&")) {

                    urlBean.setRawQueryString(url.getQuery());
                } else {

                    setUrlBeanQueryString(url, urlBean);
                }
            }

            return urlBean;
        } catch (MalformedURLException e) {
            return null;
        }
    } // getURLByString.

    private static void setUrlBeanQueryString(final java.net.URL url,
                                              final LinkCheckerAPIImpl.URL urlBean) {

        final String[] query_string =
                (url.getQuery().split("[&amp;]").length > 0)?
                        url.getQuery().split("[&amp;]"):
                        url.getQuery().split("[&]");

        final NameValuePair[] params =
                new NameValuePair[query_string.length];

        for (int i = 0; i < query_string.length; i++) {

            String[] parametro_arr = query_string[i].split("[=]");
            if (parametro_arr.length == 2) {

                params[i] = new NameValuePair
                        (parametro_arr[0], parametro_arr[1]);
            } else {
                params[i] = new NameValuePair
                        (parametro_arr[0], StringPool.BLANK);
            }
        }

        urlBean.setQueryString(params);
    } // setUrlBeanQueryString.

    @CloseDBIfOpened
    @Override
    public List<InvalidLink> findInvalidLinks(final Contentlet contentlet) throws DotDataException, DotSecurityException {

        final List<InvalidLink> result = new ArrayList<>();

        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level) {

            return result;
        }
        
        final User user = APILocator.getUserAPI().loadUserById(contentlet.getModUser());
        for(final Field field : FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode())) {

            if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {

                final String htmlText = contentlet.getStringProperty(field.getVelocityVarName());
                if(UtilMethods.isSet(htmlText)) {

                	final List<Anchor> anchorList = new ArrayList<>();
                    final Document document       = Jsoup.parse(htmlText);
                    final Elements links          = document.select(ANCHOR);

                    this.collectAnchors(anchorList, links);

                    for(final Anchor anchor : anchorList) {

                        if(anchor.getExternalLink() != null && (!anchor.isInternal())) {
                            //external link
                            checkExternalLink(result, user, field, anchor);
                        } else {
                            //internal link.
                            checkInternalLink(contentlet, result, user, field, anchor);
                        }
                    }
                }
            }
        }
        return result;
    }

    private void collectAnchors(final List<Anchor> anchorList, final Elements links) {

        for(final Element link : links) {

            final String href   = link.attr(HREF);
            final Anchor anchor = new Anchor();

            if(href.startsWith(HTTP) || href.startsWith(HTTPS)) { //external link

                anchor.setExternalLink(getURLByString(href));
                anchor.setTitle(link.attr(TITLE));
                anchor.setInternalLink(null);
                anchor.setInternal(false);
                anchorList.add(anchor);
            } else if(!(href.startsWith(PARAGRAPH)) &&
                      !href.startsWith(QUESTION) &&
                      !(RegEX.contains(href, "^\\w+\\:"))) { //internal link

                anchor.setExternalLink(null);
                anchor.setTitle(link.attr(TITLE));
                if(href.indexOf('?')>0) {
                    anchor.setInternalLink(href.substring(0, href.indexOf('?')));
                } else if(href.indexOf('#')>0) {
                    anchor.setInternalLink(href.substring(0, href.indexOf('#')));
                } else {
                    anchor.setInternalLink(href);
                }
                anchor.setInternal(true);
                anchorList.add(anchor);
            }
        }
    }

    private void checkInternalLink(final Contentlet contentlet,
                                   final List<InvalidLink> invalidLinks,
                                   final User user,
                                   final Field field,
                                   final LinkCheckerAPIImpl.Anchor anchor) throws DotDataException, DotSecurityException {

        // loook for urlmap
        String testurl = anchor.getInternalLink();
        final HostAPI hostAPI = APILocator.getHostAPI();
        final Host contentletHost = hostAPI
                .find(contentlet.getHost(), user, false);

        final Host host = (null != contentletHost && contentletHost.isSystemHost())?
                hostAPI.findDefaultHost(user, false):contentletHost;

        if(!anchor.getInternalLink().startsWith("/")){
            final Identifier id = APILocator.getIdentifierAPI()
                    .find(contentlet.getIdentifier());
            testurl = id.getPath() + anchor.getInternalLink();
        }

        final CMSUrlUtil cmsUrlUtils = CMSUrlUtil.getInstance();
        final long languageId        = contentlet.getLanguageId();
        // tests
        if(isUrlMap                    (testurl)                   ||
                cmsUrlUtils.isFileAsset(testurl, host, languageId) ||
                cmsUrlUtils.isFolder   (testurl, host)             ||
                cmsUrlUtils.isPageAsset(testurl, host, languageId) ||
                cmsUrlUtils.isVanityUrl(testurl, host, languageId)) {
            return;
        }

        final InvalidLink invalidLink = new InvalidLink();
        invalidLink.setUrl(anchor.getInternalLink());
        invalidLink.setTitle(anchor.getTitle());
        invalidLink.setField(field.getInode());

        if(!UtilMethods.isSet(invalidLink.getTitle())) {

            try {

                invalidLink.setTitle(LanguageUtil.get(user, BROKEN_LINKS_UNTITLED));
            } catch (LanguageException e) {
                invalidLink.setTitle(UNTITLED);
            }
        }

        invalidLinks.add(invalidLink);
    } // checkInternalLink.

    private void checkExternalLink(final List<InvalidLink> invalidLinks,
                                  final User user,
                                  final Field field,
                                  final LinkCheckerAPIImpl.Anchor anchor) {

        final HttpConnectionParams params =
                new HttpConnectionParams();

        params.setConnectionTimeout(DEFAULT_TIMEOUT);

        final HttpClient client = new HttpClient(new HttpClientParams(params));
        loadProxy(client);

        final String url = anchor.getExternalLink().absoluteURL();
        final HttpMethod method = new GetMethod(url);

        if(anchor.getExternalLink().isWithParameter()) {

            if (null != anchor.getExternalLink().getRawQueryString()) {
                method.setQueryString(anchor.getExternalLink().getRawQueryString());
            } else {
                method.setQueryString(anchor.getExternalLink().getQueryString());
            }
        }

        int statusCode = -1;

        try{
            statusCode = client.executeMethod(method);
        } catch(Exception e) { }

        if(statusCode < 200 || statusCode >= 400){

            final InvalidLink invalidLink = new InvalidLink();
            invalidLink.setUrl(anchor.getExternalLink().absoluteURL());
            invalidLink.setStatusCode(statusCode);
            invalidLink.setTitle(anchor.getTitle());
            invalidLink.setField(field.getInode());
            if(!UtilMethods.isSet(invalidLink.getTitle())) {
                try {
                    invalidLink.setTitle(LanguageUtil.get(user, BROKEN_LINKS_UNTITLED));
                } catch (LanguageException e) {
                    invalidLink.setTitle(UNTITLED);
                }
            }

            invalidLinks.add(invalidLink);
        }
    } // checkExternalLink.

    @WrapInTransaction
    @Override
    public void saveInvalidLinks(Contentlet contentlet,List<InvalidLink> links) throws DotDataException, DotSecurityException {
        if(LicenseUtil.getLevel()< LicenseLevel.STANDARD.level)
            return;
        linkFactory.save(contentlet.getInode(), links);
    }

    @CloseDBIfOpened
    @Override
    public void deleteInvalidLinks(Contentlet contentlet) throws DotDataException, DotSecurityException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return;
        linkFactory.deleteByInode(contentlet.getInode());
    }


    @CloseDBIfOpened
    @Override
    public List<InvalidLink> findByInode(String inode) throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return new ArrayList<>();
        return linkFactory.findByInode(inode);
    }

    @CloseDBIfOpened
    @Override
    public List<InvalidLink> findAll(int offset, int pageSize) throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return new ArrayList<>();
        return linkFactory.findAll(offset, pageSize);
    }

    @CloseDBIfOpened
    @Override
    public List<InvalidLink> findAllByStructure(String structureInode, int offset, int pageSize) throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return new ArrayList<>();
        return linkFactory.findAllByStructure(structureInode, offset, pageSize);    	
    }

    @CloseDBIfOpened
    @Override
    public int findAllCount() throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return 0;
        return linkFactory.findAllCount();
    }

    @CloseDBIfOpened
    @Override
    public int findAllByStructureCount(String structureInode) throws DotDataException {
        if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            return 0;
        return linkFactory.findAllByStructureCount(structureInode);    	
    }
    
    private boolean isUrlMap(String uri) throws DotDataException, DotSecurityException{
    	String testurl = uri;
    	List<SimpleStructureURLMap> urlMaps = StructureFactory.findStructureURLMapPatterns();
    	
    	if(!uri.endsWith("/"))
            testurl= uri + "/";
        for (SimpleStructureURLMap urlMap : urlMaps) {
            String regEx = StructureUtil.generateRegExForURLMap(urlMap.getURLMapPattern());
            List<RegExMatch> matches = RegEX.findForUrlMap(testurl, regEx);
            if (matches != null && matches.size() > 0) {
                List<RegExMatch> valueGroups = matches.get(0).getGroups();
                List<RegExMatch> find = RegEX.find(urlMap.getURLMapPattern(),"\\{(.+)\\}");
                if(find.size()>0) {
                    List<RegExMatch> fieldGroups = find.get(0).getGroups();
                    if(valueGroups.size()==fieldGroups.size()) {
                        StringBuilder sb=new StringBuilder();
                        String structureVar=CacheLocator.getContentTypeCache().getStructureByInode(urlMap.getInode()).getVelocityVarName();
                        sb.append("+structureName:")
                          .append(structureVar)
                          .append(' ');
                        for(int x=0;x<valueGroups.size();x++) {
                            sb.append("+").append(structureVar)
                                             .append('.')
                                             .append(fieldGroups.get(x).getMatch())
                                          .append(":")
                                          .append(valueGroups.get(x).getMatch())
                                          .append(' ');
                        }
                        if(APILocator.getContentletAPI().indexCount(
                             sb.toString(), APILocator.getUserAPI().getSystemUser(), false)>0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
        private String rawQueryString;

        public String getRawQueryString() {
            return rawQueryString;
        }

        public void setRawQueryString(String rawQueryString) {
            this.rawQueryString = rawQueryString;
        }

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
                sb.append(QUESTION);
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
