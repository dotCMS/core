//package com.dotcms;
//
//import com.dotcms.api.AssetAPI;
//import com.dotcms.api.AuthenticationContext;
//import com.dotcms.api.client.RestClientFactory;
//import com.dotcms.api.client.ServiceManager;
//import com.dotcms.model.asset.SearchByPathRequest;
//import com.dotcms.model.config.ServiceBean;
//import io.quarkus.test.common.QuarkusTestResource;
//import io.quarkus.test.junit.QuarkusTest;
//import org.eclipse.microprofile.config.inject.ConfigProperty;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import javax.inject.Inject;
//import javax.ws.rs.NotFoundException;
//import java.io.IOException;
//
//@QuarkusTest
//@QuarkusTestResource(ContainerResource.class)
//public class ContainerTest {
//
//    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
//    String siteName;
//
//    @Inject
//    AuthenticationContext authenticationContext;
//
//    @Inject
//    RestClientFactory clientFactory;
//
//    @Inject
//    ServiceManager serviceManager;
//
//    @BeforeEach
//    public void setupTest() throws IOException {
//        serviceManager.removeAll().persist(
//                ServiceBean.builder().
//                        name("default").
//                        active(true).
//                        build()
//        );
//
//        final String user = "admin@dotcms.com";
//        final char[] passwd = "admin".toCharArray();
//        authenticationContext.login(user, passwd);
//    }
//
//    /**
//     * Check for the proper response when a folder is not found
//     */
//    @Test
//    void Test_Asset_By_Path_Not_Found() {
//
//        final AssetAPI assetAPI = clientFactory.getClient(AssetAPI.class);
//
//        var folderByPath = SearchByPathRequest.builder().
//                assetPath(String.format("//%s/%s", siteName, "folderDoesNotExist")).build();
//
//        try {
//            assetAPI.folderByPath(folderByPath);
//            Assertions.fail(" 404 Exception should have been thrown here.");
//        } catch (Exception e) {
//            e.printStackTrace();
//            Assertions.assertTrue(e instanceof NotFoundException);
//        }
//    }
//
//    @Test
//    void test_docker_containers_up() {
//        System.out.println("test_docker_containers_up");
//    }
//}
