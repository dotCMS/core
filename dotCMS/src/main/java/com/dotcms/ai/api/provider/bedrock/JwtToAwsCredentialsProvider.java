package com.dotcms.ai.api.provider.bedrock;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResponse;
import software.amazon.awssdk.services.sts.model.StsException;

import java.time.Instant;

/**
 * The intention of this class is to use aws authentication based on jwt
 * @author jsanca
 */
public class JwtToAwsCredentialsProvider implements AwsCredentialsProvider {

    private final String jwtToken;
    private final String roleArn;
    private final Region stsRegion;

    private volatile AwsSessionCredentials cachedCredentials;
    private volatile Instant expirationTime;
    private static final long REFRESH_BEFORE_SECONDS = 300; // 5 mins

    public JwtToAwsCredentialsProvider(final String jwtToken,
                                       final String roleArn, final Region stsRegion) {
        this.jwtToken = jwtToken;
        this.roleArn = roleArn;
        this.stsRegion = stsRegion;
    }

    /**
     * Resolve and returns the aws credentials
     * Uses cache to avoid to call STS in each request
     */
    @Override
    public AwsCredentials resolveCredentials() {

        // 1. Verificar si las credenciales en caché aún son válidas
        if (cachedCredentials != null && isNotExpired()) {
            return cachedCredentials;
        }

        // 2. if expired or null, get new credentails
        return getNewCredentialsFromSts();
    }

    private boolean isNotExpired() {
        // check if expires
        return expirationTime != null &&
                expirationTime.isAfter(Instant.now().plusSeconds(REFRESH_BEFORE_SECONDS));
    }

    private AwsCredentials getNewCredentialsFromSts() {
        try (StsClient stsClient = StsClient.builder().region(stsRegion).build()) {

            final AssumeRoleWithWebIdentityRequest request = AssumeRoleWithWebIdentityRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName("BedrockJwtSession") // Cloudtrail session name
                    .webIdentityToken(jwtToken)          //
                    .durationSeconds(3600)               // 1 time out for credentials
                    .build();

            final AssumeRoleWithWebIdentityResponse response = stsClient.assumeRoleWithWebIdentity(request);

            // 3. creates the AwsSessionCredentials from the STS response
            cachedCredentials = AwsSessionCredentials.builder()
                    .accessKeyId(response.credentials().accessKeyId())
                    .secretAccessKey(response.credentials().secretAccessKey())
                    .sessionToken(response.credentials().sessionToken())
                    .build();

            // 4. Stores the new expire date
            expirationTime = response.credentials().expiration();

            return cachedCredentials;

        } catch (StsException e) {
            throw new RuntimeException("Error assuming the rol with Web Identity: " + e.getMessage(), e);
        }
    }
}
