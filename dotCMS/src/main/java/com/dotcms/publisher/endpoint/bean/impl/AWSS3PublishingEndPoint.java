package com.dotcms.publisher.endpoint.bean.impl;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Configuration;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3EndPointPublisher;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.PublishingEndPointValidationException;
import com.dotmarketing.exception.PublishingEndPointValidationException.Builder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;

/**
 * Implementation of {@link PublishingEndPoint} for fancy AWS S3 Publish.
 */
public class AWSS3PublishingEndPoint extends PublishingEndPoint {

    @Override
    public Class getPublisher() {
        return AWSS3Publisher.class;
    }// getPublisher.

    @Override
    public void validatePublishingEndPoint() throws PublishingEndPointValidationException {

        if (!UtilMethods.isSet(getAuthKey().toString())){
            throw new PublishingEndPointValidationException(
                "publisher_Endpoint_awss3_authKey_missing_properties");
        }

        // Parse AWS S3 properties
        Properties props = new Properties();
        try {
            final String decryptString = PublicEncryptionFactory
                    .decryptString(getAuthKey().toString());
            props.load(new StringReader(decryptString));
        } catch (IOException e) {
            throw new PublishingEndPointValidationException(
                    "publisher_Endpoint_awss3_authKey_format_invalid");
        }

        final Builder publishingEndPointValidationExceptionBuilder = new PublishingEndPointValidationException.Builder();

        // Validate provision of all mandatory AWS S3 properties
        String token = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_TOKEN);
        String secret = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_SECRET);
        String bucketID = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_ID);
        String bucketValidationName = props
                .getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_VALIDATION_NAME);
        String s3Endpoint = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_ENDPOINT);
        String s3Region = props.getProperty(AWSS3Publisher.DOTCMS_PUSH_AWS_S3_BUCKET_REGION);


        try {
            
            if (!UtilMethods.isSet(bucketID)) {
               throw new DotRuntimeException("Bucket ID must be set");
            }
            
            if (!UtilMethods.isSet(token) || !UtilMethods.isSet(secret)) {
    
                    // Validate DefaultAWSCredentialsProviderChain configuration
                    DefaultAWSCredentialsProviderChain creds = new DefaultAWSCredentialsProviderChain();
                    new AWSS3EndPointPublisher(creds).checkConnectSuccessfully(bucketValidationName);
    
            } else {
                    // Validate correctness of AWS S3 connection properties
                    AWSS3Configuration awss3Config =
                            new AWSS3Configuration.Builder().accessKey(token).secretKey(secret)
                                    .endPoint(s3Endpoint).region(s3Region).build();
                    new AWSS3EndPointPublisher(awss3Config).checkConnectSuccessfully(
                            bucketValidationName);
    
            }
            // we do not throw an exception here, instead we save config, log the exception and raise an error to 
            // to the user in the UI.
        } catch (Exception e) {
            Logger.warn(AWSS3PublishingEndPoint.class, e.getMessage(),e);
            final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
            SystemMessage systemMessage = systemMessageBuilder.setMessage("Unable to verify S3 Endpoint. Please check your configuration:" + e.getMessage()).setType(MessageType.SIMPLE_MESSAGE)
                            .setSeverity(MessageSeverity.WARNING).setLife(100000).create();

            String userId = Try.of(()->PortalUtil.getUser().getUserId()).getOrNull();
            
            if (userId != null) {
                SystemMessageEventUtil.getInstance().pushMessage(systemMessage, List.of(userId));
            }

        }

    } //validatePublishingEndPoint.

}
