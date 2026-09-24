/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.storage;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.*;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AWSS3Storage implements Storage {

    private static String DEFAULT_S3_REGION = "us-west-2";

    private static final int DEFAULT_STATIC_PUSH_THREAD_POOL_SIZE = Config
            .getIntProperty("DEFAULT_STATIC_PUSH_THREAD_POOL_SIZE", 10);
    private static final Long DEFAULT_STATIC_PUSH_MULTIPART_UPLOAD_THRESHOLD = Long.valueOf(Config
            .getStringProperty("STATIC_PUSH_MULTIPART_UPLOAD_THRESHOLD", "33554432"));

    private final AmazonS3 s3client;
    private final TransferManager transferManager;

    public AWSS3Storage(DefaultAWSCredentialsProviderChain credentialsProviderChain) {
        this (getAmazonS3Client(credentialsProviderChain , null, DEFAULT_S3_REGION));
    }

    public AWSS3Storage(final AWSCredentialsProvider credentialsProvider, final String endpoint,
            final String region) {
        this(getAmazonS3Client(credentialsProvider, endpoint, region));
    }

    public AWSS3Storage(final AWSS3Configuration configuration) {
        this(getAmazonS3Client(configuration.getAccessKey(), configuration.getSecretKey(),
                configuration.getEndPoint(), configuration.getRegion()));
    }

    private static AmazonS3 getAmazonS3Client(final String accessKey,
            final String secretKey, final String endPoint, final String region) {
        return getAmazonS3Client(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))
                , endPoint, region);
    }

    private static AmazonS3 getAmazonS3Client(final AWSCredentialsProvider credentialsProvider,
            final String endPoint, final String region) {


        final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withClientConfiguration(getClientConfiguration());
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled() || UtilMethods.isSet(endPoint)) {
            builder.withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                UtilMethods.isSet(endPoint) ? endPoint : "s3.amazonaws.com",
                                region));
        } else if (UtilMethods.isSet(region)) {
            builder.withRegion(region);
        }
        return builder.build();
    }

    private AWSS3Storage(AmazonS3 s3client) {
        this.s3client = s3client;

        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMultipartUploadThreshold(DEFAULT_STATIC_PUSH_MULTIPART_UPLOAD_THRESHOLD);

        this.transferManager = new TransferManager(s3client, Executors.newFixedThreadPool(DEFAULT_STATIC_PUSH_THREAD_POOL_SIZE));
        this.transferManager.setConfiguration(configuration);

    }

    private static ClientConfiguration getClientConfiguration(){
        ClientConfiguration conf = new ClientConfiguration();
        conf.setSignerOverride(com.dotcms.storage.AssetStorageFeature.isEnabled() ? "AWSS3V4SignerType" : "S3SignerType");

        return conf;
    }

    @Override
    public void shutdownTransferManager(){
        this.transferManager.shutdownNow();
    }

    @Override
    public boolean existsBucket(final String bucketName) throws DotRuntimeException {

        return this.s3client.doesBucketExistV2(bucketName);
    } // existsBucket.

    @Override
    public void createBucket(final String bucketName) throws DotRuntimeException {
        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            s3client.createBucket(createBucketRequest);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public void createBucket(final String bucketName, final String region) throws DotRuntimeException {
        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName, region);
            s3client.createBucket(createBucketRequest);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public List<Bucket> listBuckets() throws DotRuntimeException {
        try {
            return s3client.listBuckets();
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public void createFolder(final String bucketName, final String folderName) throws DotRuntimeException {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(0);
            InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
                folderName + File.separator, emptyContent, metadata);

            s3client.putObject(putObjectRequest);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public MultipleFileUpload uploadFolder(final String bucketName, final String folderPath, final File folder)
        throws DotRuntimeException {
        try {
            return transferManager.uploadDirectory(bucketName, folderPath, folder, true);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public MultipleFileUpload uploadFolder(final String bucketName, final String folderPath,
                                           final File folder, final ObjectMetadataProvider objectMetadataProvider)
        throws DotRuntimeException {
        try {
            return transferManager.uploadDirectory(bucketName, folderPath, folder, true, objectMetadataProvider);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public ObjectListing listObjects(String bucketName, String folderPath) throws DotRuntimeException {
        try {
            ListObjectsRequest lor = new ListObjectsRequest().withBucketName(bucketName).withPrefix(folderPath);
            final ObjectListing result = s3client.listObjects(lor);
            if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
                return result;
            }
            ObjectListing page = result;
            while (page.isTruncated()) {
                page = s3client.listNextBatchOfObjects(page);
                result.getObjectSummaries().addAll(page.getObjectSummaries());
            }
            result.setTruncated(false);
            return result;
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public void deleteFolder(final String bucketName, final String folderName) throws DotRuntimeException {
        try {
            List<S3ObjectSummary> fileList = s3client.listObjects(bucketName, folderName).getObjectSummaries();
            for (S3ObjectSummary file : fileList) {
                s3client.deleteObject(bucketName, file.getKey());
            }
            s3client.deleteObject(bucketName, folderName);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public Upload uploadFile(final String bucketName, final String folderPath, final File file)
        throws DotRuntimeException {
        try {
            final String completeFileKey =
                UtilMethods.isSet(folderPath) ? folderPath + File.separator + file.getName() : file.getName();
            return transferManager.upload(bucketName, completeFileKey, file);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public Upload uploadFile(final PutObjectRequest putObjectRequest)
        throws DotRuntimeException {
        try {
            return transferManager.upload(putObjectRequest);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public S3Object getObject(final String bucket, final String key) {
        try {
            return s3client.getObject(bucket, key);
        } catch (AmazonS3Exception failure) {
            if (failure.getStatusCode() == 404 && "NoSuchKey".equals(failure.getErrorCode())) return null;
            throw failure;
        }
    }

    @Override
    public String uploadFileIfMatch(final String bucket, final String key, final File file, final String etag) {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) throw new IllegalStateException("S3 asset storage is disabled");
        final var request = new PutObjectRequest(bucket, key, file);
        request.putCustomRequestHeader(etag == null ? "If-None-Match" : "If-Match", etag == null ? "*" : "\"" + etag + "\"");
        try {
            return s3client.putObject(request).getETag();
        } catch (AmazonS3Exception failure) {
            if (failure.getStatusCode() == 412 || failure.getStatusCode() == 409
                    || (etag != null && failure.getStatusCode() == 404 && "NoSuchKey".equals(failure.getErrorCode()))) return null;
            throw failure;
        }
    }

    /** Creates an object without replacing an existing key, including multipart completion. */
    @Override
    public void uploadFileIfAbsent(final String bucket, final String key, final File file) {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            throw new IllegalStateException("S3 asset storage is disabled");
        }
        if (file.length() <= DEFAULT_STATIC_PUSH_MULTIPART_UPLOAD_THRESHOLD) {
            final PutObjectRequest request = new PutObjectRequest(bucket, key, file);
            request.putCustomRequestHeader("If-None-Match", "*");
            s3client.putObject(request);
            return;
        }
        // The bundled TransferManager does not carry custom headers to multipart completion.
        final String uploadId = s3client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key)).getUploadId();
        try {
            final List<PartETag> parts = new ArrayList<>();
            final long length = file.length();
            final long partSize = Math.max(8L * 1024 * 1024, (length + 9999) / 10000);
            for (long offset = 0; offset < length; offset += partSize) {
                parts.add(s3client.uploadPart(new UploadPartRequest().withBucketName(bucket).withKey(key)
                        .withUploadId(uploadId).withPartNumber(parts.size() + 1).withFile(file)
                        .withFileOffset(offset).withPartSize(Math.min(partSize, length - offset))).getPartETag());
            }
            final CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(bucket, key, uploadId, parts);
            request.putCustomRequestHeader("If-None-Match", "*");
            s3client.completeMultipartUpload(request);
        } catch (RuntimeException failure) {
            try {
                s3client.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
            } catch (RuntimeException abortFailure) {
                failure.addSuppressed(abortFailure);
            }
            throw failure;
        }
    }

    /** Verifies opaque/encrypted/multipart ETags by reading the actual object bytes. */
    @Override
    public boolean fileContentsMatch(final String bucket, final String key, final File file) throws IOException {
        try (final S3Object object = s3client.getObject(bucket, key);
             final InputStream local = Files.newInputStream(file.toPath())) {
            return object.getObjectMetadata().getContentLength() == file.length()
                    && org.apache.commons.io.IOUtils.contentEquals(object.getObjectContent(), local);
        }
    }


    @Override
    public Download downloadFile(final String bucketName, final String filePath, File file) throws DotRuntimeException {
        try {
            return transferManager.download(new GetObjectRequest(bucketName, filePath), file);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public void deleteFile(final String bucketName, final String filePath) throws DotRuntimeException {
        try {
            s3client.deleteObject(bucketName, filePath);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public void deleteBucket(final String bucketName) throws DotRuntimeException {
        try {
            s3client.deleteBucket(bucketName);
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }

    @Override
    public HeadBucketResult headBucket(String bucketName) throws DotRuntimeException {
        try {
            return s3client.headBucket(new HeadBucketRequest(bucketName));
        } catch (AmazonServiceException ase) {
            throw new DotRuntimeException("Caught an error from Amazon S3: request made but was rejected", ase);
        } catch (AmazonClientException ace) {
            throw new DotRuntimeException("Caught an error from Amazon S3: client encountered an internal error", ace);
        }
    }
}
