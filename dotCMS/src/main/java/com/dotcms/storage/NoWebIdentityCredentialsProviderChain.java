package com.dotcms.storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import java.util.List;

/**
 * The AWS SDK default credential chain without its web identity step: environment variables, Java
 * system properties, the shared profile files, then the container or EC2 instance role, in the
 * SDK's own order.
 *
 * <p>Before the S3 asset storage feature, dotCMS did not package the AWS STS module, so the web
 * identity step of {@link DefaultAWSCredentialsProviderChain} always failed and the chain moved on to
 * the next provider. With STS packaged, that step can now succeed, for example in a Kubernetes pod
 * that has a web identity token. With the feature flag off this chain keeps the earlier resolution.
 * It extends {@link DefaultAWSCredentialsProviderChain} so the public constructors that take that
 * type keep their signatures.</p>
 */
public final class NoWebIdentityCredentialsProviderChain extends DefaultAWSCredentialsProviderChain {

    private final List<AWSCredentialsProvider> providers = List.of(
            new EnvironmentVariableCredentialsProvider(),
            new SystemPropertiesCredentialsProvider(),
            new ProfileCredentialsProvider(),
            new EC2ContainerCredentialsProviderWrapper());
    private final AWSCredentialsProviderChain delegate = new AWSCredentialsProviderChain(providers);

    private NoWebIdentityCredentialsProviderChain() { }

    /**
     * Returns the credential chain to use where dotCMS falls back to the AWS default chain. With the
     * S3 asset storage flag on, this is the standard SDK chain, including web identity. With the flag
     * off, it is the chain without web identity, which resolves credentials the way dotCMS did before
     * the STS module was packaged.
     *
     * @return a new credential chain for the current mode
     */
    public static DefaultAWSCredentialsProviderChain defaultChain() {
        return AssetStorageFeature.isEnabled()
                ? new DefaultAWSCredentialsProviderChain()
                : new NoWebIdentityCredentialsProviderChain();
    }

    /**
     * Resolves credentials from the first provider in this chain that supplies them.
     *
     * @return the resolved credentials
     */
    @Override
    public AWSCredentials getCredentials() {
        return delegate.getCredentials();
    }

    /**
     * Asks every provider in this chain to refresh its credentials.
     */
    @Override
    public void refresh() {
        delegate.refresh();
    }

    /**
     * Returns the providers this chain consults, in order, so tests can check its composition.
     *
     * @return the providers in the order they are tried
     */
    List<AWSCredentialsProvider> providers() {
        return providers;
    }
}
