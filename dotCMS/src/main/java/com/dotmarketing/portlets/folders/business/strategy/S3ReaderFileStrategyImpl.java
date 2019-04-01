package com.dotmarketing.portlets.folders.business.strategy;

import com.dotcms.repackage.com.amazonaws.services.s3.AmazonS3Client;
import com.dotcms.repackage.com.amazonaws.services.s3.model.GetObjectRequest;
import com.dotcms.repackage.com.amazonaws.services.s3.model.S3Object;
import io.vavr.Tuple3;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class S3ReaderFileStrategyImpl implements ReaderFileStrategy {

    @Override
    public boolean test(final String file) {
        return null != file && file.toLowerCase().startsWith(S3_SYSTEM_PREFIX);
    }

    @Override
    public Reader apply(final String file) throws IOException {


        final Tuple3<AmazonS3Client, String, String> clientBuckerAndKey =
                this.getAmazonClientBucketAndKey (file);
        final String    key         = clientBuckerAndKey._3;
        final String    bucketName  = clientBuckerAndKey._2;
        final AmazonS3Client client = clientBuckerAndKey._1;
        final S3Object fullObject   = client.getObject(new GetObjectRequest(bucketName, key));

        return new InputStreamReader(fullObject.getObjectContent());
    }

    private Tuple3<AmazonS3Client, String, String> getAmazonClientBucketAndKey(final String file) {
        return null; // todo: implement me
    }

    @Override
    public Source source() {
        return Source.S3;
    }
}
