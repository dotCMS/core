package com.dotcms.graphql.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TypeUtilTest {



    @Test
    void testCollectionizedName() {
        // Basic functionality
        assertEquals("TestCollection", TypeUtil.collectionizedName("Test"));

        // With empty string
        assertEquals("Collection", TypeUtil.collectionizedName(""));

        // Already has Collection suffix
        assertEquals("TestCollectionCollection", TypeUtil.collectionizedName("TestCollection"));
    }

    @Test
    void testSingularizeCollectionName() {
        // Basic functionality
        assertEquals("Test", TypeUtil.singularizeCollectionName("TestCollection"));

        // Without Collection suffix
        assertEquals("Test", TypeUtil.singularizeCollectionName("Test"));

        // Collection in the middle
        assertEquals("TestCollectionSuffix", TypeUtil.singularizeCollectionName("TestCollectionSuffix"));

        // Collection in the middle and End
        assertEquals("TestCollectionSuffix", TypeUtil.singularizeCollectionName("TestCollectionSuffixCollection"));


        // Multiple Collections, only removes from end
        assertEquals("TestCollection", TypeUtil.singularizeCollectionName("TestCollectionCollection"));

        // Empty string
        assertEquals("", TypeUtil.singularizeCollectionName(""));
    }

    @Test
    void testSingularizeBaseTypeCollectionName() {
        // Both Collection and BaseType
        assertEquals("Test", TypeUtil.singularizeBaseTypeCollectionName("TestBaseTypeCollection"));

        // Only Collection suffix
        assertEquals("Test", TypeUtil.singularizeBaseTypeCollectionName("TestCollection"));

        // Only BaseType
        assertEquals("Test", TypeUtil.singularizeBaseTypeCollectionName("TestBaseType"));

        // Multiple BaseType occurrences
        assertEquals("TestTest", TypeUtil.singularizeBaseTypeCollectionName("TestBaseTypeTestBaseType"));

        // Neither Collection nor BaseType
        assertEquals("Test", TypeUtil.singularizeBaseTypeCollectionName("Test"));

        // Empty string
        assertEquals("", TypeUtil.singularizeBaseTypeCollectionName(""));
    }
}
