/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.staticpublishing;

import static com.dotcms.util.CollectionsUtils.set;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.dotcms.enterprise.publishing.bundlers.FileAssetBundler;
import com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentBundler;
import com.dotcms.enterprise.publishing.bundlers.URLMapBundler;
import com.dotcms.enterprise.publishing.storage.AWSS3Storage;
import com.dotcms.enterprise.publishing.storage.Storage;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publishing.DotPublishingException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.amazonaws.util.IOUtils;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.tika.TikaUtils;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.file.FilteredFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.http.conn.HttpHostConnectException;

/**
 * This implementation is for amazon  s3 protocol.
 * Checks if the endpoint is up, creates or use buckets.
 * Publish/Upload files or directories.
 *
 * @author jsanca
 */
public class AWSS3EndPointPublisher implements EndPointPublisher {

    private static final String AWSS3_PUBLISHER_WAITFORCOMPLETION_CONFIG_PARAM_KEY     = "awss3.publisher.waitforcompletion";
    private final Storage storage;
    private final FileFilter awss3FileFilter = new AWSS3FileFilter();

    public AWSS3EndPointPublisher(final AWSS3Configuration configuration) {

        this (new AWSS3Storage(configuration));
    }

    public AWSS3EndPointPublisher(DefaultAWSCredentialsProviderChain credentialsProviderChain) {

        this (new AWSS3Storage(credentialsProviderChain));
    }

    @VisibleForTesting
    protected AWSS3EndPointPublisher(final Storage storage) {

        this.storage = storage;
    }

    public void shutdownTransferManager(){
        this.storage.shutdownTransferManager();
    }

    /**
     * Implementation of {@link EndPointPublisher#checkConnectSuccessfully(String)}.
     *
     * To check the connection this method do the follow:
     *
     * - If the validationBucketName params is not null then it tries to do a headbucket to the S3 server
     * {@link com.amazonaws.services.s3.AmazonS3#headBucket(HeadBucketRequest)}.
     *
     * - If the validationBucketName params is null then it tries to do a listBucket to the S3 server
     * {@link AmazonS3#listBuckets()}.
     *
     * @param validationBucketName bucket name to validate the connection
     * @throws EndPointPublisherConnectionException if the connection ca be established
     */
    @Override
    public void checkConnectSuccessfully(String validationBucketName)  throws EndPointPublisherConnectionException {

        try {

            // Use a highly probable un-existing validation bucket name as default
            if (UtilMethods.isSet(validationBucketName)) {
                // Check basic connectivity to AWS-S3 through "head-bucket" operation against a validation bucket
                this.storage.headBucket(validationBucketName);
            } else {
                this.storage.listBuckets();
            }
        } catch (Exception e) {
           throw new EndPointPublisherConnectionException(e.getCause());
        }
    } // canConnectSuccessfully.

    @Override
    public void deleteFilesFromEndpoint(final String bucketName,
                                        final String bucketRootPrefix,
                                        final String filePath)
        throws DotPublishingException{

        try{
            //We want to ignore these extensions because they weren't pushed.
            if (awss3FileFilter.accept(new File(filePath))) {
                String filePathInBucket = filePath;
                if (filePathInBucket.startsWith(File.separator)){
                    filePathInBucket = filePathInBucket.substring(1);
                }
                if (UtilMethods.isSet(bucketRootPrefix)){
                    filePathInBucket = bucketRootPrefix + File.separator + filePathInBucket;
                }
                Logger.debug(this, "Deleting file named: " + filePathInBucket + " from bucket: " + bucketName);
                this.storage.deleteFile(bucketName, filePathInBucket);
            }

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotPublishingException(e.getMessage(), e);
        }
    } // deleteFilesFromEndpoint

    @Override
    public void pushBundleToEndpoint(final String bucketName, final String region, final String bucketRootPrefix,
                                     final String filePath, final File file) throws DotPublishingException {

        if (file.isDirectory()) {
            Logger.info(this, "Pushing Folder: " + file.getAbsolutePath());

            try{
                this.pushDirectory(bucketName, bucketRootPrefix, filePath, file);
            } catch (Exception pushDirectoryE){
                Logger.error(this, "Error trying to push whole Folder at once, will retry file by file, path "
                    + file.getAbsolutePath(), pushDirectoryE);
                //If we get an exception pushing WHOLE folder we should try file by file.
                //We need to get all the files under the folder and push one by one.
                Collection<File> listFiles = FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                for (File eachFile : listFiles) {
                    try {
                        String eachFolderName = file.getName();
                        if (!eachFolderName.endsWith(File.separator)){
                            eachFolderName = eachFolderName + File.separator;
                        }
                        String eachFilePath = eachFile.getAbsolutePath().replace(file.getAbsolutePath(), "");
                        if (eachFilePath.startsWith(File.separator)){
                            eachFilePath = eachFilePath.substring(1);
                        }
                        eachFilePath = eachFolderName + eachFilePath;
                        Logger.debug(this, "Pushing File: " + eachFile.getAbsolutePath());
                        this.pushSingleFile(bucketName, bucketRootPrefix, eachFilePath, eachFile);
                    } catch (Exception e) {
                        Logger.error(this, "Can't push File: " + eachFile.getAbsolutePath(), e);
                    }
                }
            }
        } else {
            pushSingleFile(bucketName, bucketRootPrefix, filePath, file);
        }
    } // pushBundleToEndpoint.

    private boolean pushSingleFile(String bucketName, String bucketRootPrefix, String filePath, File file) {
        //We want to filter these extensions.
        if (!awss3FileFilter.accept(file)) {
            return true;
        }

        final int secondsToSleep = Config.getIntProperty("STATIC_PUSH_SLEEP_ON_ERROR_SECONDS", 10);
        final int pushRetries = Config.getIntProperty("STATIC_PUSH_RETRY_ATTEMPTS", 3);

        boolean success = false;
        int retriesCount = 0;
        String errorMessages = "";

        while (!success && retriesCount <= pushRetries){
            try {
                Logger.info(this, "Pushing File: " + filePath + ", retries: " + retriesCount);
                this.pushFile(bucketName, bucketRootPrefix, filePath, file);
                success = true;
            } catch (Exception e) {
                errorMessages += "\n Retry #: " + retriesCount + ", Error: " + e.getMessage();
                retriesCount++;
                try {
                    Logger.info(this, "Sleeping before next push try, seconds: " + secondsToSleep);
                    Thread.sleep(secondsToSleep * 1000);
                } catch (InterruptedException ie){
                    Logger.error(this, "Can't Sleep before retry file: " + file.getAbsolutePath());
                }
            }
        }

        if (!success){
            Logger.error(this, "Can't push file: " + file.getAbsolutePath() + ", reasons : " + errorMessages);
        }

        return success;
    }

    protected void pushFile(final String bucketName, final String bucketRootPrefix,
                            final String filePath, final File file) throws IOException, DecoderException, InterruptedException {

        UploadResult result;

        //We want to filter these extensions.
        if (awss3FileFilter.accept(file)) {
            Logger.debug(this, "Pushing File: " + file);

            String folderPath = this.getFolderPath(bucketRootPrefix, filePath, file);
            final String
                completeFileKey =
                UtilMethods.isSet(folderPath) ? folderPath + File.separator + file.getName() : file.getName();

            ObjectMetadata objectMetadata = new ObjectMetadata();
            setMD5Based64(file, objectMetadata);
            setContentType(file, objectMetadata);
            try (InputStream is = Files.newInputStream(file.toPath())){
                objectMetadata.setContentLength(file.length());
                PutObjectRequest
                        putObjectRequest =
                        new PutObjectRequest(bucketName, completeFileKey, is, objectMetadata);

                final Upload upload = this.storage.uploadFile(putObjectRequest);

                if (null != upload) {

                    if (this.isWaitForCompletionNeeded()) {

                        result = upload.waitForUploadResult();
                        Logger.debug(this, "File: " + file + " has been uploaded, result: " + result);
                    }
                }
            }
        }
    }

    protected void pushDirectory(final String bucketName, final String bucketRootPrefix,
                                 final String filePath, final File file) throws InterruptedException {

        UploadResult result;
        Collection<? extends Upload> subTransfers;

        AWSS3ObjectMetadataProvider awss3ObjectMetadataProvider = new AWSS3ObjectMetadataProvider();
        FilteredFile filteredFile = new FilteredFile(file, awss3FileFilter);

        Logger.debug(this, "Updating the Directory: " + filteredFile);

        final MultipleFileUpload multipleFileUpload =
                this.storage.uploadFolder(bucketName,
                        this.getFolderPath(bucketRootPrefix, filePath, filteredFile), filteredFile, awss3ObjectMetadataProvider);

        if ((null != multipleFileUpload) && (this.isWaitForCompletionNeeded())) {

            multipleFileUpload.waitForCompletion();

            subTransfers = multipleFileUpload.getSubTransfers();

            if (null != subTransfers) {

                Logger.info(this, "Directory: " + filteredFile + " has been uploaded");
                for (Upload upload : subTransfers) {

                    result = upload.waitForUploadResult();
                    Logger.debug(this, "\tUpload result: " + result);
                }
            }
        }
    } // pushDirectory.
    
    private boolean isWaitForCompletionNeeded() {

        return Config.getBooleanProperty
                (AWSS3_PUBLISHER_WAITFORCOMPLETION_CONFIG_PARAM_KEY, true);
    } // isWaitForCompletionNeeded.


    protected String getFolderPath(final String bucketRootPrefix,
                                   final String filePath, final File file) {

        String fileName = file.getName();
        String folderPath = filePath;
        if (file.isFile()){
            folderPath = folderPath.substring(0, folderPath.lastIndexOf(File.separator)+1);
        }

        if (UtilMethods.isSet(bucketRootPrefix)) {

            if (bucketRootPrefix.endsWith(File.separator) && folderPath.startsWith(File.separator)) {
                // removing the extra /
                folderPath = bucketRootPrefix + folderPath.substring(1);
            } else if (bucketRootPrefix.endsWith(File.separator) || folderPath.startsWith(File.separator)) {
                // if just one of them has the /, concatening is ok
                folderPath = bucketRootPrefix + folderPath;
            } else {
                // not any have a file separator
                folderPath = bucketRootPrefix + File.separator + folderPath;
            }
        }

        if (folderPath.startsWith(File.separator)) {
            // the store will add a path separator, so if there is one we have to remove it.
            folderPath = folderPath.substring(1);
        }

        if (folderPath.endsWith(File.separator)) {
            // the store will add a path separator, so if there is one we have to remove it.
            folderPath = folderPath.substring(0, folderPath.length() -1);
        }

        return folderPath;
    } // getFolderPath.

    public void createBucket(final String bucketName, final String region) throws DotPublishingException {

        if (!this.exists (bucketName)) {
            Logger.info(this, "Creating bucket, name: " + bucketName + " , Region: " + region);
            try {
                this.createNewBucket(bucketName, region);
            } catch (Exception e){
                throw new DotPublishingException(
                    "The Bucket: " + bucketName + " does not exists and can not created", e);
            }
        }
    } // createBucket.

    public boolean exists(final String bucketName) {

        return this.storage.existsBucket(bucketName);
    } // exists.

    /**
     * Creates the bucket using {@link AWSS3Storage}.
     *
     * @param bucketName
     * @param region if value is 'us-east-1' it will create the bucket without region.
     */
    private void createNewBucket(final String bucketName, final String region) {

        if (AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_REGION_DEFAULT.equals(region)){
            this.storage.createBucket(bucketName);
        } else {
            this.storage.createBucket(bucketName, region);
        }
    } // createNewBucket.

    ///////////////////////////////////
    ///////ObjectMetadata Utils////////
    ///////////////////////////////////
    private class AWSS3ObjectMetadataProvider implements ObjectMetadataProvider{
        @Override
        public void provideObjectMetadata(File file, ObjectMetadata objectMetadata) {
            InputStream is = null;
            try{
                setMD5Based64(file, objectMetadata);
                setContentType(file, objectMetadata);
                is = Files.newInputStream(file.toPath());
                objectMetadata.setContentLength(file.length());
            } catch (Exception e){
                Logger.error(this, "Error setting objectMetadata in file: " + file.getAbsolutePath(), e);
            } finally {
                CloseUtils.closeQuietly(is);
            }

        }
    }

    private void setMD5Based64(File file, ObjectMetadata objectMetadata) throws IOException, DecoderException{
        //Setting MD5 to check against uploaded file.
        try (InputStream is = Files.newInputStream(file.toPath())){
            String md5HexString = DigestUtils.md5Hex(is);
            //Amazon expects the md5 value to be base64 encoded
            byte[] decodedHex = Hex.decodeHex(md5HexString.toCharArray());
            objectMetadata.setContentMD5(Base64.encodeBase64String(decodedHex));
        }
    }

    
    private void setContentType(File file, ObjectMetadata objectMetadata){
      if (file.isDirectory()) return;
      String mimeType = Config.CONTEXT.getMimeType(file.getAbsolutePath());
        
      if( !UtilMethods.isSet(mimeType)){
          try{
              mimeType = new TikaUtils().detect(file);
          }catch(Exception e){
            Logger.warn(this.getClass(), e.getMessage() +  e.getStackTrace()[0]);
          }
      }
        
      if (UtilMethods.isSet(mimeType)){
          objectMetadata.setContentType(mimeType);
          if (mimeType.toLowerCase().contains("html")){
              objectMetadata.setHeader("Content-Language", "html");
          }
      } else {
          objectMetadata.setContentType("text/html");
          objectMetadata.setHeader("Content-Language", "html");
          
      }
    }

    private String[] combinedExtensions = Stream.of(
                    URLMapBundler.URLMAP_EXTENSIONS,
                    FileAssetBundler.FILE_ASSET_EXTENSIONS,
                    HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSIONS
            )
            .flatMap(Arrays::stream)
            .toArray(String[]::new);

    private class AWSS3FileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                return true;
            }
            for (String ext : combinedExtensions) {
                if (pathname.getName().toLowerCase().endsWith(ext)) {
                    return false;
                }
            }
            return true;
        }
    }
    //////////////////////////////////////////
    ///////End of ObjectMetadata Utils////////
    //////////////////////////////////////////

} // E:O:F:AWSS3EndPointPublisher.
