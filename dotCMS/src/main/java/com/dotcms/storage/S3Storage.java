package com.dotcms.storage;

public class S3Storage {

}
/*
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
    public Object pushFile(final String bucketName, final String path, final File file, final Map<String, Object> extraMeta) {

        UploadResult result = null;

        Logger.debug(this, ()->"Pushing File: " + file);

        final ObjectMetadata objectMetadata = new ObjectMetadata();
        //setMD5Based64(file, objectMetadata); // todo: set the extraMeta.
        //setContentType(file, objectMetadata);
        try (InputStream is = Files.newInputStream(file.toPath())) {

            objectMetadata.setContentLength(file.length());
            final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path, is, objectMetadata);
            final Upload upload = this.getStorage().uploadFile(putObjectRequest);

            if (null != upload) {

                result = upload.waitForUploadResult();
                Logger.debug(this, "File: " + file + " has been uploaded, result: " + result);
            }
        } catch (IOException | InterruptedException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }

        return result;
    }

    private Tuple2<Integer, InputStream> objectToStream(final ObjectWriterDelegate writerDelegate,
                                                        final Object object) {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  // todo not sure if I should compress this
        writerDelegate.write(byteArrayOutputStream, object);
        return Tuple.of(byteArrayOutputStream.size(), new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    }

    @Override
    public Object pushObject(final String bucketName,
                             final String path,
                             final ObjectWriterDelegate writerDelegate,
                             final Object object,
                             final Map<String, Object> extraMeta) {

        UploadResult result = null;

        Logger.debug(this, ()->"Pushing Object: " + object);

        final ObjectMetadata objectMetadata = new ObjectMetadata();
        //setMD5Based64(file, objectMetadata); // todo: set the extraMeta.
        //setContentType(file, objectMetadata);
        final Tuple2<Integer, InputStream> sizeAndStream = this.objectToStream(writerDelegate, object);
        final long objectLength = ConversionUtils.toLong(sizeAndStream._1(), 0l);
        try (InputStream is = sizeAndStream._2()) {

            objectMetadata.setContentLength(objectLength);
            final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path, is, objectMetadata);
            final Upload upload = this.getStorage().uploadFile(putObjectRequest);

            if (null != upload) {

                result = upload.waitForUploadResult();
                Logger.debug(this, "Object: " + object + " has been uploaded, result: " + result);
            }
        } catch (IOException | InterruptedException e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }

        return result;
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
*/
