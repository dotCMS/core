package com.dotcms.publishing;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.YamlUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    /**
     * Method to test: {@link PushPublishFiltersInitializer#loadFilter(Path)}
     * Given Scenario: Given a yaml file that contains a FilterDescriptor, the initializer reads the file and loads the filter
     * ExpectedResult: filter is successfully added to the filterDescriptorMap
     *
     */
    @Test
    public void test_loadFilter_success() throws IOException, DotDataException {
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",true,"relationships",true,"excludeClasses","Host,Workflow");
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor("filterTest.yml","Filter Test Title",filtersMap,true,"Reviewer,dotcms.org.2789");
        createFilterFile(filterDescriptor);

        Stream<Path> pathStream = Files.list(path.toPath());

        pathStream.forEach(path1 -> pushPublishFiltersInitializer.loadFilter(path1));

        final List<FilterDescriptor> filterDescriptorList = APILocator.getPublisherAPI().getFiltersDescriptorsByRole(
                TestUserUtils.getAdminUser());
        Assert.assertFalse(filterDescriptorList.isEmpty());
        Assert.assertTrue(filterDescriptorList.stream().anyMatch(filter -> filter.getKey().equalsIgnoreCase(filterDescriptor.getKey())));
    }

    /**
     * Method to test: {@link PushPublishFiltersInitializer#loadFilter(Path)}
     * Given Scenario: Given 2 yaml files, one without any error and one empty, the initializer reads both files and only loads the one without errors
     * ExpectedResult: filter without errors is successfully added to the filterDescriptorMap
     *
     */
    @Test
    public void test_loadFilter_filterWithAnError_otherFiltersLoadSuccessfully() throws IOException, DotDataException {
        //YAML file without issues
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",true,"relationships",true,"excludeClasses","Host,Workflow");
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor("filterTestWithoutAnError.yml","Filter Test Title",filtersMap,true,"Reviewer,dotcms.org.2789");
        createFilterFile(filterDescriptor);

        // Bad YAML file, it's empty
        final File file = File.createTempFile("filterTestWithoutAnError", ".yml",path);
        FileUtil.write(file, "");

        Files.list(path.toPath()).forEach(path1 -> pushPublishFiltersInitializer.loadFilter(path1));

        final List<FilterDescriptor> filterDescriptorList = APILocator.getPublisherAPI().getFiltersDescriptorsByRole(
                TestUserUtils.getAdminUser());
        Assert.assertFalse(filterDescriptorList.isEmpty());
        Assert.assertTrue(filterDescriptorList.stream().anyMatch(filter -> filter.getKey().equalsIgnoreCase(filterDescriptor.getKey())));
    }

}
