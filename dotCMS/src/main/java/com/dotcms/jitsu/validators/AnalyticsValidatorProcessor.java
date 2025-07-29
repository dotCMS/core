package com.dotcms.jitsu.validators;

import com.dotcms.analytics.metrics.EventType;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processor class for analytics validation.
 * <p>
 * This class reads and processes JSON validation files from the {@code analytics.validators}
 * resource folder. It builds a map of field validators for each {@link EventType}.
 * </p>
 *
 * <p>
 * There should be one validation file per event type, as well as a special {@code all.json} file.
 * The {@code all.json} file defines global field validations and can also override validations
 * specified in the individual event files.
 * </p>
 *
 * <p>
 * Each validation file follows a structure similar to the example below:
 * </p>
 *
 * <pre>
 * {
 *   "context": {
 *     "site_key": {
 *       "type": "string",
 *       "required": true,
 *       "custom-validator": "SiteKeyValidator"
 *     },
 *     "session_id": {
 *       "type": "string",
 *       "required": true
 *     },
 *     "user_Id": {
 *       "type": "string",
 *       "required": true
 *     }
 *   },
 *   "events": {
 *     "type": "json array",
 *     "array_type": "json_object",
 *     "required": true,
 *     "attributes": {
 *       "eventType": {
 *         "type": "string",
 *         "required": true
 *       },
 *       "data": {
 *         "type": "json object",
 *         "required": true
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public class AnalyticsValidatorProcessor {

    final static AnalyticsValidatorProcessor INSTANCE = new AnalyticsValidatorProcessor();
    public static final String VALIDATOR_TYPE_ATTRIBUTE = "type";
    public static final String VALIDATOR_ARRAY_TYPE_ATTRIBUTE = "array_type";

    private AnalyticsValidatorProcessor(){}

    private static final String ALL_JSON_PATH = "analytics/validators/all.json";
    private static final String EVENT_JSON_PATH_TEMPLATE = "analytics/validators/%s.json";

    private static final List<AnalyticsValidator> ALL_VALIDATORS = List.of(
        new RequiredFieldValidator(),
        new StringTypeValidator(),
        new JsonObjectTypeValidator(),
        new JsonArrayTypeValidator(),
        new DateValidator(),
        new SiteAuthValidator()
    );

    public Validators getGlobalValidators(){
        try {
            return getValidators(ALL_JSON_PATH);
        } catch (IOException e) {
            Logger.error(this, "Error processing global validators", e);
            throw new RuntimeException("Error processing global validators", e);
        }
    }

    /**
     * Processes JSON validation files and creates a map of validators for each event type.
     * 
     * @return The map of validators for each event type
     */
    public Map<EventType, Validators> getEventTypeValidators(List<Validators.JSONPathValidators> eventsGlobalValidators) {
        final Map<EventType, Validators> validatorsByEventType = new HashMap<>();

        try {
            for (EventType eventType : EventType.values()) {

                final String eventName = eventType.getName();
                final String eventJsonPath = String.format(EVENT_JSON_PATH_TEMPLATE, eventName);

                final Validators validators = getValidators(eventJsonPath, "data");
                validators.addAll(eventsGlobalValidators);

                validatorsByEventType.put(eventType, validators);
            }
        } catch (IOException e) {
            Logger.error(this, "Error processing analytics validators", e);
            throw new RuntimeException("Error processing analytics validators", e);
        }

        return validatorsByEventType;
    }

    private Validators getValidators(String eventJsonPath) throws IOException {
        return getValidators(eventJsonPath, "");
    }

    private Validators getValidators(final String eventJsonPath, final String pathPrefix)
            throws IOException {
        final String jsonContent = FileUtil.getFileContentFromResourceContext(eventJsonPath);
        return processValidatorsMap(new JSONObject(jsonContent), pathPrefix);
    }


    /**
     * Processes the JSON validators and creates a map of field paths to validator classes.
     *
     * @param jsonValidationConfig    The JSON configuration
     * @return The map of field paths to validator classes
     */
    private Validators processValidatorsMap(final JSONObject jsonValidationConfig, final String pathPrefix) {

        final List<Validators.JSONPathValidators> jsonPathValidatorsList = new ArrayList<>();
        processValidatorsRecursive(jsonValidationConfig, pathPrefix, jsonPathValidatorsList);

        return new Validators(jsonPathValidatorsList);
    }

    /**
     * Recursively processes the JSON validators and populates the result map based on the JSON
     * object that was sent by the client.
     *
     * @param jsonValidationConfig   The JSON validation file context
     * @param path                   The current path
     * @param jsonPathValidatorsList The list of JSON path validators.
     */
    private void processValidatorsRecursive(final JSONObject jsonValidationConfig,
                                            final String path,
                                            final List<Validators.JSONPathValidators> jsonPathValidatorsList) {
        for (final Object keyObj : jsonValidationConfig.keySet()) {
            final String key = keyObj.toString();
            final Object value = jsonValidationConfig.get(key);
            final String currentPath = path.isEmpty() ? key : path + "." + key;

            if (value instanceof JSONObject) {
                final JSONObject jsonValue = (JSONObject) value;

                if (!jsonValue.has(VALIDATOR_TYPE_ATTRIBUTE)) {
                    throw new DotRuntimeException(String.format("JSON path %s contains no type", currentPath));
                }

                final ValidatorType type = ValidatorType.valueOf(
                        jsonValue.get(VALIDATOR_TYPE_ATTRIBUTE).toString().toUpperCase());

                final List<AnalyticsValidator> fieldValidators = getValidatorsForField(jsonValue);

                if (!fieldValidators.isEmpty()) {
                    jsonPathValidatorsList.add(new Validators.JSONPathValidators(currentPath, fieldValidators));
                }

                callRecursivelyIfNeed(jsonPathValidatorsList, type, jsonValue, currentPath);
            }
        }
    }

/**
     * If the current validator type represents a JSON object or an array of JSON objects,
     * recursively processes the nested "allowed_attributes" section to collect validators
     * for nested fields.
     *
     * @param jsonPathValidatorsList Collector list where discovered path validators are added
     * @param type                   The validator type for the current node (object/array/etc.)
     * @param jsonValue              The JSON configuration object at the current path
     * @param currentPath            The dot-notation path accumulated so far
     */
    private void callRecursivelyIfNeed(final List<Validators.JSONPathValidators> jsonPathValidatorsList,
                                       final ValidatorType type, JSONObject jsonValue,
                                       final String currentPath) {
        if (isJsonObject(type) || isJsonObjectArray(type, jsonValue)) {
            if (jsonValue.has("allowed_attributes")) {
                processValidatorsRecursive(jsonValue.getJSONObject("allowed_attributes"),
                        currentPath, jsonPathValidatorsList);
            }
        }
    }

    /**
     * Determines whether the current validator node represents an array of JSON objects.
     * This is true when the node type is JSON_ARRAY and the array_type is JSON_OBJECT.
     *
     * @param type      The declared validator type for the node
     * @param jsonValue The JSON configuration object for the node
     * @return true if this node is an array of JSON objects; false otherwise
     */
    private static boolean isJsonObjectArray(final ValidatorType type, final JSONObject jsonValue) {
        return ValidatorType.JSON_ARRAY == type &&
                ValidatorType.valueOf(jsonValue.get(VALIDATOR_ARRAY_TYPE_ATTRIBUTE).toString().toUpperCase()) == ValidatorType.JSON_OBJECT;
    }

    /**
     * Checks if the validator type denotes a JSON object node.
     *
     * @param type The validator type to check
     * @return true if the type is JSON_OBJECT; false otherwise
     */
    private static boolean isJsonObject(ValidatorType type) {
        return ValidatorType.JSON_OBJECT == type;
    }

    /**
     * Gets the validator classes for a field based on its JSON configuration.
     * Uses the test method of AnalyticsValidator to determine which validators to apply.
     * 
     * @param fieldConfig The field configuration
     * @return The list of validator classes
     */
    private List<AnalyticsValidator> getValidatorsForField(final JSONObject fieldConfig) {
        final List<AnalyticsValidator> validators = new ArrayList<>();

        // Use the test method of each validator to determine if it should be applied
        for (AnalyticsValidator validator : ALL_VALIDATORS) {
            if (validator.test(fieldConfig)) {
                try {
                    validators.add(validator.getClass().getDeclaredConstructor().newInstance());
                } catch (final InstantiationException |
                        IllegalAccessException |
                        InvocationTargetException |
                        NoSuchMethodException e) {
                    final String message = String.format("Validator '%s' could not be instantiated. " +
                            "It must have a default constructor", validator.getClass().getName());
                    throw new DotRuntimeException(message);
                }
            }
        }
        return validators;
    }

    private enum ValidatorType {
        JSON_OBJECT,
        JSON_ARRAY,
        STRING,
        DATE;
    }

}
