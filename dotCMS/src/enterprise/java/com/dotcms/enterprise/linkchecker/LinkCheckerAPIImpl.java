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
import com.dotcms.repackage.org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import com.dotcms.repackage.org.apache.commons.httpclient.Header;
import com.dotcms.repackage.org.apache.commons.httpclient.HostConfiguration;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpClient;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpMethodRetryHandler;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpState;
import com.dotcms.repackage.org.apache.commons.httpclient.NameValuePair;
import com.dotcms.repackage.org.apache.commons.httpclient.UsernamePasswordCredentials;
import com.dotcms.repackage.org.apache.commons.httpclient.methods.GetMethod;
import com.dotcms.repackage.org.apache.commons.httpclient.params.HttpClientParams;
import com.dotcms.repackage.org.apache.commons.httpclient.params.HttpConnectionParams;
import com.dotcms.repackage.org.apache.commons.httpclient.params.HttpMethodParams;
import com.dotcms.repackage.org.apache.commons.httpclient.protocol.Protocol;
import com.dotcms.repackage.org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import com.dotcms.repackage.org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import com.dotcms.repackage.org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
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
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
    private static final int DEFAULT_READ_TIMEOUT = 10000;
    private static final int MAX_REDIRECTS = 5;
    private static final String LOCATION_HEADER = "Location";
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
        params.setSoTimeout(Config.getIntProperty("LINK_CHECKER_READ_TIMEOUT", DEFAULT_READ_TIMEOUT));

        final HttpClient client = new HttpClient(new HttpClientParams(params));
        loadProxy(client);

        final String url = anchor.getExternalLink().absoluteURL();
        String queryString = null;
        if(anchor.getExternalLink().isWithParameter()) {

            if (null != anchor.getExternalLink().getRawQueryString()) {
                queryString = anchor.getExternalLink().getRawQueryString();
            } else {
                // encodes the parameters the same way the request does
                final GetMethod encoder = new GetMethod();
                encoder.setQueryString(anchor.getExternalLink().getQueryString());
                queryString = encoder.getQueryString();
            }
        }

        final Optional<Integer> status = getExternalLinkStatus(client, url, queryString,
                LinkCheckerAPIImpl::isHostAllowed, LinkCheckerAPIImpl::isAddressAllowed);
        if (status.isEmpty()) {
            Logger.debug(LinkCheckerAPIImpl.class, "Skipping link to a host that is not allowed: " + url);
            return;
        }
        final int statusCode = status.get();

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

    /**
     * Links are only checked against hosts outside of the configured private subnets
     * ({@code REMOTE_CALL_SUBNET_BLACKLIST}), unless {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS} is
     * {@code true}, consistent with the other outgoing HTTP calls.
     */
    static boolean isHostAllowed(final String host) {
        return Config.getBooleanProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false)
                || !IPUtils.isIpPrivateSubnet(host);
    }

    /**
     * Same rule as {@link #isHostAllowed(String)} for an address the host name resolved to when
     * the connection is opened.
     */
    static boolean isAddressAllowed(final InetAddress address) {
        return Config.getBooleanProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false)
                || !IPUtils.isInternalAddress(address);
    }

    /**
     * Requests the URL and returns its HTTP status. Redirects are followed manually, up to
     * {@link #MAX_REDIRECTS}, so that every location is validated before it is requested.
     * Unless a proxy is configured, the connection is opened to an address that passed
     * {@code isAddressAllowed}, resolved when the connection is opened, so a host name that
     * resolves differently after the first check cannot lead to a disallowed address.
     *
     * @param client           The HTTP client.
     * @param url              The URL to check.
     * @param queryString      Query string of the first request, or {@code null}.
     * @param isHostAllowed    Decides whether a host may be requested.
     * @param isAddressAllowed Decides whether an address the host resolves to may be connected to.
     *
     * @return The status code (-1 if the request failed), or empty if the URL or one of its
     * redirect locations is not allowed.
     */
    static Optional<Integer> getExternalLinkStatus(final HttpClient client, final String url,
                                                   final String queryString,
                                                   final Predicate<String> isHostAllowed,
                                                   final Predicate<InetAddress> isAddressAllowed) {

        String currentUrl = url;
        String currentQueryString = queryString;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {

            final java.net.URL target;
            try {
                target = new java.net.URL(currentUrl);
            } catch (final MalformedURLException e) {
                return Optional.of(-1);
            }
            final String protocol = target.getProtocol();
            if (!(HTTP.equalsIgnoreCase(protocol) || HTTPS.equalsIgnoreCase(protocol))) {
                return Optional.empty();
            }
            if (!isResolvable(target.getHost())) {
                // a host that does not exist is a broken link
                return Optional.of(-1);
            }
            if (!isHostAllowed.test(target.getHost())) {
                return Optional.empty();
            }

            // a relative request, so that the host configuration below decides the connection
            final GetMethod method;
            try {
                method = new GetMethod(UtilMethods.isSet(target.getFile()) ? target.getFile() : "/");
            } catch (final IllegalArgumentException | IllegalStateException e) {
                return Optional.of(-1);
            }
            method.setFollowRedirects(false);
            method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, NO_RETRY_ON_DISALLOWED_ADDRESS);
            if (null != currentQueryString) {
                method.setQueryString(currentQueryString);
            }
            try {
                final int statusCode = client.executeMethod(
                        hostConfiguration(client, target, isAddressAllowed), method);
                final Header location = method.getResponseHeader(LOCATION_HEADER);
                if (statusCode < 300 || statusCode >= 400 || null == location
                        || !UtilMethods.isSet(location.getValue())) {
                    return Optional.of(statusCode);
                }
                currentUrl = new java.net.URL(target, location.getValue().trim()).toString();
                currentQueryString = null;
            } catch (final AddressNotAllowedException e) {
                return Optional.empty();
            } catch (final Exception e) {
                return Optional.of(-1);
            } finally {
                method.releaseConnection();
            }
        }
        return Optional.of(-1);
    } // getExternalLinkStatus.

    /**
     * Builds the connection settings for one request: the client's own settings (such as a
     * proxy) for the target host, with a socket factory that checks the resolved address when no
     * proxy is used. When a proxy is used, the proxy resolves the host and the factory is not
     * involved.
     */
    private static HostConfiguration hostConfiguration(final HttpClient client,
                                                       final java.net.URL target,
                                                       final Predicate<InetAddress> isAddressAllowed) {
        final HostConfiguration hostConfiguration = new HostConfiguration(client.getHostConfiguration());
        final boolean secure = HTTPS.equalsIgnoreCase(target.getProtocol());
        final Protocol protocol;
        if (null != hostConfiguration.getProxyHost()) {
            protocol = Protocol.getProtocol(secure ? HTTPS : HTTP);
        } else if (secure) {
            protocol = new Protocol(HTTPS, (ProtocolSocketFactory)
                    new SecureAddressCheckingSocketFactory(isAddressAllowed), 443);
        } else {
            protocol = new Protocol(HTTP, new AddressCheckingSocketFactory(isAddressAllowed), 80);
        }
        hostConfiguration.setHost(target.getHost(), target.getPort(), protocol);
        return hostConfiguration;
    }

    /** Keeps the default retries, except for a connection refused because of its address. */
    private static final HttpMethodRetryHandler NO_RETRY_ON_DISALLOWED_ADDRESS =
            (method, exception, executionCount) -> !(exception instanceof AddressNotAllowedException)
                    && new DefaultHttpMethodRetryHandler().retryMethod(method, exception, executionCount);

    /** Thrown when a host name resolves to an address that may not be connected to. */
    static final class AddressNotAllowedException extends IOException {
        private static final long serialVersionUID = 1L;

        AddressNotAllowedException(final String host) {
            super("Host resolves to an address that is not allowed: " + host);
        }
    }

    /**
     * Opens plain connections to an address of the host only after checking every address the
     * host resolves to, and connects to the address that was checked.
     */
    static class AddressCheckingSocketFactory implements ProtocolSocketFactory {

        private final Predicate<InetAddress> isAddressAllowed;

        AddressCheckingSocketFactory(final Predicate<InetAddress> isAddressAllowed) {
            this.isAddressAllowed = isAddressAllowed;
        }

        @Override
        public Socket createSocket(final String host, final int port) throws IOException {
            return createSocket(host, port, null, 0, null);
        }

        @Override
        public Socket createSocket(final String host, final int port, final InetAddress localAddress,
                                   final int localPort) throws IOException {
            return createSocket(host, port, localAddress, localPort, null);
        }

        @Override
        public Socket createSocket(final String host, final int port, final InetAddress localAddress,
                                   final int localPort, final HttpConnectionParams params)
                throws IOException {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (final InetAddress address : addresses) {
                if (!isAddressAllowed.test(address)) {
                    throw new AddressNotAllowedException(host);
                }
            }
            final Socket socket = new Socket();
            try {
                if (null != localAddress) {
                    socket.bind(new InetSocketAddress(localAddress, localPort));
                }
                socket.connect(new InetSocketAddress(addresses[0], port),
                        null == params ? 0 : params.getConnectionTimeout());
                return socket;
            } catch (final IOException e) {
                socket.close();
                throw e;
            }
        }
    }

    /**
     * Same as {@link AddressCheckingSocketFactory}, with TLS on top of the checked connection.
     */
    static final class SecureAddressCheckingSocketFactory extends AddressCheckingSocketFactory
            implements SecureProtocolSocketFactory {

        private final SecureProtocolSocketFactory tls = new SSLProtocolSocketFactory();

        SecureAddressCheckingSocketFactory(final Predicate<InetAddress> isAddressAllowed) {
            super(isAddressAllowed);
        }

        @Override
        public Socket createSocket(final String host, final int port, final InetAddress localAddress,
                                   final int localPort, final HttpConnectionParams params)
                throws IOException {
            return createSocket(super.createSocket(host, port, localAddress, localPort, params),
                    host, port, true);
        }

        @Override
        public Socket createSocket(final Socket socket, final String host, final int port,
                                   final boolean autoClose) throws IOException {
            return tls.createSocket(socket, host, port, autoClose);
        }
    }

    private static boolean isResolvable(final String host) {
        try {
            return UtilMethods.isSet(host) && java.net.InetAddress.getAllByName(host).length > 0;
        } catch (final java.net.UnknownHostException e) {
            return false;
        }
    }

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
