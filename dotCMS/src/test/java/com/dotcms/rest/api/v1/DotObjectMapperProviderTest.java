
package com.dotcms.rest.api.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class DotObjectMapperProviderTest {

    @BeforeClass
    public static void prepare() throws Exception {

    }

    /**
     * Method to test: Default ObjectMapper When: Serializing and deserializing Instant Should: Handle Instant as
     * timestamps (epoch milliseconds)
     */
    @Test
    public void testDefaultMapper_SerializesInstantAsTimestamp() throws JsonProcessingException {
        // Create an Instant with only millisecond precision to match the expected behavior
        final Instant now = Instant.ofEpochMilli(Instant.now().toEpochMilli());
        final ObjectMapper defaultMapper = DotObjectMapperProvider.getInstance().getTimestampObjectMapper();

        final TestInstantHolder holder = new TestInstantHolder();
        holder.timestamp = now;

        // Serialize to JSON
        final String json = defaultMapper.writeValueAsString(holder);

        // Should contain the timestamp as a number (epoch milliseconds)
        final Map<String, Object> jsonMap = defaultMapper.readValue(json, Map.class);
        assertEquals("Instant should be serialized as epoch milliseconds",
                now.toEpochMilli(), ((Number) jsonMap.get("timestamp")).longValue());

        // Deserialize back
        final TestInstantHolder deserialized = defaultMapper.readValue(json, TestInstantHolder.class);
        assertEquals("Deserialized Instant should match original", now, deserialized.timestamp);
    }

    /**
     * Method to test: ISO8601 ObjectMapper When: Serializing and deserializing Instant Should: Handle Instant as
     * ISO8601 formatted strings
     */
    @Test
    public void testIso8601Mapper_SerializesInstantAsIso8601() throws JsonProcessingException {
        final Instant now = Instant.now();
        final ObjectMapper iso8601Mapper = DotObjectMapperProvider.getInstance().getIso8610ObjectMapper();

        final TestInstantHolder holder = new TestInstantHolder();
        holder.timestamp = now;

        // Serialize to JSON
        final String json = iso8601Mapper.writeValueAsString(holder);

        // Should contain the timestamp as an ISO8601 string
        final Map<String, Object> jsonMap = iso8601Mapper.readValue(json, Map.class);
        final String timestampStr = (String) jsonMap.get("timestamp");
        assertTrue("Instant should be serialized as ISO8601 string",
                timestampStr.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z"));

        // Deserialize back
        final TestInstantHolder deserialized = iso8601Mapper.readValue(json, TestInstantHolder.class);
        assertEquals("Deserialized Instant should match original", now, deserialized.timestamp);
    }

    /**
     * Method to test: Both ObjectMappers When: Serializing and deserializing LocalDateTime Should: Handle LocalDateTime
     * correctly
     */
    @Test
    public void testBothMappers_SerializeLocalDateTime() throws JsonProcessingException {
        final LocalDateTime now = LocalDateTime.now();
        final ObjectMapper defaultMapper = DotObjectMapperProvider.getInstance().getTimestampObjectMapper();
        final ObjectMapper iso8601Mapper = DotObjectMapperProvider.getInstance().getIso8610ObjectMapper();

        final TestLocalDateTimeHolder holder = new TestLocalDateTimeHolder();
        holder.dateTime = now;

        // Test default mapper
        final String defaultJson = defaultMapper.writeValueAsString(holder);
        final TestLocalDateTimeHolder defaultDeserialized = defaultMapper.readValue(defaultJson,
                TestLocalDateTimeHolder.class);
        assertEquals("Default mapper should handle LocalDateTime correctly", now, defaultDeserialized.dateTime);

        // Test ISO8601 mapper
        final String iso8601Json = iso8601Mapper.writeValueAsString(holder);
        final TestLocalDateTimeHolder iso8601Deserialized = iso8601Mapper.readValue(iso8601Json,
                TestLocalDateTimeHolder.class);
        assertEquals("ISO8601 mapper should handle LocalDateTime correctly", now, iso8601Deserialized.dateTime);
    }

    /**
     * Method to test: Both ObjectMappers When: Serializing and deserializing LocalDate Should: Handle LocalDate
     * correctly
     */
    @Test
    public void testBothMappers_SerializeLocalDate() throws JsonProcessingException {
        final LocalDate today = LocalDate.now();
        final ObjectMapper defaultMapper = DotObjectMapperProvider.getInstance().getTimestampObjectMapper();
        final ObjectMapper iso8601Mapper = DotObjectMapperProvider.getInstance().getIso8610ObjectMapper();

        final TestLocalDateHolder holder = new TestLocalDateHolder();
        holder.date = today;

        // Test default mapper
        final String defaultJson = defaultMapper.writeValueAsString(holder);
        final TestLocalDateHolder defaultDeserialized = defaultMapper.readValue(defaultJson, TestLocalDateHolder.class);
        assertEquals("Default mapper should handle LocalDate correctly", today, defaultDeserialized.date);

        // Test ISO8601 mapper
        final String iso8601Json = iso8601Mapper.writeValueAsString(holder);
        final TestLocalDateHolder iso8601Deserialized = iso8601Mapper.readValue(iso8601Json, TestLocalDateHolder.class);
        assertEquals("ISO8601 mapper should handle LocalDate correctly", today, iso8601Deserialized.date);
    }

    /**
     * Method to test: Both ObjectMappers When: Serializing and deserializing ZonedDateTime Should: Handle ZonedDateTime
     * correctly
     */
    @Test
    public void testBothMappers_SerializeZonedDateTime() throws JsonProcessingException {
        final ZonedDateTime now = ZonedDateTime.now();
        final ObjectMapper defaultMapper = DotObjectMapperProvider.getInstance().getTimestampObjectMapper();
        final ObjectMapper iso8601Mapper = DotObjectMapperProvider.getInstance().getIso8610ObjectMapper();

        final TestZonedDateTimeHolder holder = new TestZonedDateTimeHolder();
        holder.zonedDateTime = now;

        // Test default mapper - when using timestamps, timezone info is lost and converted to system default
        final String defaultJson = defaultMapper.writeValueAsString(holder);
        final TestZonedDateTimeHolder defaultDeserialized = defaultMapper.readValue(defaultJson,
                TestZonedDateTimeHolder.class);
        // When serialized as timestamp, we expect the same instant but potentially different timezone
        assertEquals("Default mapper should preserve the instant",
                now.toInstant(), defaultDeserialized.zonedDateTime.toInstant());

        // Test ISO8601 mapper - preserves more precision but Jackson normalizes timezone to UTC
        final String iso8601Json = iso8601Mapper.writeValueAsString(holder);
        final TestZonedDateTimeHolder iso8601Deserialized = iso8601Mapper.readValue(iso8601Json,
                TestZonedDateTimeHolder.class);
        // Jackson's default behavior is to normalize ZonedDateTime to UTC during deserialization
        assertEquals("ISO8601 mapper should preserve the instant",
                now.toInstant(), iso8601Deserialized.zonedDateTime.toInstant());
    }

    /**
     * Method to test: Both ObjectMappers When: Serializing and deserializing OffsetDateTime Should: Handle
     * OffsetDateTime correctly
     */
    @Test
    public void testBothMappers_SerializeOffsetDateTime() throws JsonProcessingException {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final ObjectMapper defaultMapper = DotObjectMapperProvider.getInstance().getTimestampObjectMapper();
        final ObjectMapper iso8601Mapper = DotObjectMapperProvider.getInstance().getIso8610ObjectMapper();

        final TestOffsetDateTimeHolder holder = new TestOffsetDateTimeHolder();
        holder.offsetDateTime = now;

        // Test default mapper - when using timestamps, offset info is lost and converted to system default
        final String defaultJson = defaultMapper.writeValueAsString(holder);
        final TestOffsetDateTimeHolder defaultDeserialized = defaultMapper.readValue(defaultJson,
                TestOffsetDateTimeHolder.class);
        // When serialized as timestamp, we expect the same instant but potentially different offset
        assertEquals("Default mapper should preserve the instant",
                now.toInstant(), defaultDeserialized.offsetDateTime.toInstant());

        // Test ISO8601 mapper - preserves more precision but Jackson may normalize offset
        final String iso8601Json = iso8601Mapper.writeValueAsString(holder);
        final TestOffsetDateTimeHolder iso8601Deserialized = iso8601Mapper.readValue(iso8601Json,
                TestOffsetDateTimeHolder.class);
        // Since we're using UTC offset, this should be preserved, but check instant to be safe
        assertEquals("ISO8601 mapper should preserve the instant",
                now.toInstant(), iso8601Deserialized.offsetDateTime.toInstant());
    }


    /**
     * Method to test: Both mappers When: Deserializing with disabled WRAP_EXCEPTIONS Should: Both mappers should not
     * wrap exceptions during deserialization
     */
    @Test(expected = java.time.format.DateTimeParseException.class)
    public void testBothMappers_DoNotWrapExceptions_DefaultMapper() throws JsonProcessingException {
        final String invalidJson = "{\"timestamp\":\"not-a-valid-timestamp\"}";
        final ObjectMapper defaultMapper = DotObjectMapperProvider.getInstance().getTimestampObjectMapper();

        defaultMapper.readValue(invalidJson, TestInstantHolder.class);
    }

    @Test(expected = com.fasterxml.jackson.databind.exc.InvalidFormatException.class)
    public void testBothMappers_DoNotWrapExceptions_Iso8601Mapper() throws JsonProcessingException {
        final String invalidJson = "{\"timestamp\":\"not-a-valid-timestamp\"}";
        final ObjectMapper iso8601Mapper = DotObjectMapperProvider.getInstance().getIso8610ObjectMapper();

        iso8601Mapper.readValue(invalidJson, TestInstantHolder.class);
    }

    /**
     * Method to test: {@link DotObjectMapperProvider#getInstance()} When: Getting the singleton instance multiple times
     * {{ ... }}
     */
    @Test
    public void testGetInstance_ReturnsSingleton() {
        final DotObjectMapperProvider instance1 = DotObjectMapperProvider.getInstance();
        final DotObjectMapperProvider instance2 = DotObjectMapperProvider.getInstance();

        assertTrue("getInstance should return the same singleton instance",
                instance1 == instance2);
    }

    /**
     * Method to test: Both mappers When: Getting the mappers multiple times from the same instance Should: Return the
     * same mapper instances
     */
    @Test
    public void testGetMappers_ReturnsSameInstances() {
        final DotObjectMapperProvider provider = DotObjectMapperProvider.getInstance();

        final ObjectMapper defaultMapper1 = provider.getTimestampObjectMapper();
        final ObjectMapper defaultMapper2 = provider.getTimestampObjectMapper();
        final ObjectMapper iso8601Mapper1 = provider.getIso8610ObjectMapper();
        final ObjectMapper iso8601Mapper2 = provider.getIso8610ObjectMapper();

        assertTrue("getDefaultObjectMapper should return the same instance",
                defaultMapper1 == defaultMapper2);
        assertTrue("getIso8610ObjectMapper should return the same instance",
                iso8601Mapper1 == iso8601Mapper2);
    }

    // Test helper classes
    public static class TestInstantHolder {

        public Instant timestamp;
    }

    public static class TestLocalDateTimeHolder {

        public LocalDateTime dateTime;
    }

    public static class TestLocalDateHolder {

        public LocalDate date;
    }

    public static class TestZonedDateTimeHolder {

        public ZonedDateTime zonedDateTime;
    }

    public static class TestOffsetDateTimeHolder {

        public OffsetDateTime offsetDateTime;
    }

    public static class TestSimpleObject {

        public String name;
    }

    public static class TestOptionalHolder {

        public Optional<String> presentValue;
        public Optional<String> emptyValue;
    }
}
