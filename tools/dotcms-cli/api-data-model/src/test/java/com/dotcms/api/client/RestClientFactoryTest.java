//package com.dotcms.api.client;
//
//
//import com.dotcms.api.AuthenticationAPI;
//import com.dotcms.model.ResponseEntityView;
//import com.dotcms.model.authentication.APITokenRequest;
//import com.dotcms.model.authentication.TokenEntity;
//import com.dotcms.model.config.ServiceBean;
//import io.quarkus.test.junit.QuarkusTest;
//import io.restassured.path.json.JsonPath;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.auth.impl.jose.JWT;
//import java.io.IOException;
//import javax.inject.Inject;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//@QuarkusTest
//class RestClientFactoryTest {
//
//    @Inject
//    RestClientFactory apiClientFactory;
//
//    @Inject
//    ServiceManager serviceManager;
//
//    @BeforeEach
//    public void setupTest() throws IOException {
//        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
//    }
//
//    @Test
//    public void Config_Test() {
//        final AuthenticationAPI authenticationApi = apiClientFactory.getClient(AuthenticationAPI.class);
//        APITokenRequest request = APITokenRequest.builder().user("admin@DOTcms.com").password("admin".toCharArray()).expirationDays(1).build();
//        ResponseEntityView<TokenEntity> tokenResponse = authenticationApi.getToken(request);
//        JsonObject token = JWT.parse(new String(tokenResponse.entity().token()));
//        JsonPath path = JsonPath.from(token.encode());
//        Assertions.assertEquals("HS256", path.getString("header.alg"));
//    }
//}