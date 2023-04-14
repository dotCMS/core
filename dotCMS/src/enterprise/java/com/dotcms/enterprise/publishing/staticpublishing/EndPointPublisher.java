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

package com.dotcms.enterprise.publishing.staticpublishing;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.dotcms.publishing.DotPublishingException;

import java.io.File;
import java.io.Serializable;

/**
 * Encapsulate a behaviour for an endpoint publisher
 * Basically a publisher determine if the endpoint can be reach out (if can connect)
 * and also publish the bundle to the endpoint.
 * @author jsanca
 */
public interface EndPointPublisher extends Serializable {

    /**
     * If the connection can be established successfully then this method not throw any {@link Exception},
     * but if it fails trying to connect then a {@lik EndPointPublisherConnectionException} is thrown.
     *
     * @param validationBucketName bucket name to validate the connection
     * @throws EndPointPublisherConnectionException if the connection ca be established
     */
    void checkConnectSuccessfully(final String validationBucketName) throws EndPointPublisherConnectionException;

    /**
     * Push a file or directory to the endpoint
     * If the bucketName does not exists, it will be created (if the configuration enables)
     * If the bucketRootPrefix is not null, will be use for the root directory (bucketName/bucketRootPrefix)
     * The bundleRootPath is needed to remove it from the file path to upload (the file argument path)
     *
     * @param bucketName String
     * @param bucketRootPrefix String
     * @param filePath String
     * @param file File
     * @throws DotPublishingException
     */
    void pushBundleToEndpoint(String bucketName,
                              final String region,
                              String bucketRootPrefix,
                              String filePath,
                              File file) throws DotPublishingException;

    /**
     * It will delete the file path under the bucket.
     *
     * @param bucketName
     * @param filePath
     */
    void deleteFilesFromEndpoint(final String bucketName,
                                 final String bucketRootPrefix,
                                 final String filePath) throws DotPublishingException;

    /**
     * Call the Transfer Manager shutdown from the storage we have running.
     */
    void shutdownTransferManager();

    /**
     * Checks if a bucket exists in the s3 storage, otherwise it will be created
     * @param bucketName String name of the bucket to be created
     * @param region String region where the bucket will be created
     * @throws DotPublishingException
     */
    void createBucket(final String bucketName, final String region) throws DotPublishingException;

} // E:O:F:EndPointPublisher.
