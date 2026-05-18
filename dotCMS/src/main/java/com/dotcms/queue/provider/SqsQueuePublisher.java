package com.dotcms.queue.provider;

import com.dotcms.queue.DotQueueException;
import com.dotcms.queue.DotQueuePublisher;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@link DotQueuePublisher} backed by AWS SQS (SDK v2). Uses the default AWS
 * credentials chain (IAM roles, env vars, instance profiles) — no hardcoded
 * credentials.
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

    private volatile SqsClient client;

    @Override
    public void publish(final String queueName,
                        final String messageBody,
                        @Nullable final Map<String, String> attributes) {

        if (queueName == null || queueName.isEmpty()) {
            throw new DotQueueException("queueName must not be null or empty");
        }
        if (messageBody == null || messageBody.isEmpty()) {
            throw new DotQueueException("messageBody must not be null or empty");
        }

        final String queueUrl = resolveQueueUrl(queueName);

        final Map<String, MessageAttributeValue> sqsAttributes = new HashMap<>();
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (UtilMethods.isSet(key) && UtilMethods.isSet(value)) {
                    sqsAttributes.put(key, MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(value)
                            .build());
                }
            });
        }

        final SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .messageAttributes(sqsAttributes)
                .build();

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
        if (queueName == null || queueName.isEmpty()) {
            return false;
        }
        final String configKey = QUEUE_URL_PREFIX + queueName.toUpperCase(Locale.ROOT);
        return UtilMethods.isSet(Config.getStringProperty(configKey, ""));
    }

    private String resolveQueueUrl(final String queueName) {
        final String configKey = QUEUE_URL_PREFIX + queueName.toUpperCase(Locale.ROOT);
        final String url = Config.getStringProperty(configKey, "");
        if (!UtilMethods.isSet(url)) {
            throw new DotQueueException(
                    "No SQS queue URL configured for queue '" + queueName
                            + "'. Set property '" + configKey + "'.");
        }
        return url;
    }

    private SqsClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    final String region = Config.getStringProperty(REGION_PROP, "us-east-1");
                    final String endpoint = Config.getStringProperty(ENDPOINT_PROP, "");

                    final SqsClientBuilder builder = SqsClient.builder()
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .region(Region.of(region));

                    if (UtilMethods.isSet(endpoint)) {
                        builder.endpointOverride(URI.create(endpoint));
                    }

                    client = builder.build();

                    Logger.info(SqsQueuePublisher.class,
                            "Initialized SQS client (SDK v2) — region: " + region
                                    + (UtilMethods.isSet(endpoint) ? ", endpoint: " + endpoint : ""));
                }
            }
        }
        return client;
    }
}
