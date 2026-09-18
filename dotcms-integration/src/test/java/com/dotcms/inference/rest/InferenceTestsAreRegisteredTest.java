package com.dotcms.inference.rest;

import com.dotcms.MainSuite2a;
import org.junit.Test;
import org.junit.runners.Suite;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Fails if an integration test in this family is not registered in a suite CI actually runs.
 *
 * <p>A test class that no {@code MainSuite} lists compiles, passes locally under
 * {@code -Dit.test=}, and is <strong>never executed in CI</strong>. There is no error, no warning
 * and no coverage — the build stays green and the class silently protects nothing. Three classes
 * in this family were in exactly that state and it was caught by eye, not by tooling; at least one
 * class elsewhere in the repo still is.</p>
 *
 * <p>This guard exists because the failure mode is invisible by construction: the only thing that
 * would notice an unregistered test is another test. It reads the suite's own registration list
 * rather than a copy, so it cannot drift from what CI runs, and it discovers test classes from the
 * compiled output rather than a hand-maintained list, so a class added tomorrow is covered without
 * anyone remembering this file exists.</p>
 */
public class InferenceTestsAreRegisteredTest {

    /**
     * Given every compiled test class in this package
     * When the suite CI runs is inspected
     * Then each one is registered in it
     */
    @Test
    public void test_everyInferenceTestClassIsRegisteredInTheSuite() throws Exception {
        final Set<String> registered =
                Arrays.stream(MainSuite2a.class.getAnnotation(Suite.SuiteClasses.class).value())
                        .map(Class::getName)
                        .collect(Collectors.toSet());

        final List<String> unregistered = new ArrayList<>();
        for (final Class<?> testClass : compiledTestClassesInThisPackage()) {
            final boolean hasTests = Arrays.stream(testClass.getDeclaredMethods())
                    .anyMatch(method -> method.isAnnotationPresent(Test.class));
            if (hasTests && !registered.contains(testClass.getName())) {
                unregistered.add(testClass.getSimpleName());
            }
        }

        assertTrue("These test classes are never run by CI — they compile, they pass when named "
                        + "explicitly, and the build stays green while they protect nothing. Add "
                        + "them to MainSuite2a's @SuiteClasses: " + unregistered,
                unregistered.isEmpty());
    }

    /**
     * @return every compiled class in this package, read from the build output rather than a list
     *         someone has to remember to update
     */
    private List<Class<?>> compiledTestClassesInThisPackage() throws Exception {
        final String packagePath = getClass().getPackageName().replace('.', '/');
        final URL location = getClass().getClassLoader().getResource(packagePath);
        assertNotNull("Could not locate the compiled test classes; this guard would silently "
                + "pass without checking anything", location);

        final File[] files = new File(location.toURI()).listFiles(
                (dir, name) -> name.endsWith("Test.class") && !name.contains("$"));
        assertNotNull(files);
        assertTrue("No compiled test classes found — the guard is not actually looking at "
                + "anything", files.length > 0);

        final List<Class<?>> classes = new ArrayList<>(files.length);
        for (final File file : files) {
            final String simpleName = file.getName().replace(".class", "");
            classes.add(Class.forName(getClass().getPackageName() + "." + simpleName));
        }
        return classes;
    }
}
