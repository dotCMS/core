package com.dotcms.architecture;

import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotcms.rest.WebResource;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
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

    @Test
    public void indexLibraryIndependent(){
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(IndexLibraryIndependent.class)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.elasticsearch..",
                        "org.opensearch..",
                        "co.elastic.clients.."
                )
                .because("Classes annotated with @IndexLibraryIndependent must not depend on specific " +
                        "search engine libraries (Elasticsearch, OpenSearch) to maintain abstraction and portability");
        rule.check(getProductionClasses());
    }

    /**
     * Enforces that the servlet-only {@code WebResource.authenticate(...)} /
     * {@code WebResource.getCurrentUser(...)} overloads which take a {@code boolean} anonymous-fallback
     * flag are called only from the static/binary asset-servlet helper in
     * {@code com.dotmarketing.servlets} (i.e. {@code ServletUtils}).
     *
     * <p>Rationale: those overloads downgrade an authentication failure to anonymous access so that
     * an upstream Basic-Auth gating layer (whose credentials the browser replays on sub-resource
     * requests, per RFC 7617) does not break anonymously-readable assets — see
     * <a href="https://github.com/dotCMS/core/issues/35536">#35536</a>. A JAX-RS REST endpoint
     * calling them would silently convert a {@code 401} (invalid credentials) into anonymous data
     * access. The capability is intentionally not exposed via {@code AuthCheckOptions} or
     * {@code InitBuilder}; this rule is the structural guard against the remaining public overload.</p>
     *
     * <p>The two target overloads are identified by carrying both a {@code boolean} parameter and an
     * {@code AuthCheckOptions[]} (varargs) parameter, which distinguishes them from the legacy
     * {@code rejectWhenNoUser} boolean overloads. {@code WebResource} itself is excluded because its
     * public overloads delegate to these with {@code false}.</p>
     */
    @Test
    public void shouldNotCallServletAnonymousFallbackOverloadsOutsideAssetServlets() {

        final DescribedPredicate<JavaMethodCall> servletFallbackOverloadCall =
                new DescribedPredicate<JavaMethodCall>(
                        "a WebResource.authenticate/getCurrentUser overload taking a boolean anonymous-fallback flag") {
                    @Override
                    public boolean test(final JavaMethodCall call) {
                        return call.getTarget().getOwner().isEquivalentTo(WebResource.class)
                                && ("authenticate".equals(call.getTarget().getName())
                                        || "getCurrentUser".equals(call.getTarget().getName()))
                                && call.getTarget().getRawParameterTypes().stream()
                                        .anyMatch(p -> p.isEquivalentTo(boolean.class))
                                && call.getTarget().getRawParameterTypes().stream()
                                        .anyMatch(p -> p.isEquivalentTo(WebResource.AuthCheckOptions[].class));
                    }
                };

        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("com.dotmarketing.servlets..")
                .and(not(equivalentTo(WebResource.class)))
                .should().callMethodWhere(servletFallbackOverloadCall)
                .because("The anonymous-fallback authenticate/getCurrentUser overloads downgrade an "
                        + "authentication failure to anonymous access and must only be used by the "
                        + "static/binary asset servlets (com.dotmarketing.servlets). A REST endpoint "
                        + "calling them would silently turn a 401 into anonymous data access (#35536).");

        rule.check(getProductionClasses());
    }
}