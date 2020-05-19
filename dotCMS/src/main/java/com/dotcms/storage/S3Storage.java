package com.dotcms.storage;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class S3Storage implements Storage {

    private final Map<String, com.dotcms.enterprise.publishing.storage.Storage> storageByHostMap = new ConcurrentHashMap<>();

    private com.dotcms.enterprise.publishing.storage.Storage getStorage() {

        Optional<AppSecrets> appSecretsOpt = Optional.empty();
        final Host host = resolveHost();

        if (this.storageByHostMap.containsKey(host.getIdentifier())) {

            return this.storageByHostMap.get(host.getIdentifier());
        }

        try {

            appSecretsOpt = APILocator.getAppsAPI().getSecrets
                    ("app-s3-storage", true, host, APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, e.getMessage());
            throw new DotRuntimeException(e);
        }

        if (appSecretsOpt.isPresent()) {


            final Secret accesskeySecret = appSecretsOpt.get().getSecrets().get("accesskey");
            final Secret secretkeySecret = appSecretsOpt.get().getSecrets().get("secretkey");
            final String accesskey       = null != accesskeySecret? accesskeySecret.getString(): null;
            final String secretkey       = null != secretkeySecret? secretkeySecret.getString(): null;

            if (null != accesskey && null != secretkey) {

                final AWSS3Storage storage = new AWSS3Storage(accesskey, secretkey);
                this.storageByHostMap.put(host.getIdentifier(), storage);
                return storage;
            }
        }

        throw new DotRuntimeException("THe S3Storage needs the app: app-s3-storage, configurated");
    }

    private Host resolveHost () {

        Host host = null;

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (null != request) {
            host = Try.of(() -> WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request)).getOrNull();
        }

        if (null == host) {
            host = Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)).getOrNull();
        }

        return null == host ? APILocator.systemHost() : host;
    }

    @Override
    public boolean existsBucket(final String bucketName) {

        return this.getStorage().existsBucket(bucketName);
    }

    @Override
    public boolean existsObject(final String bucket, final String objectPath) {

        return false;
    }

    @Override
    public boolean createBucket(final String bucketName) {

        return Try.of(()-> { this.getStorage().createBucket(bucketName); return true; }).getOrElse(false);
    }

    @Override
    public boolean createBucket(final String bucketName, final Map<String, Object> extraOptions) {
        return Try.of(()-> {
                            if (extraOptions.containsKey("region")) {
                                this.getStorage().createBucket(bucketName, (String)extraOptions.get("region"));
                            } else {
                                this.getStorage().createBucket(bucketName);
                            }
                            return true;
        }).getOrElse(false);
    }

    @Override
    public boolean deleteBucket(final String bucketName) {
        return Try.of(()-> {
            this.getStorage().deleteBucket(bucketName);
            return true;
        }).getOrElse(false);
    }

    @Override
    public boolean deleteObject(final String bucket, final String path) {
        return Try.of(()-> {
            this.getStorage().deleteFile(bucket, path);
            return true;
        }).getOrElse(false);
    }

    @Override
    public List<Object> listBuckets() {
        final List buckets = Try.of(()->this.getStorage().listBuckets()).getOrElse(()-> Collections.emptyList());
        return buckets;
    }

    @Override
    public Object pushFile(String bucketName, String path, File file, Map<String, Object> extraMeta) {
        return null;
    }

    @Override
    public Object pushObject(String bucketName, String path, ObjectWriterDelegate writerDelegate, Object object, Map<String, Object> extraMeta) {
        return null;
    }

    @Override
    public Future<Object> pushFileAsync(String bucketName, String path, File file, Map<String, Object> extraMeta) {
        return null;
    }

    @Override
    public Future<Object> pushObjectAsync(String bucketName, String path, ObjectWriterDelegate writerDelegate, Object object, Map<String, Object> extraMeta) {
        return null;
    }

    @Override
    public File pullFile(String bucketName, String path) {
        return null;
    }

    @Override
    public Object pullObject(String bucketName, String path, ObjectReaderDelegate readerDelegate) {
        return null;
    }

    @Override
    public Future<File> pullFileAsync(String bucketName, String path) {
        return null;
    }

    @Override
    public Future<Object> pullObjectAsync(String bucketName, String path, ObjectReaderDelegate readerDelegate) {
        return null;
    }
}
