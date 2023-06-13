package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.repackage.com.amazonaws.AmazonClientException;
import com.dotcms.repackage.com.amazonaws.ClientConfiguration;
import com.dotcms.repackage.com.amazonaws.auth.AWSCredentials;
import com.dotcms.repackage.com.amazonaws.auth.BasicAWSCredentials;
import com.dotcms.repackage.com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.dotcms.repackage.com.amazonaws.services.s3.AmazonS3Client;
import com.dotcms.repackage.com.amazonaws.services.s3.model.HeadBucketRequest;
import com.dotcms.repackage.com.amazonaws.services.s3.model.HeadBucketResult;

/**
 * Wrapper class around {@link DotAmazonS3Client} to be able to use the protected method
 * headBucket().
 *
 * TODO: This class is not needed for later versions of the AWS SDK, but right now we need it.
 * ---> https://github.com/dotCMS/core/issues/12187
 */
public class DotAmazonS3Client extends AmazonS3Client {

    public DotAmazonS3Client(DefaultAWSCredentialsProviderChain credentialsProviderChain) {
        super(credentialsProviderChain);
    }

    public DotAmazonS3Client(DefaultAWSCredentialsProviderChain credentialsProviderChain, ClientConfiguration conf ) {
        super(credentialsProviderChain, conf);
    }

    public DotAmazonS3Client(BasicAWSCredentials basicAWSCredentials) {
        super(basicAWSCredentials);
    }

    public DotAmazonS3Client(BasicAWSCredentials awsCredentials, ClientConfiguration conf) {
        super(awsCredentials, conf);
    }

    @Override
    public HeadBucketResult headBucket(HeadBucketRequest headBucketRequest)
            throws AmazonClientException {
        return super.headBucket(headBucketRequest);
    }

}
