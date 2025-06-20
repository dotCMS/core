package com.dotcms.jitsu.validators;

import com.dotcms.analytics.metrics.EventType;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.Logger;
import org.jetbrains.annotations.NotNull;

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
 *       "required": true
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

    private AnalyticsValidatorProcessor(){}

    private static final String ALL_JSON_PATH = "analytics/validators/all.json";
    private static final String EVENT_JSON_PATH_TEMPLATE = "analytics/validators/%s.json";

    private static final List<AnalyticsValidator> ALL_VALIDATORS = List.of(
        new RequiredFieldValidator(),
        new StringTypeValidator(),
        new JsonObjectTypeValidator(),
        new JsonArrayTypeValidator()
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
    public Map<EventType, Validators> getEventTypeValidators() {
        final Map<EventType, Validators> validatorsByEventType = new HashMap<>();

        try {
            for (EventType eventType : EventType.values()) {

                final String eventName = eventType.getName();
                final String eventJsonPath = String.format(EVENT_JSON_PATH_TEMPLATE, eventName);

                final Validators validators = getValidators(eventJsonPath);

                validatorsByEventType.put(eventType, validators);
            }
        } catch (IOException e) {
            Logger.error(this, "Error processing analytics validators", e);
            throw new RuntimeException("Error processing analytics validators", e);
        }

        return validatorsByEventType;
    }

    private Validators getValidators(String eventJsonPath) throws IOException {
        final String jsonContent = FileUtil.getFileContentFromResourceContext(eventJsonPath);
        final Validators validators = processValidatorsMap(new JSONObject(jsonContent));
        return validators;
    }


    /**
     * Processes the JSON validators and creates a map of field paths to validator classes.
     *
     * @param jsonValidationConfig    The JSON configuration
     * @return The map of field paths to validator classes
     */
    private Validators processValidatorsMap(final JSONObject jsonValidationConfig) {

        final List<Validators.JSONPathValidators> jsonPathValidatorsList = new ArrayList<>();
        processValidatorsRecursive(jsonValidationConfig, "", jsonPathValidatorsList);

        return new Validators(jsonPathValidatorsList);
    }

    /**
     * Recursively processes the JSON validators and populates the result map.
     * 
     * @param jsonValidationConfig The JSON validation file context
     * @param path The current path
     * @param jsonPathValidatorsList
     */
    private void processValidatorsRecursive(final JSONObject jsonValidationConfig,
                                            final String path,
                                            final List<Validators.JSONPathValidators> jsonPathValidatorsList) {
        for (Object keyObj : jsonValidationConfig.keySet()) {
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

                if (ValidatorType.JSON_OBJECT == type) {
                    processValidatorsRecursive(jsonValue.getJSONObject("allowed_attributes"),
                            currentPath, jsonPathValidatorsList);
                }
            }
        }
    }

    /**
     * Gets the validator classes for a field based on its JSON configuration.
     * Uses the test method of AnalyticsValidator to determine which validators to apply.
     * 
     * @param fieldConfig The field configuration
     * @return The list of validator classes
     */
    private List<AnalyticsValidator> getValidatorsForField(JSONObject fieldConfig) {
        List<AnalyticsValidator> validators = new ArrayList<>();

        // Use the test method of each validator to determine if it should be applied
        for (AnalyticsValidator validator : ALL_VALIDATORS) {
            if (validator.test(fieldConfig)) {
                try {
                    validators.add(validator.getClass().getDeclaredConstructor().newInstance());
                } catch (InstantiationException |
                        IllegalAccessException |
                        InvocationTargetException |
                        NoSuchMethodException e) {
                    final String message = String.format("the validator %s could not be instantiated, " +
                            "it must has a default constructor", validator.getClass().getName());

                    throw new RuntimeException(message);
                }

            }
        }

        return validators;
    }

    private  enum ValidatorType {
        JSON_OBJECT,
        JSON_ARRAY,
        STRING;
    }
}