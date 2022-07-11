package com.dotcms.apl.client;


import com.dotcms.api.AuthenticationAPI;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.jose.JWT;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RestClientFactoryTest {

    @Inject
    RestClientFactory apiClientFactory;

    @Test
    public void Config_Test() {
        final AuthenticationAPI authenticationApi = apiClientFactory.getClient(AuthenticationAPI.class);
        APITokenRequest request = APITokenRequest.builder().user("admin@dotcms.com").password("admin").expirationDays(1).build();
        ResponseEntityView<TokenEntity> tokenResponse = authenticationApi.getToken(request);
        JsonObject token = JWT.parse(tokenResponse.entity().token());
        System.out.println("token="+token.encodePrettily());
        JsonPath path = JsonPath.from(token.encode());
        Assertions.assertEquals("HS256", path.getString("header.alg"));
    }
}