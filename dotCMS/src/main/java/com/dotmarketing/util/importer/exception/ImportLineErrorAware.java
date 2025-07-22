package com.dotmarketing.util.importer.exception;

import com.dotmarketing.util.importer.ImportLineValidationCodes;
import java.util.Map;
import java.util.Optional;

public interface ImportLineErrorAware {

    default String getCode() { return ImportLineValidationCodes.UNKNOWN_ERROR.name(); }

    default Optional<Map<String, ? extends Object>> getContext(){ return Optional.empty(); }

    default Optional<String> getField() { return Optional.empty(); }

    default Optional<String> getValue() { return Optional.empty(); }

}
