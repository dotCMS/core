/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
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
