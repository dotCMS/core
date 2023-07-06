package com.dotmarketing.microprofile.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.io.File;

@ConfigMapping(prefix = "dot")
public interface PathsBean {


    @WithDefault("${dot.test-runtime-root}/dotsecure")
    File dynamicContentPath();

    @WithDefault("${user.dir}/build")
    File buildOutputPath();

    @WithDefault("${dot.build-output-path}/test-runtime")
    File testRuntimeRoot();

    @WithDefault("${dot.test-runtime-root}/context-root")
    File contextRootPath();

    @WithDefault("${dot.test-runtime-root}/assets")
    File assetRealPath();




}
