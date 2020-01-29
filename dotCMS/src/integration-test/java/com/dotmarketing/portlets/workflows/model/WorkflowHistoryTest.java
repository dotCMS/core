package com.dotmarketing.portlets.workflows.model;

import com.liferay.util.StringPool;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class WorkflowHistoryTest {

    @Test
    public void test_getChangeMap_null_description ()  {

        final WorkflowHistory workflowHistory = new WorkflowHistory();
        workflowHistory.setChangeDescription(null);
        final Map<String, Object> changeMap = workflowHistory.getChangeMap();

        Assert.assertNotNull("GetChangeMap must not return a null map",changeMap);
        Assert.assertEquals("Should return a blank description", changeMap.get("description"), StringPool.BLANK);
        Assert.assertEquals("Should return a comment type", changeMap.get("type"), WorkflowHistoryType.COMMENT.name());
        Assert.assertEquals("Should return a comment type", changeMap.get("state"), WorkflowHistoryState.NONE.name());
    }

    @Test
    public void test_getChangeMap_with_description ()  {

        final WorkflowHistory workflowHistory = new WorkflowHistory();
        workflowHistory.setChangeDescription("test");
        final Map<String, Object> changeMap = workflowHistory.getChangeMap();

        Assert.assertNotNull("GetChangeMap must not return a null map",changeMap);
        Assert.assertEquals("Should return test description", workflowHistory.getChangeDescription(), "test");
        Assert.assertEquals("Should return test description", changeMap.get("description"), "test");
        Assert.assertEquals("Should return a comment type", changeMap.get("type"), WorkflowHistoryType.COMMENT.name());
        Assert.assertEquals("Should return a comment type", changeMap.get("state"), WorkflowHistoryState.NONE.name());
    }

    @Test
    public void test_getChangeMap_approval_type ()  {

        final WorkflowHistory workflowHistory = new WorkflowHistory();
        workflowHistory.setChangeDescription("{'description':'test', 'type':'" + WorkflowHistoryType.APPROVAL.name() + "', 'state':'"+  WorkflowHistoryState.NONE.name() +"' }");
        final Map<String, Object> changeMap = workflowHistory.getChangeMap();

        Assert.assertNotNull("GetChangeMap must not return a null map",changeMap);
        Assert.assertEquals("Should return test description", workflowHistory.getChangeDescription(), "test");
        Assert.assertEquals("Should return test description", changeMap.get("description"), "test");
        Assert.assertEquals("Should return approval type", changeMap.get("type"), WorkflowHistoryType.APPROVAL.name());
        Assert.assertEquals("Should return a comment type", changeMap.get("state"), WorkflowHistoryState.NONE.name());
    }
}
