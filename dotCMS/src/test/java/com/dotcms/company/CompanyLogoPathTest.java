package com.dotcms.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the rules that decide whether a stored company logo value is a dotAsset path,
 * and which path the logo is served from.
 */
class CompanyLogoPathTest {

    private static final String ID = "e9f32fc04a4d7a0e7e6a025ee65fcf30";

    /**
     * dotAsset paths in every shape the product stores are valid: the stock logo (identifier
     * without dashes), a migrated logo (UUID identifier), the short forms, image transformations,
     * and file names with the characters an uploaded file name may keep.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/dotCMS-400x200.png",
            "/dA/a083e942-5fb0-47ef-9619-6f8f40593474/asset/company_logo.png",
            "/dA/" + ID,
            "/dA/" + ID + "/asset",
            "/dA/" + ID + "/asset/200w/logo.png",
            "/dA/" + ID + "/.5,.5fp/logo.png",
            "/dA/" + ID + "/asset/my logo (1).png",
            "/dA/" + ID + "/asset/my%20logo.png",
            "/dA/" + ID + "/asset/my..logo.png",
            "/dA/" + ID + "/asset/O'Neil logo.png",
            "/dA/" + ID + "/asset/logo#2.png",
            "/dA/" + ID + "/asset/brand;v2.png",
            "/dA/" + ID + "/asset/50%.png",
            "/dA/" + ID + "/asset/a+b&c.png",
            "/dA/" + ID + "/asset/%zz.png"})
    void dotAssetPaths_areValid(final String path) {
        assertTrue(CompanyLogoPath.isValid(path), path);
    }

    /** A missing value is not a dotAsset path. */
    @Test
    void missingValue_isNotValid() {
        assertFalse(CompanyLogoPath.isValid(null));
        assertFalse(CompanyLogoPath.isValid(""));
    }

    /** Only paths under the /dA/ prefix, with its slash, are dotAsset paths. */
    @ParameterizedTest
    @ValueSource(strings = {"/dA", "/dAx/abc/logo.png", "/html/images/logo.png"})
    void pathsOutsideDotAssetPrefix_areNotValid(final String path) {
        assertFalse(CompanyLogoPath.isValid(path), path);
    }

    /** The segment after /dA/ must look like an identifier. */
    @ParameterizedTest
    @ValueSource(strings = {
            "/dA/",
            "/dA//asset/logo.png",
            "/dA/a b/logo.png",
            "/dA/..;/html/logo.png",
            "/dA/%2e%2e/html/logo.png"})
    void identifierSegmentWithOtherCharacters_isNotValid(final String path) {
        assertFalse(CompanyLogoPath.isValid(path), path);
    }

    /**
     * A path with a "." or ".." segment, literal, percent-encoded (also when another part of the
     * value is not a valid percent sequence), or followed by a ";" part, is not valid.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/dA/" + ID + "/../../html/logo.png",
            "/dA/" + ID + "/./logo.png",
            "/dA/" + ID + "/asset/.",
            "/dA/" + ID + "/%2e%2e/%2e%2e/html/logo.png",
            "/dA/" + ID + "%2f..%2f..%2fhtml/logo.png",
            "/dA/" + ID + "/..;x/..;x/html/logo.png",
            "/dA/" + ID + "/%2e%2e/%2e%2e/html/logo.png#%zz",
            "/dA/" + ID + "/.%2E/html/logo.png%",
            "/dA/" + ID + "/%2E./%2e/html/logo.png%",
            "/dA/" + ID + "/%2e%2e;x/html/logo.png%zz",
            "/dA/" + ID + "/%2e%2e%3bx/html/logo.png%zz",
            "/dA/" + ID + "/asset%2f%2e%2e%2fhtml/logo.png%zz"})
    void dotSegments_areNotValid(final String path) {
        assertFalse(CompanyLogoPath.isValid(path), path);
    }

    /** Characters an uploaded file name never keeps are not allowed. */
    @ParameterizedTest
    @ValueSource(strings = {
            "/dA/" + ID + "/asset\\logo.png",
            "/dA/" + ID + "/asset/logo.png?x=1",
            "/dA/" + ID + "/asset/lo\"go.png",
            "/dA/" + ID + "/asset/<logo>.png",
            "/dA/" + ID + "/asset/lo%22go.png"})
    void characterNeverKeptInFileNames_isNotValid(final String path) {
        assertFalse(CompanyLogoPath.isValid(path), path);
    }

    /** Control characters, literal or percent-encoded, are not allowed. */
    @ParameterizedTest
    @ValueSource(strings = {
            "/dA/" + ID + "/asset/lo\ngo.png",
            "/dA/" + ID + "/asset/lo\tgo.png",
            "/dA/" + ID + "/asset/lo%0ago.png"})
    void controlCharacters_areNotValid(final String path) {
        assertFalse(CompanyLogoPath.isValid(path), path);
    }

    /**
     * The logo is served from the stored path with each segment encoded, so the container reads
     * back exactly the segments that were checked.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/dotCMS-400x200.png | /dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/dotCMS-400x200.png",
            "/dA/" + ID + "/asset/200w/logo.png | /dA/" + ID + "/asset/200w/logo.png",
            "/dA/" + ID + "/.5,.5fp/logo.png | /dA/" + ID + "/.5,.5fp/logo.png",
            "/dA/" + ID + "/asset/my logo (1).png | /dA/" + ID + "/asset/my%20logo%20(1).png",
            "/dA/" + ID + "/asset/my%20logo.png | /dA/" + ID + "/asset/my%20logo.png",
            "/dA/" + ID + "/asset/O'Neil.png | /dA/" + ID + "/asset/O'Neil.png",
            "/dA/" + ID + "/asset/logo#2.png | /dA/" + ID + "/asset/logo%232.png",
            "/dA/" + ID + "/asset/brand;v2.png | /dA/" + ID + "/asset/brand%3Bv2.png",
            "/dA/" + ID + "/asset/50%.png | /dA/" + ID + "/asset/50%25.png",
            "/dA/" + ID + "/asset/my%20\uD83D\uDE00.png | /dA/" + ID + "/asset/my%20%F0%9F%98%80.png"})
    void forwardPath_encodesEachSegment(final String stored, final String expected) {
        assertEquals(Optional.of(expected), CompanyLogoPath.forwardPath(stored));
    }

    /**
     * The path a logo is served from is itself valid and comes back unchanged, so saving it
     * again keeps the logo.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/dotCMS-400x200.png",
            "/dA/" + ID + "/asset/my logo (1).png",
            "/dA/" + ID + "/asset/O'Neil.png",
            "/dA/" + ID + "/asset/logo#2.png",
            "/dA/" + ID + "/asset/brand;v2.png",
            "/dA/" + ID + "/asset/50%.png",
            "/dA/" + ID + "/asset/%zz.png",
            "/dA/" + ID + "/asset/café 😀.png",
            "/dA/" + ID + "/asset/200w/logo.png"})
    void forwardPath_isValidAndStable(final String stored) {
        final String served = CompanyLogoPath.forwardPath(stored).orElseThrow();

        assertTrue(CompanyLogoPath.isValid(served), served);
        assertEquals(Optional.of(served), CompanyLogoPath.forwardPath(served));
    }

    /** A value with a UTF-16 surrogate that is not part of a pair is not valid. */
    @ParameterizedTest
    @ValueSource(strings = {"/dA/" + ID + "/asset/lo\uD800go.png", "/dA/" + ID + "/asset/lo\uDC00go.png",
            "/dA/" + ID + "/asset/my%20logo\uD800"})
    void unpairedSurrogate_isNotValid(final String path) {
        assertFalse(CompanyLogoPath.isValid(path), path);
    }

    /** A value that is not valid has no path to serve the logo from. */
    @ParameterizedTest
    @ValueSource(strings = {"/dA/../html/logo.png", "/dAx/logo.png", "/html/logo.png"})
    void forwardPath_isEmptyForInvalidValues(final String stored) {
        assertEquals(Optional.empty(), CompanyLogoPath.forwardPath(stored));
    }

    /** A missing value has no path to serve the logo from. */
    @Test
    void forwardPath_isEmptyForMissingValue() {
        assertEquals(Optional.empty(), CompanyLogoPath.forwardPath(null));
    }
}
