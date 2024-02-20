package com.dotcms;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DotCMSITProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.emptyMap();
    }


    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Collections.emptySet();
    }


    @Override
    public String getConfigProfile() {
        return "test";
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.singletonList(new TestResourceEntry(ContainerResource.class));
    }

    @Override
    public boolean disableGlobalTestResources() {
        return false;
    }

    /**
     * The tags this profile is associated with.
     * When the {@code quarkus.test.profile.tags} System property is set (its value is a comma separated list of strings)
     * then Quarkus will only execute tests that are annotated with a {@code @TestProfile} that has at least one of the
     * supplied (via the aforementioned system property) tags.
     */
    @Override
    public Set<String> tags() {
        return Set.of("integration");
    }

    /**
     * The command line parameters that are passed to the main method on startup.
     */
    @Override
    public String[] commandLineParameters() {
        return new String[0];
    }

    /**
     * If the main method should be run.
     */
    @Override
    public boolean runMainMethod() {
        return false;
    }

    /**
     * If this method returns true then all {@code StartupEvent} and {@code ShutdownEvent} observers declared on application
     * beans should be disabled.
     */
    @Override
    public boolean disableApplicationLifecycleObservers() {
        return false;
    }
}

