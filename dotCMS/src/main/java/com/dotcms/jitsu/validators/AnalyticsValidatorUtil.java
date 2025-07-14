package com.dotcms.jitsu.validators;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for analytics validation.
 * This class delegates to AnalyticsValidatorProcessor for the actual processing of JSON validation files.
 */
public class AnalyticsValidatorUtil {

    public final static AnalyticsValidatorUtil INSTANCE = new AnalyticsValidatorUtil();

    private final Validators globalValidators;
    private final Map<EventType, Validators> eventsValidators;


    /**
     * Constructor that initializes the validators map by processing JSON validation files.
     * It delegates to AnalyticsValidatorProcessor for the actual processing.
     */
    private AnalyticsValidatorUtil() {
        globalValidators = AnalyticsValidatorProcessor.INSTANCE.getGlobalValidators();
        eventsValidators = AnalyticsValidatorProcessor.INSTANCE.getEventTypeValidators();
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
            final List<AnalyticsValidator> pathValidators = validators.getValidators(jsonValue.path);

            if (pathValidators != null) {
                for (final AnalyticsValidator validator : pathValidators) {
                    try {
                        validator.validate(jsonValue.value);
                    } catch (AnalyticsValidator.AnalyticsValidationException e) {
                        errors.add(new Error(jsonValue.path, e.getCode(), e.getMessage()));
                        break;
                    }
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
    public List<Error> validateEvents(final JSONArray events) {
        final List<Error> errors = new ArrayList<>();

        for (int i = 0; i < events.length(); i++) {
            final JSONObject event = events.optJSONObject(i);

            final Optional<String> eventTypeStr = checkEventObject(event, errors, i);

            if (eventTypeStr.isPresent()) {
                final Validators validators = getEventType(eventTypeStr.get())
                        .map(eventsValidators::get)
                        .orElse(new Validators(Collections.emptyList()));

                final List<Error> eventErrors = validate(event, validators);

                for (Error error : eventErrors) {
                    errors.add(new Error("events[" + i + "]." + error.getField(),
                            error.getCode(), error.getMessage(), i));
                }
            }
        }

        return errors;
    }

    /**
     * Checks if the event object is valid by verifying:
     * 1. The event is not null
     * 2. The event has an "event_type" field
     * 3. The "event_type" field is a non-empty string
     * 
     * If any validation fails, appropriate errors are added to the errors list.
     *
     * @param event The JSON object representing the event to validate
     * @param errors The list to which validation errors will be added
     * @param index The index of the event in the events array, used for error reporting
     * @return The event_type string value from the event object if it is set in the payload
     */
    private static Optional<String> checkEventObject(final JSONObject event, final List<Error> errors, final  int index) {
        if (event == null) {
            errors.add(new Error("events[" + index + "]", ValidationErrorCode.INVALID_JSON_OBJECT_TYPE,
                    "Event is not a JSON object", index));
            return Optional.empty();
        }

        if (!event.has("event_type")) {
            errors.add(new Error("events[" + index + "].event_type", ValidationErrorCode.REQUIRED_FIELD_MISSING,
                    "Required field is missing: event_type", index));
            return Optional.empty();
        }

        final String eventTypeStr = event.optString("event_type");
        if (eventTypeStr == null || eventTypeStr.isEmpty()) {
            errors.add(new Error("events[" + index + "].event_type", ValidationErrorCode.INVALID_STRING_TYPE,
                    "Event type is empty or not a string", index));
        }
        return Optional.of(eventTypeStr);
    }

    private static Optional<EventType> getEventType(String eventTypeStr) {
        try {
            return Optional.of(EventType.get(eventTypeStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves a list of field paths that have date validators associated with them for a specific event type.
     * This method examines all validators for the given event type and identifies fields that are validated
     * using a DateValidator.
     *
     * @param eventType The event type for which to find date fields
     * @return A list of paths (in dot notation) to fields that are validated as dates
     */
    public List<String> getDateField(final EventType eventType) {
        final List<String> paths = new ArrayList<>();
        final Validators validators = eventsValidators.get(eventType);

        for (String path : validators.getPaths()) {
            final boolean isDate = validators.getValidators(path).stream()
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
