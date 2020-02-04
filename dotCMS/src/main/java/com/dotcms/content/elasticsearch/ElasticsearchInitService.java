//package com.dotcms.content.elasticsearch;
//
//import com.dotcms.config.DotInitializer;
//import com.rainerhahnekamp.sneakythrow.Sneaky;
//import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
//import pl.allegro.tech.embeddedelasticsearch.PopularProperties;
//
//public class ElasticsearchInitService implements DotInitializer {
//
//    @Override
//    public void init() {
////        Sneaky.sneak(()->EmbeddedElastic.builder()
////                .withElasticVersion("6.7.1")
////                .withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9301)
////                .withSetting(PopularProperties.CLUSTER_NAME, "testDotCMSCluster")
////                .withSetting("http.port", 9201)
////                .withSetting("http.enabled", true)
////                .build()
////                .start());
//    }
//}