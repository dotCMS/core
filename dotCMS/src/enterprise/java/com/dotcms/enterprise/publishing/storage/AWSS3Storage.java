/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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


        return AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withClientConfiguration(getClientConfiguration())
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                UtilMethods.isSet(endPoint) ? endPoint : "s3.amazonaws.com",
                                region))
                .build();
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
        conf.setSignerOverride("S3SignerType");

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
            return s3client.listObjects(lor);
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
