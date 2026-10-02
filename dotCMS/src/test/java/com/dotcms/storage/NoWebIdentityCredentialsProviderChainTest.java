package com.dotcms.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.dotmarketing.util.Config;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoWebIdentityCredentialsProviderChainTest {
    private String previousFlag;

    @BeforeEach
    void remember() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
    }

    @AfterEach
    void restore() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @Test
    void disabledFlagResolvesCredentialsWithoutTheWebIdentityStep() {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        final DefaultAWSCredentialsProviderChain chain = NoWebIdentityCredentialsProviderChain.defaultChain();
        final var legacy = assertInstanceOf(NoWebIdentityCredentialsProviderChain.class, chain);
        // SDK 1.12.488 orders the default chain as environment, system properties, web identity,
        // profile, container or instance role. Without STS on the classpath the web identity step
        // always failed over, so the effective chain is the other four in the same order.
        assertEquals(List.of(EnvironmentVariableCredentialsProvider.class,
                        SystemPropertiesCredentialsProvider.class,
                        ProfileCredentialsProvider.class,
                        EC2ContainerCredentialsProviderWrapper.class),
                legacy.providers().stream().map(Object::getClass).toList());
    }

    @Test
    void enabledFlagUsesTheStandardSdkChain() {
        Config.setProperty(AssetStorageFeature.FLAG, true);
        assertSame(DefaultAWSCredentialsProviderChain.class,
                NoWebIdentityCredentialsProviderChain.defaultChain().getClass());
    }
}
