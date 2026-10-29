package tests.defaults;

import com.intuit.karate.junit5.Karate;

public class WorkflowFirePermissionListRunner {

    @Karate.Test
    Karate testWorkflowFirePermissionList() {
        return Karate.run("WorkflowFirePermissionList").relativeTo(getClass());
    }

}
