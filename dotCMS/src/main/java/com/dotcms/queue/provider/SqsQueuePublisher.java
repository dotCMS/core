package com.dotcms.queue.provider;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.dotcms.queue.DotQueueException;
import com.dotcms.queue.DotQueuePublisher;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link DotQueuePublisher} backed by AWS SQS. Uses the default AWS credentials
 * chain (IAM roles, env vars, instance profiles) — no hardcoded credentials.
 *
 * <p>Configuration:
 * <ul>
 *   <li>{@code DOT_QUEUE_SQS_REGION} — AWS region (default: {@code us-east-1})</li>
 *   <li>{@code DOT_QUEUE_SQS_ENDPOINT} — optional custom endpoint for LocalStack/dev;
 *       omit in production so the SDK uses the standard AWS endpoint</li>
 *   <li>{@code DOT_QUEUE_SQS_URL_<QUEUE_NAME>} — maps a logical queue name to its
 *       SQS queue URL (e.g. {@code DOT_QUEUE_SQS_URL_ANALYTICS_EVENTS})</li>
 * </ul>
 */
public final class SqsQueuePublisher implements DotQueuePublisher {

    static final String REGION_PROP = "DOT_QUEUE_SQS_REGION";
    static final String ENDPOINT_PROP = "DOT_QUEUE_SQS_ENDPOINT";
    static final String QUEUE_URL_PREFIX = "DOT_QUEUE_SQS_URL_";

    private volatile AmazonSQS client;

    @Override
    public void publish(final String queueName,
                        final String messageBody,
                        @Nullable final Map<String, String> attributes) {

        if (!UtilMethods.isSet(queueName)) {
            throw new DotQueueException("queueName must not be null or empty");
        }
        if (!UtilMethods.isSet(messageBody)) {
            throw new DotQueueException("messageBody must not be null or empty");
        }

        final String queueUrl = resolveQueueUrl(queueName);

        final Map<String, MessageAttributeValue> sqsAttributes = new HashMap<>();
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (UtilMethods.isSet(key) && UtilMethods.isSet(value)) {
                    sqsAttributes.put(key, new MessageAttributeValue()
                            .withDataType("String")
                            .withStringValue(value));
                }
            });
        }

        final SendMessageRequest request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageAttributes(sqsAttributes);

        try {
            getClient().sendMessage(request);
            Logger.debug(SqsQueuePublisher.class,
                    () -> "Published message to SQS queue '" + queueName + "' at " + queueUrl);
        } catch (final Exception e) {
            throw new DotQueueException(
                    "Failed to publish message to SQS queue '" + queueName + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable(final String queueName) {
        try {
            if (!UtilMethods.isSet(queueName)) {
                return false;
            }
            final String configKey = QUEUE_URL_PREFIX + queueName.toUpperCase();
            return UtilMethods.isSet(Config.getStringProperty(configKey, ""))
                    && getClient() != null;
        } catch (final Exception e) {
            Logger.debug(SqsQueuePublisher.class,
                    () -> "SQS publisher not available for queue '" + queueName + "': " + e.getMessage());
            return false;
        }
    }

    private String resolveQueueUrl(final String queueName) {
        final String configKey = QUEUE_URL_PREFIX + queueName.toUpperCase();
        final String url = Config.getStringProperty(configKey, "");
        if (!UtilMethods.isSet(url)) {
            throw new DotQueueException(
                    "No SQS queue URL configured for queue '" + queueName
                            + "'. Set property '" + configKey + "'.");
        }
        return url;
    }

    private AmazonSQS getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    final String region = Config.getStringProperty(REGION_PROP, "us-east-1");
                    final String endpoint = Config.getStringProperty(ENDPOINT_PROP, "");

                    final AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard()
                            .withCredentials(new DefaultAWSCredentialsProviderChain());

                    if (UtilMethods.isSet(endpoint)) {
                        builder.withEndpointConfiguration(
                                new EndpointConfiguration(endpoint, region));
                    } else {
                        builder.withRegion(region);
                    }

                    client = builder.build();

                    Logger.info(SqsQueuePublisher.class,
                            "Initialized SQS client — region: " + region
                                    + (UtilMethods.isSet(endpoint) ? ", endpoint: " + endpoint : ""));
                }
            }
        }
        return client;
    }
}
