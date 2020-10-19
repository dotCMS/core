package com.dotmarketing.startup;

import static org.junit.Assert.assertEquals;

import com.dotcms.util.IntegrationTestInitService;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class StartupTasksExecutorTest {

    @DataProvider
    public static Object [] testCases() {
        return new TestCase []{
                new TestCase("201009", "Task201009TestMethod"),
                new TestCase("2010091", "Task2010091TestMethod"),
                new TestCase("05030", "Task05030TestMethod")
        };
    }

    private static class TestCase{
        String taskId;
        String taskName;

        public TestCase(final String taskId, final String taskName){
            this.taskId = taskId;
            this.taskName = taskName;
        }
    }

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link StartupTasksExecutor#getTaskId(String)}
     * Test case: A task name whose id has a specific length
     * Expected result: It should extract the length correctly
     * @param testCase
     */
    @Test
    @UseDataProvider("testCases")
    public void testGetTaskId(TestCase testCase){
        final String id = StartupTasksExecutor.getInstance().getTaskId(testCase.taskName);
        assertEquals(testCase.taskId, id);
    }

}
