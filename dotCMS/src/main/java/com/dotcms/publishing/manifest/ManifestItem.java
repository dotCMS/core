package com.dotcms.publishing.manifest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.liferay.util.StringPool;
import java.util.HashMap;
import java.util.Map;

public interface ManifestItem {

    @JsonIgnore
    ManifestInfo getManifestInfo();

    class ManifestInfoBuilder {
        private String objectType;
        private String id;
        private String title;
        private String siteId;
        private String folderId;
        private String folderPath;

        public static ManifestInfo merge(final ManifestInfo manifestInfo1, final ManifestInfo manifestInfo2){
            final ManifestInfoBuilder builder = new ManifestInfoBuilder();
            builder.objectType = UtilMethods.isSet(manifestInfo2.objectType) ? manifestInfo2.objectType :
                    manifestInfo1.objectType;
            builder.id = UtilMethods.isSet(manifestInfo2.id) ? manifestInfo2.id :
                    manifestInfo1.id;
            builder.title = UtilMethods.isSet(manifestInfo2.title) ? manifestInfo2.title :
                    manifestInfo1.title;
            builder.siteId = UtilMethods.isSet(manifestInfo2.siteId) ? manifestInfo2.siteId :
                    manifestInfo1.siteId;
            builder.folderId = UtilMethods.isSet(manifestInfo2.folderId) ? manifestInfo2.folderId :
                    manifestInfo1.folderId;
            builder.folderPath = UtilMethods.isSet(manifestInfo2.folderPath) ? manifestInfo2.folderPath :
                    manifestInfo1.folderPath;

            return builder.build();
        }

        public ManifestInfoBuilder objectType(String objectType){
            this.objectType = objectType;
            return this;
        }

        public ManifestInfoBuilder id(String id){
            this.id = id;
            return this;
        }

        public ManifestInfoBuilder title(final String title){
            this.title = title;
            return this;
        }

        public ManifestInfoBuilder site(final Host site){
            if (UtilMethods.isSet(site)) {
                this.siteId = site.getIdentifier();
            }
            return this;
        }

        public ManifestInfoBuilder siteId(final String siteId){
            this.siteId = siteId;
            return this;
        }

        public ManifestInfoBuilder folderId(String folderId){
            this.folderId = folderId;
            return this;
        }

        public ManifestInfoBuilder folder(Folder folder){
            if (UtilMethods.isSet(folder)) {
                this.folderId = folder.getIdentifier();
            }

            return this;
        }

        public ManifestInfoBuilder path(String path){
            this.folderPath = path;
            return this;
        }

        public ManifestInfo build(){
            return new ManifestInfo(this);
        }
    }

    class ManifestInfo {
        private String objectType;
        private String id;
        private String title;
        private String siteId;
        private String folderId;
        private String folderPath;

        public ManifestInfo(final ManifestInfoBuilder builder){
            this.objectType = builder.objectType;
            this.id = builder.id;
            this.title = builder.title;
            this.siteId = builder.siteId;
            this.folderId = builder.folderId;
            this.folderPath = builder.folderPath;
        }

        public String objectType(){
            return this.objectType;
        }

        public String id(){
            return this.id;
        }

        public String title(){
            return this.title;
        }

        public String site(){
            if (UtilMethods.isSet(siteId)) {
                final Host host = getHost(siteId);
                return host.getHostname();
            } else {
                return StringPool.BLANK;
            }
        }

        private Host getHost(String siteId) {
            try {
                return APILocator.getHostAPI()
                        .find(siteId, APILocator.systemUser(), false);
            } catch (DotDataException | DotSecurityException e) {
                throw new DotRuntimeException(e);
            }
        }

        public String folder(){
            if (UtilMethods.isSet(folderId)) {
                final Folder folder = getFolder(folderId);
                return folder.getPath();
            } else if (UtilMethods.isSet(folderPath)) {
                return folderPath;
            } else {
                return StringPool.BLANK;
            }
        }

        private Folder getFolder(final String folderId) {
            try {
                return APILocator.getFolderAPI()
                        .find(folderId, APILocator.systemUser(), false);
            } catch (DotSecurityException | DotDataException e) {
                throw new DotRuntimeException(e);
            }
        }
    }
}
