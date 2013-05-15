package com.ettrema.httpclient;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.DateUtils;
import com.bradmcevoy.http.DateUtils.DateParseException;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.ettrema.cache.Cache;
import com.ettrema.httpclient.PropFindMethod.Response;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mcevoyb
 */
public abstract class Resource {

    private static final Logger log = LoggerFactory.getLogger(Resource.class);

    static Resource fromResponse(Folder parent, Response resp, Cache<Folder, List<Resource>> cache) {
        if (resp.isCollection) {
            return new Folder(parent, resp, cache);
        } else {
            return new com.ettrema.httpclient.File(parent, resp);
        }
    }

    /**
     * does percentage decoding on a path portion of a url
     *
     * E.g. /foo > /foo /with%20space -> /with space
     *
     * @param href
     */
    public static String decodePath(String href) {
        // For IPv6
        href = href.replace("[", "%5B").replace("]", "%5D");

        // Seems that some client apps send spaces.. maybe..
        href = href.replace(" ", "%20");
        // ok, this is milton's bad. Older versions don't encode curly braces
        href = href.replace("{", "%7B").replace("}", "%7D");
        try {
            if (href.startsWith("/")) {
                URI uri = new URI("http://anything.com" + href);
                return uri.getPath();
            } else {
                URI uri = new URI("http://anything.com/" + href);
                String s = uri.getPath();
                return s.substring(1);
            }
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
    public Folder parent;
    public String name;
    public String displayName;
    private Date modifiedDate;
    private Date createdDate;
    private final Long quotaAvailableBytes;
    private final Long quotaUsedBytes;
    private final Long crc;
    final List<ResourceListener> listeners = new ArrayList<ResourceListener>();
    private String lockOwner;
    private String lockToken;

    public abstract java.io.File downloadTo(java.io.File destFolder, ProgressListener listener) throws FileNotFoundException, IOException, HttpException, Utils.CancelledException, NotAuthorizedException, BadRequestException;
    
    private static long count = 0;

    public static long getCount() {
        return count;
    }

    public abstract String encodedUrl();

    /**
     * Special constructor for Host
     */
    Resource() {
        this.parent = null;
        this.name = "";
        this.displayName = "";
        this.createdDate = null;
        this.modifiedDate = null;
        quotaAvailableBytes = null;
        quotaUsedBytes = null;
        crc = null;
        count++;
    }

    public Resource(Folder parent, Response resp) {
        count++;
        try {
            if (parent == null) {
                throw new NullPointerException("parent");
            }
            this.parent = parent;
            name = Resource.decodePath(resp.name);
            displayName = Resource.decodePath(resp.displayName);
            if (resp.createdDate != null && resp.createdDate.length() > 0) {
                createdDate = DateUtils.parseWebDavDate(resp.createdDate);
            }
            quotaAvailableBytes = resp.quotaAvailableBytes;
            quotaUsedBytes = resp.quotaUsedBytes;
            crc = resp.crc;

            if (StringUtils.isEmpty(resp.modifiedDate)) {
                modifiedDate = null;
            } else if (resp.modifiedDate.endsWith("Z")) {
                modifiedDate = DateUtils.parseWebDavDate(resp.modifiedDate);
                if (resp.serverDate != null) {
                    // calc difference and use that as delta on local time
                    Date serverDate = DateUtils.parseDate(resp.serverDate);
                    long delta = serverDate.getTime() - modifiedDate.getTime();
                    modifiedDate = new Date(System.currentTimeMillis() - delta);
                } else {
                    log.debug("no server date");
                }
            } else {
                modifiedDate = DateUtils.parseDate(resp.modifiedDate);
            }
            lockToken = resp.lockToken;
            lockOwner = resp.lockOwner;

            //log.debug( "parsed mod date: " + modifiedDate);
        } catch (DateParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Resource(Folder parent, String name, String displayName, String href, Date modifiedDate, Date createdDate) {
        count++;
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        this.parent = parent;
        this.name = name;
        this.displayName = displayName;
        this.modifiedDate = modifiedDate;
        this.createdDate = createdDate;
        quotaAvailableBytes = null;
        quotaUsedBytes = null;
        crc = null;
    }

    public Resource(Folder parent, String name) {
        count++;
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        this.parent = parent;
        this.name = name;
        this.displayName = name;
        this.modifiedDate = null;
        this.createdDate = null;
        quotaAvailableBytes = null;
        quotaUsedBytes = null;
        crc = null;
    }

    @Override
    protected void finalize() throws Throwable {
        count--;
        super.finalize();
    }

    public void addListener(ResourceListener l) {
        listeners.add(l);
    }

    public String post(Map<String, String> params) throws HttpException, NotAuthorizedException, ConflictException, BadRequestException, NotFoundException {
        return host().doPost(encodedUrl(), params);
    }

    public void lock() throws HttpException, NotAuthorizedException, ConflictException, BadRequestException, NotFoundException  {
        if (lockToken != null) {
            log.warn("already locked: " + href() + " token: " + lockToken);
        }
        lockToken = host().doLock(encodedUrl());
    }

    public int unlock() throws HttpException , NotAuthorizedException, ConflictException, BadRequestException, NotFoundException {
        if (lockToken == null) {
            throw new IllegalStateException("Can't unlock, is not currently locked (no lock token) - " + href());
        }
        return host().doUnLock(encodedUrl(), lockToken);
    }

    public void copyTo(Folder folder) throws IOException, HttpException, NotAuthorizedException, ConflictException, BadRequestException, NotFoundException  {
        copyTo(folder, name);
    }

    public void copyTo(Folder folder, String destName) throws IOException, HttpException, NotAuthorizedException, ConflictException, BadRequestException, NotFoundException  {
        host().doCopy(encodedUrl(), folder.encodedUrl() + com.bradmcevoy.http.Utils.percentEncode(destName));
        folder.flush();
    }
    
    public void rename(String newName) throws IOException, HttpException, NotAuthorizedException, ConflictException, BadRequestException, NotFoundException  {
        String dest = "";
        if (parent != null) {
            dest = parent.encodedUrl();
        }
        dest = dest + com.bradmcevoy.http.Utils.percentEncode(newName);
        int res = host().doMove(encodedUrl(), dest);
        if (res == 201) {
            this.name = newName;
        }
    }

    public void moveTo(Folder folder) throws IOException, HttpException, NotAuthorizedException, ConflictException, BadRequestException, NotFoundException  {
        moveTo(folder, name);
    }
    public void moveTo(Folder folder, String destName) throws IOException, HttpException, NotAuthorizedException, ConflictException, BadRequestException, NotFoundException  {
        log.info("Move: " + this.href() + " to " + folder.href());
        int res = host().doMove(encodedUrl(), folder.href() + com.bradmcevoy.http.Utils.percentEncode(destName));
        if (res == 201) {
            this.parent.flush();
            folder.flush();
        }
    }

    public void removeListener(ResourceListener l) {
        listeners.remove(l);
    }

    @Override
    public String toString() {
        return href() + "(" + displayName + ")";
    }

    public void delete() throws IOException, HttpException, NotAuthorizedException, ConflictException, BadRequestException, NotFoundException  {
        host().doDelete(encodedUrl());
        notifyOnDelete();
    }

    void notifyOnDelete() {
        if (this.parent != null) {
            this.parent.notifyOnChildRemoved(this);
        }
        List<ResourceListener> l2 = new ArrayList<ResourceListener>(listeners);
        for (ResourceListener l : l2) {
            l.onDeleted(this);
        }
    }

    public Host host() {
        Host h = parent.host();
        if (h == null) {
            throw new NullPointerException("no host");
        }
        return h;
    }

    public String encodedName() {
        return com.bradmcevoy.http.Utils.percentEncode(name);
    }

    /**
     * Returns the UN-encoded url
     *
     * @return
     */
    public String href() {
        if (parent == null) {
            return name;
            //return encodedName();
        } else {
            //return parent.href() + encodedName();
            return parent.href() + name;
        }
    }

    public Path path() {
        if (parent == null) {
            return Path.root;
            //return encodedName();
        } else {
            //return parent.href() + encodedName();
            return parent.path().child(name);
        }
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Long getQuotaAvailableBytes() {
        return quotaAvailableBytes;
    }

    public Long getQuotaUsedBytes() {
        return quotaUsedBytes;
    }

    public Long getCrc() {
        return crc;
    }

    public String getLockToken() {
        return lockToken;
    }

    public String getLockOwner() {
        return lockOwner;
    }
}
