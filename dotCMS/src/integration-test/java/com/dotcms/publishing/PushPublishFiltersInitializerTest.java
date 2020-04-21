package com.dotcms.publishing;

import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.YamlUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PushPublishFiltersInitializerTest {

    private static PushPublishFiltersInitializer pushPublishFiltersInitializer;
    private static File path;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        final String tmpdirPath = System.getProperty("java.io.tmpdir");
        path = new File(tmpdirPath + File.separator + "filters");
        if (!path.exists()) {
            path.mkdir();
        }
        pushPublishFiltersInitializer = new PushPublishFiltersInitializer();
    }

    private void createFilterFile(final FilterDescriptor filterDescriptor){
        Logger.info(this,"PATH" + path.toString());
        final File file = new File(path.toString(),filterDescriptor.getKey());
        YamlUtil.write(file,filterDescriptor);
    }

    @Test
    public void test_loadFilter() throws IOException {
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",true,"relationships",true,"excludeClasses","Host,Workflow");
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor("filterTest.yml","Filter Test Title",filtersMap,true,"Reviewer,dotcms.org.2789");
        createFilterFile(filterDescriptor);

        Stream<Path> pathStream = Files.list(path.toPath());

        pathStream.forEach(path1 -> pushPublishFiltersInitializer.loadFilter(path1));

        final Map<String,FilterDescriptor> filterDescriptorMap = APILocator.getPublisherAPI().getFilterDescriptorMap();
        Assert.assertFalse(filterDescriptorMap.isEmpty());
        Assert.assertTrue(filterDescriptorMap.containsKey(filterDescriptor.getKey()));
    }

}
