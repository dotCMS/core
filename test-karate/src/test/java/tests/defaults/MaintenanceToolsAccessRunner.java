package tests.defaults;

import com.intuit.karate.junit5.Karate;

public class MaintenanceToolsAccessRunner {

    @Karate.Test
    Karate testMaintenanceToolsAccess() {
        return Karate.run("MaintenanceToolsAccess").relativeTo(getClass());
    }

}
