package com.dotmarketing.webdav;

import java.util.Optional;
import java.util.regex.Pattern;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.CMSFilter.IAm;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.filters.CMSUrlUtil;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.vavr.control.Try;

public class DavParams {

    
    public enum DAV_RESOURCE{
        NOTHING,
        ROOT,
        SYSTEM,
        HOST,
        FOLDER,
        FILE;

    }
    
    
    
    private final static Pattern TEMP_RESOURCE = Pattern.compile("/\\(.*\\)|/._\\(.*\\)|/\\.|^\\.|^\\(.*\\)");
    

    
    public final String davUrl;
    public final long languageId;
    public final Host host;
    public final boolean autoPub;
    public final String path;
    public final String parentPath;
    public final String name;
    public final DAV_RESOURCE iam;
    

    public DavParams(String urlIncoming) {
        urlIncoming=urlIncoming.replace("/webdav", "");
        this.davUrl=urlIncoming;
        final String splitUrl = this.davUrl.substring((this.davUrl.startsWith("/")? 1 : 0), this.davUrl.endsWith("/") ? this.davUrl.length()-1 : this.davUrl.length());
        String[] params  = splitUrl.split("/", 4);
        this.autoPub= "live".equals(params[0]);
        this.languageId = Try.of(()-> APILocator.getLanguageAPI().getLanguage(params[1]).getId()).getOrElse(-1L);
        
        this.host = Try.of(() -> {
            return "system".equals(params[2])
                ? APILocator.systemHost()
                : APILocator.getHostAPI().findByName(params[2], APILocator.systemUser(), false);
        }).getOrNull();
        
        if(params.length<4) {
            this.path="/";
            this.name=(host!=null) ? host.getHostname() : null;
            this.parentPath="/";
            this.iam=DAV_RESOURCE.ROOT;
            return;
        }

        String[] pathSplit=params[3].split("/");
        this.name = pathSplit[pathSplit.length-1];
        this.path="/" + params[3];
        this.parentPath =  this.path.replaceAll(this.name , "");
        IAm iam =CMSUrlUtil.getInstance().resolveResourceType(null, path, host, languageId);
        
        this.iam = (iam == IAm.FOLDER) 
                ? DAV_RESOURCE.FOLDER
                : (iam == IAm.FILE) 
                    ? DAV_RESOURCE.FILE
                        : DAV_RESOURCE.NOTHING;

    }

   
    
    






    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (autoPub ? 1231 : 1237);
        result = prime * result + ((davUrl == null) ? 0 : davUrl.hashCode());
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((iam == null) ? 0 : iam.hashCode());
        result = prime * result + (int) (languageId ^ (languageId >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((parentPath == null) ? 0 : parentPath.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }










    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DavParams other = (DavParams) obj;
        if (autoPub != other.autoPub)
            return false;
        if (davUrl == null) {
            if (other.davUrl != null)
                return false;
        } else if (!davUrl.equals(other.davUrl))
            return false;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (iam != other.iam)
            return false;
        if (languageId != other.languageId)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (parentPath == null) {
            if (other.parentPath != null)
                return false;
        } else if (!parentPath.equals(other.parentPath))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }










    @Override
    public String toString() {
        return "DavParams [davUrl=" + davUrl + ", languageId=" + languageId + ", host=" + host + ", autoPub=" + autoPub
                        + ", path=" + path + ", parentPath=" + parentPath + ", name=" + name + "]";
    }
    
    public boolean isFolder() {
        return this.iam == DAV_RESOURCE.FOLDER;
    }
    
    public boolean isHost() {
        return this.iam == DAV_RESOURCE.ROOT && this.host!=null && !APILocator.systemHost().equals(this.host) && this.host.getHostname().equals(this.name);
    }
    
    public boolean isFile() {
        return this.iam == DAV_RESOURCE.FILE;
    }
    
    public boolean isRoot() {
        return this.iam == DAV_RESOURCE.ROOT;
    }
    
    public boolean isSystem() {
        return this.iam == DAV_RESOURCE.ROOT  && this.host!=null && APILocator.systemHost().equals(this.host) && APILocator.systemHost().getHostname().equals(this.name);
    }
    
    public boolean isLanguages() {
        return this.iam == DAV_RESOURCE.NOTHING  && this.host!=null && APILocator.systemHost().equals(this.host) && "langauges".equals(this.name);
    }
    
    public boolean isTempFile() {
        return TEMP_RESOURCE.matcher(davUrl).find();
    }
    
    
    
    public Optional<DavParams> getDestination(){
        final Request request = HttpManager.request();
        request.getMethod();
        if(UtilMethods.isSet(request.getDestinationHeader())) {
            return Optional.of(new DavParams(request.getDestinationHeader()));
        }
        return Optional.empty();
        
        
    }
    
    
    
    
    
}
