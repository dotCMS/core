package com.dotcms.jitsu.validators;

import com.dotcms.analytics.attributes.MaxCustomAttributesReachedException;
import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.CUSTOM_ATTRIBUTE_NAME;
import static com.dotcms.jitsu.ValidAnalyticsEventPayloadAttributes.DATA_ATTRIBUTE_NAME;

/**
 * Utility class for analytics validation.
 * This class delegates to AnalyticsValidatorProcessor for the actual processing of JSON validation files.
 */
public class AnalyticsValidatorUtil {

    public final static AnalyticsValidatorUtil INSTANCE = new AnalyticsValidatorUtil();

    private final Validators globalValidators;
    private final List<Validators.JSONPathValidators> eventsGlobalValidators;
    private final Map<EventType, Validators> eventsValidators;


    /**
     * Constructor that initializes the validators map by processing JSON validation files.
     * It delegates to AnalyticsValidatorProcessor for the actual processing.
     */
    private AnalyticsValidatorUtil() {
        final Validators allJsonvalidators = AnalyticsValidatorProcessor.INSTANCE.getGlobalValidators();

        globalValidators = new Validators(getGlobalValidators(allJsonvalidators));
        eventsGlobalValidators = getEventsGlobalValidators(allJsonvalidators);

        eventsValidators = AnalyticsValidatorProcessor.INSTANCE.getEventTypeValidators(eventsGlobalValidators);
    }

    private List<Validators.JSONPathValidators> getEventsGlobalValidators(final Validators allJsonvalidators) {
        return allJsonvalidators.getValidators().entrySet().stream()
                .filter(entrySet -> entrySet.getKey().startsWith("events."))
                .map(entrySet -> new Validators.JSONPathValidators(
                        entrySet.getKey().replace("events.", ""), entrySet.getValue()))
                .collect(Collectors.toUnmodifiableList());
    }

    private static List<Validators.JSONPathValidators> getGlobalValidators(Validators allJsonvalidators) {
        return allJsonvalidators.getValidators().entrySet().stream()
                    .filter(entrySet -> !entrySet.getKey().startsWith("events."))
                    .map(entrySet -> new Validators.JSONPathValidators(
                            entrySet.getKey(), entrySet.getValue()))
                    .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Validates a JSON object against the all.json files, it validates the context section and makes sure
     * that the event has an events array too
     *
     * @param jsonObject The JSON object to validate
     * @return A list of validation errors, empty if no errors are found
     */
    public List<Error> validateGlobalContext(final JSONObject jsonObject)  {
        return validate(jsonObject, globalValidators);

    }

    private List<Error> validate(final JSONObject jsonObject, final Validators validators) {
        final List<Error> errors = new ArrayList<>();
        // Check for required fields that are missing from the JSON object
        errors.addAll(checkRequiredFields(jsonObject, validators));

        // Validate fields that are present in the JSON object
        final List<JsonValues> jsonValues = extractJsonValues(jsonObject);

        for (final JsonValues jsonValue : jsonValues) {
            final List<AnalyticsValidator> pathValidators = validators.getValidators(jsonValue.path)
                    .orElse(Collections.emptyList());

            for (final AnalyticsValidator validator : pathValidators) {
                try {
                    validator.validate(jsonValue.value);
                } catch (AnalyticsValidator.AnalyticsValidationException e) {
                    errors.add(new Error(jsonValue.path, e.getCode(), e.getMessage()));
                    break;
                }
            }
        }

        errors.addAll(checkExtraFields(validators, jsonValues));

        return errors;
    }

    private static List<Error>  checkExtraFields(Validators validators, List<JsonValues> jsonValues) {
        final List<String> jsonPaths = jsonValues.stream()
                .map(values -> values.path)
                .collect(Collectors.toList());

        jsonPaths.removeAll(validators.getPaths());

        return jsonPaths.stream()
                .map(path -> new Error(path, ValidationErrorCode.UNKNOWN_FIELD,
                        String.format("Unknown field '%s'", path)))
                .collect(Collectors.toList());
    }

    /**
     * Checks if required fields are present in the JSON object.
     * If a required field is missing, adds an error to the errors list.
     *
     * @param jsonObject The JSON object to check
     */
    private List<Error> checkRequiredFields(final JSONObject jsonObject, final Validators validators) {
        final List<Error> requiredFields = new ArrayList<>();

        // Get all paths from the validators map
        for (String path : validators.getPaths()) {
            if (validators.isRequired(path)) {
                Object value = getValueFromPath(jsonObject, path);
                if (value == null) {
                    requiredFields.add(new Error(path, ValidationErrorCode.REQUIRED_FIELD_MISSING,
                            "Required field is missing: " + path));
                }
            }
        }

        return requiredFields;
    }

    /**
     * Gets a value from a JSON object using a dot-notation path.
     *
     * @param jsonObject The JSON object to get the value from
     * @param path The path to the value, using dot notation (e.g., "context.site_key")
     * @return The value at the path, or null if the path doesn't exist
     */
    private Object getValueFromPath(final JSONObject jsonObject, final String path) {
        String[] parts = path.split("\\.");
        JSONObject current = jsonObject;

        // Navigate through the path
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i])) {
                return null;
            }
            Object value = current.opt(parts[i]);
            if (!(value instanceof JSONObject)) {
                return null;
            }
            current = (JSONObject) value;
        }

        // Get the final value
        String lastPart = parts[parts.length - 1];
        if (!current.has(lastPart)) {
            return null;
        }
        return current.opt(lastPart);
    }

    /**
     * Validates each event in the events array using the appropriate validators based on the event_type.
     *
     * @param events The JSON array of events to validate
     * @return A list of validation errors, empty if no errors are found
     */
    public List<Error> validateEvents(final JSONArray events)  {
        final List<Error> errors = new ArrayList<>();

        for (int i = 0; i < events.length(); i++) {
            final JSONObject event = events.optJSONObject(i);

            final Validators validators =  Optional.ofNullable(event.optString("event_type"))
                    .map(AnalyticsValidatorUtil::getEventType)
                    .map(eventsValidators::get)
                    .orElse(new Validators(eventsGlobalValidators));

            try {
                checkCustomSection(event, event.optString("event_type"));

                final List<Error> eventErrors = validate(event, validators);

                for (Error error : eventErrors) {
                    errors.add(new Error("events[" + i + "]." + error.getField(),
                            error.getCode(), error.getMessage(), i));
                }
            } catch (MaxCustomAttributesReachedException e) {
                errors.add(new Error("events[" + i + "].custom: ",
                        ValidationErrorCode.MAX_LIMIT_OF_CUSTOM_ATTRIBUTE_REACHED, e.getMessage(), i));
            }
        }

        return errors;
    }

    /**
     * Validates and persists custom attribute mappings for an event, if present.
     * <p>
     * This method extracts the data.custom section (if any), and invokes the
     * Analytics Custom Attribute API to check that adding any new custom attributes for the
     * given event type does not exceed the allowed limit. It may create or update mappings.
     * Any persistence error is wrapped in a DotRuntimeException.
     *
     * @param event        The event JSON object that may contain data.custom
     * @param eventTypeStr The event type name used to validate custom attributes
     */
    private static void checkCustomSection(final JSONObject event, String eventTypeStr) {
        final Optional<JSONObject> customSection = removeCustomSection(event);

        if (customSection.isPresent()) {
            try {
                APILocator.getAnalyticsCustomAttribute()
                        .checkCustomPayloadValidation(eventTypeStr, customSection.get());
            } catch (DotDataException e) {
                throw new DotRuntimeException(e);
            }
        }
    }

    /**
     * Removes and returns the data.custom JSON object from the event if present.
     * <p>
     * This is used prior to validation so the custom section can be processed separately and
     * does not interfere with the standard validators.
     *
     * @param event The event JSON object
     * @return An Optional containing the removed custom object if it existed; otherwise Optional.empty()
     */
    private static Optional<JSONObject> removeCustomSection(JSONObject event) {
        if (event.has(DATA_ATTRIBUTE_NAME)) {
            final JSONObject data = event.getJSONObject(DATA_ATTRIBUTE_NAME);

            if (data.has(CUSTOM_ATTRIBUTE_NAME)) {
                final JSONObject custom = data.getJSONObject(CUSTOM_ATTRIBUTE_NAME);

                event.getJSONObject(DATA_ATTRIBUTE_NAME).remove(CUSTOM_ATTRIBUTE_NAME);
                return Optional.ofNullable(custom);
            }

        }

        return Optional.empty();
    }

    private static EventType getEventType(String eventTypeStr) {
        try {
            return EventType.get(eventTypeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Retrieves a list of field paths that have date validators associated with them for a specific event type.
     * This method examines all validators for the given event type and identifies fields that are validated
     * using a DateValidator.
     *
     * @param eventTypeName The event type name for which to find date fields
     * @return A list of paths (in dot notation) to fields that are validated as dates
     */
    public List<String> getDateField(final String eventTypeName) {
        final List<String> paths = new ArrayList<>();
        final EventType eventType = getEventType(eventTypeName);

        final Validators validators = eventType != null ? eventsValidators.get(eventType) :
                new Validators(eventsGlobalValidators);

        for (String path : validators.getPaths()) {

            final boolean isDate = validators.getValidators(path).stream()
                    .flatMap(Collection::stream)
                    .anyMatch(validator -> DateValidator.class.isAssignableFrom(validator.getClass()));

            if (isDate) {
                paths.add(path);
            }
        }

        return paths;
    }

    private static class JsonValues {
        final String path;
        final Object value;

        public JsonValues(final String path, final Object value) {
            this.path = path;
            this.value = value;
        }
    }

    /**
     * Extracts a list of JsonValues from a JSONObject.
     * Each JsonValue contains a path and its corresponding value.
     * For nested objects, the path is represented using dot notation (e.g., "context.site_key").
     *
     * @param jsonObject The JSONObject to extract values from
     * @return A list of JsonValues containing paths and their corresponding values
     */
    private List<JsonValues> extractJsonValues(final JSONObject jsonObject) {
        final List<JsonValues> result = new ArrayList<>();
        extractJsonValues(jsonObject, "", result);
        return result;
    }

    /**
     * Recursive helper method to extract JsonValues from a JSONObject.
     *
     * @param jsonObject The JSONObject to extract values from
     * @param parentPath The path of the parent object (empty for root)
     * @param result     The list to store the extracted JsonValues
     */
    private void extractJsonValues(final JSONObject jsonObject, final String parentPath,
                                   final List<JsonValues> result) {
        final Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            final String key = keys.next();
            final String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            final Object value = jsonObject.opt(key);

            if (value instanceof JSONObject) {
                // Recursively process nested objects
                extractJsonValues((JSONObject) value, currentPath, result);
            } else {
                // Add leaf values to the result
                result.add(new JsonValues(currentPath, value));
            }
        }
    }

    public static class Error {
        private String field;
        private ValidationErrorCode code;
        private String message;
        private int eventIndex = -1;

        private Error(final String field, final ValidationErrorCode code, final String message) {
            this.field = field;
            this.code = code;
            this.message = message;
        }

        private Error(final String field, final ValidationErrorCode code, final String message,
                      final int eventIndex) {
            this(field, code, message);
            this.eventIndex = eventIndex;
        }

        public String getField() {
            return field;
        }

        public ValidationErrorCode getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public int getEventIndex() {
            return eventIndex;
        }
    }


}
