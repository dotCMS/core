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

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.HeadBucketResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.ObjectMetadataProvider;
import com.amazonaws.services.s3.transfer.Upload;
import com.dotmarketing.exception.DotRuntimeException;

import java.io.File;
import java.util.List;

/**
 * Wrapper around Amazon SDK methods for basic file operation against S3.
 */
public interface Storage {

    /**
     * Returns true if the bucket exists.
     * @param bucketName String
     * @return Boolean
     * @throws DotRuntimeException
     */
    boolean existsBucket(final String bucketName) throws DotRuntimeException;

    /**
     * Creates a new Amazon S3 bucket with the specified name.
     */
    void createBucket(final String bucketName) throws DotRuntimeException;

    /**
     * Creates a new Amazon S3 bucket with the specified name and region.
     */
    void createBucket(final String bucketName, final String region) throws DotRuntimeException;

    /**
     * Deletes the specified bucket. All objects (and all object versions, if versioning was ever enabled)
     * in the bucket must be deleted before the bucket itself can be deleted.
     */
    void deleteBucket(final String bucketName) throws DotRuntimeException;

    /**
     * Returns a list of all Amazon S3 buckets.
     */
    List<Bucket> listBuckets() throws DotRuntimeException;

    /**
     * Create meta-data for your folder and set content-length to 0.
     */
    void createFolder(final String bucketName, final String folderName) throws DotRuntimeException;

    /**
     * Async method: Uploads all files in the directory given to the bucket named, recursively for all subdirectories.
     */
    MultipleFileUpload uploadFolder(final String bucketName, final String folderPath, final File folder)
        throws DotRuntimeException;

    /**
     * Async method: Uploads all files including a {@link ObjectMetadataProvider} in the directory given
     * to the bucket named, recursively for all subdirectories.
     */
    MultipleFileUpload uploadFolder(final String bucketName, final String folderPath,
                                           final File folder, final ObjectMetadataProvider objectMetadataProvider);

    /**
     * This method first deletes all the files in given folder and than the
     * folder itself
     */
    void deleteFolder(final String bucketName, final String folderName) throws DotRuntimeException;

    /**
     * Async method: Schedules a new transfer to upload data to Amazon S3.
     */
    Upload uploadFile(final String bucketName, final String folderName, final File file) throws DotRuntimeException;

    /**
     * Async method: Schedules a new transfer to upload data to Amazon S3 using {@link PutObjectRequest}
     */
    Upload uploadFile(final PutObjectRequest putObjectRequest) throws DotRuntimeException;

    /**
     * Async method: Schedules a new transfer to download data from Amazon S3 and save it to the specified file.
     */
    Download downloadFile(final String bucketName, final String filePath, File file) throws DotRuntimeException;

    /**
     * Deletes the specified object in the specified bucket.
     */
    void deleteFile(final String bucketName, final String filePath) throws DotRuntimeException;

    /**
     * Contains the results of listing the objects in an Amazon S3 bucket under the folder path specified.
     */
    ObjectListing listObjects(final String bucketName, final String folderPath) throws DotRuntimeException;

    /**
     * Performs a head bucket operation on the requested bucket name. This operation is useful to determine if a bucket exists and you have permission to access it.
     */
    HeadBucketResult headBucket(String bucketName) throws DotRuntimeException;

    /**
     * Forcefully shuts down this TransferManager instance - currently executing transfers will not be allowed to finish.
     * It also by default shuts down the underlying Amazon S3 client.
     */
    void shutdownTransferManager();

}
