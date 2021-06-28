package com.dotcms.publishing.manifest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import java.util.HashMap;
import java.util.Map;

public interface ManifestItem {

    ManifestInfo getManifestInfo();

    class ManifestInfo {
        private Map<String, Object> info;

        public ManifestInfo(final Map<String, Object> info){
            this.info = info;
        }

        public ManifestInfo merge(final Map<String, Object> mergeWith){
            final Map<String, Object> map = new HashMap<>();
            map.putAll(info);
            map.putAll(mergeWith);

            return new ManifestInfo(map);
        }

        public String objectType(){
            return this.info.get("object type").toString();
        }

        public String id(){
            return this.info.get("id").toString();
        }

        public String title(){
            return this.info.get("title").toString();
        }

        public String site(){
            if (UtilMethods.isSet(this.info.get("site"))) {
                final Object siteObject = this.info.get("site");
                final Host host = Host.class.isInstance(siteObject) ? (Host) this.info.get("site") :
                        getHost(siteObject.toString());
                return host.getName();
            } else {
                return "";
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
            final Object folderObject = this.info.get("folder");

            if (UtilMethods.isSet(folderObject)) {
                final Folder folder = Folder.class.isInstance(folderObject) ? (Folder) folderObject:
                        getFolder(folderObject.toString());
                return folder.getPath();
            } else if (UtilMethods.isSet(this.info.get("path"))) {
                return this.info.get("path").toString();
            } else {
                return "";
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
