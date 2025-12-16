package com.dotcms.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests to enforce coding standards and architectural rules across the dotCMS codebase.
 *
 * These tests run at compile time and fail the build if any violations are detected.
 * They help maintain code quality and prevent the introduction of problematic patterns.
 */
public class CodingStandardsArchTest {

    /**
     * Import all production classes from com.dotcms and com.dotmarketing packages.
     * Excludes test classes to focus on production code.
     * Note: Classes are loaded lazily - only analyzed when rules are checked.
     */
    private JavaClasses getProductionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages("com.dotcms..", "com.dotmarketing..");
    }

    /**
     * Verifies that no code uses internal Jersey APIs from org.glassfish.jersey.internal package.
     *
     * <p>Rationale: Jersey internal APIs are implementation details that:
     * <ul>
     *   <li>May change without notice between versions</li>
     *   <li>Are not part of the public API contract</li>
     *   <li>Can cause runtime issues after Jersey upgrades</li>
     * </ul>
     *
     * <p>Instead, use:
     * <ul>
     *   <li>Public Jersey APIs from org.glassfish.jersey.* (excluding .internal)</li>
     *   <li>Standard Java APIs like java.util.Base64 instead of org.glassfish.jersey.internal.util.Base64</li>
     *   <li>JAX-RS standard APIs from javax.ws.rs.*</li>
     * </ul>
     *
     * <p>Example violation and fix:
     * <pre>
     * // ❌ WRONG - uses internal API
     * import org.glassfish.jersey.internal.util.Base64;
     * String encoded = Base64.encodeAsString("data");
     *
     * // ✅ CORRECT - uses standard Java API
     * import java.util.Base64;
     * String encoded = Base64.getEncoder().encodeToString("data".getBytes());
     * </pre>
     *
     * @see <a href="https://github.com/dotCMS/core/issues/33857">Issue #33857</a>
     */
    @Test
    public void shouldNotUseJerseyInternalAPIs() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("org.glassfish.jersey.internal..")
                .because("Jersey internal APIs are not part of the public contract and may change without notice. "
                        + "Use public Jersey APIs (org.glassfish.jersey.*) or standard Java APIs instead. "
                        + "For example, replace org.glassfish.jersey.internal.util.Base64 with java.util.Base64.");

        rule.check(getProductionClasses());
    }
}