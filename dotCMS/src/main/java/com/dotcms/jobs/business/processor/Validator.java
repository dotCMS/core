package com.dotcms.jobs.business.processor;

import com.dotcms.jobs.business.error.JobValidationException;
import java.util.Map;

/**
 * Interface for validating job parameters before creation. Processors can implement this interface
 * to provide validation logic that will be executed before a job is created.
 */
public interface Validator {

    /**
     * Validates the job parameters before job creation.
     *
     * @param parameters The parameters to validate
     * @throws JobValidationException if the parameters are invalid
     */
    void validate(Map<String, Object> parameters) throws JobValidationException;

}
