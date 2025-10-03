package com.dotmarketing.portlets.contentlet.business;

import static com.dotmarketing.util.FieldNameUtils.convertFieldClassName;
import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.SPACE;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.FieldNameUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used for throwing contentlet validation problems
 * @author Jason Tesser
 * @since 1.6
 */
public class DotContentletValidationException extends DotContentletStateException implements ImportLineError {

    public static final String VALIDATION_FAILED_BADTYPE = "badType";
    public static final String VALIDATION_FAILED_REQUIRED = "required";
    public static final String VALIDATION_FAILED_PATTERN = "pattern";

    public static final String VALIDATION_FAILED_REQUIRED_REL = "reqRel";
    public static final String VALIDATION_FAILED_INVALID_REL_CONTENT = "badRelCon";
    public static final String VALIDATION_FAILED_BAD_REL = "badRel";
    public static final String VALIDATION_FAILED_UNIQUE = "unique";
    public static final String VALIDATION_FAILED_BAD_CARDINALITY = "badCar";

	private static final long serialVersionUID = 1L;
	private final Map<String, List<Field>> notValidFields = new HashMap<>();
	private final Map<String, Map<Relationship, List<Contentlet>>> notValidRelationships = new HashMap<>();

	private ImportLineError importLineError = null;

	/**
	 * Used for throwing contentlet validation problems
	 * @param x
	 */
	public DotContentletValidationException(String x) {
		super(x);
	}

	/**
	 * Used for throwing contentlet validation problems
	 * @param x
	 * @param e
	 */
	public DotContentletValidationException(String x, Exception e) {
		super(x, e);
	}

	/**
	 * Package-private constructor for setting ImportLineError from Builder
	 * @param x
	 * @param importLineError
	 */
	protected DotContentletValidationException(String x, ImportLineError importLineError) {
		super(x);
		this.importLineError = importLineError;
	}

	/**
	 * Package-private constructor for setting ImportLineError and validation details from Builder
	 * @param x
	 * @param importLineError
	 * @param notValidFields
	 * @param notValidRelationships
	 */
	protected DotContentletValidationException(String x, ImportLineError importLineError,
			Map<String, List<Field>> notValidFields,
			Map<String, Map<Relationship, List<Contentlet>>> notValidRelationships) {
		super(x);
		this.importLineError = importLineError;
		this.notValidFields.putAll(notValidFields);
		this.notValidRelationships.putAll(notValidRelationships);
	}


	/**
	 * Returns a map where the key is the validation property and the value is a List<Field> that failed
	 * @return
	 */
	public Map<String, List<Field>> getNotValidFields() {
		return notValidFields;
	}

	/**
	 * if contentlet is missing a required relationship
	 * @param relationship
	 * @param contentlets
	 * @deprecated Use {@link Builder#addRequiredRelationship(Relationship, List)} instead
	 */
	@Deprecated(forRemoval = true)
	public void addRequiredRelationship(Relationship relationship, List<Contentlet> contentlets){
		Map<Relationship, List<Contentlet>> m = new HashMap<>();
		m.put(relationship, contentlets);
		notValidRelationships.put(VALIDATION_FAILED_REQUIRED_REL, m);
	}

	/**
	 * If the contentlet doesn't match the proper structure
	 * @param relationship
	 * @param contentlets
	 * @deprecated Use {@link Builder#addInvalidContentRelationship(Relationship, List)} instead
	 */
	@Deprecated(forRemoval = true)
	public void addInvalidContentRelationship(Relationship relationship, List<Contentlet> contentlets){
		Map<Relationship, List<Contentlet>> m = new HashMap<>();
		m.put(relationship, contentlets);
		notValidRelationships.put(VALIDATION_FAILED_INVALID_REL_CONTENT, m);
	}

	/**
	 * Use to add a validation exception if the structure isn't allowed to be related to the content
	 * @param relationship
	 * @param contentlets
	 * @deprecated Use {@link Builder#addBadRelationship(Relationship, List)} instead
	 */
	@Deprecated(forRemoval = true)
	public void addBadRelationship(Relationship relationship, List<Contentlet> contentlets){
		Map<Relationship, List<Contentlet>> m = new HashMap<>();
		m.put(relationship, contentlets);
		notValidRelationships.put(VALIDATION_FAILED_BAD_REL, m);
	}

	/**
	 * Use to add a validation exception if the structure isn't allowed to be related to the content
	 * @param relationship
	 * @param contentlets
	 * @deprecated Use {@link Builder#addBadCardinalityRelationship(Relationship, List)} instead
	 */
	@Deprecated(forRemoval = true)
	public void addBadCardinalityRelationship(Relationship relationship, List<Contentlet> contentlets){
		Map<Relationship, List<Contentlet>> m = new HashMap<>();
		m.put(relationship, contentlets);
		notValidRelationships.put(VALIDATION_FAILED_BAD_CARDINALITY, m);
	}

	/**
	 * Use to add a required field that failed validation
	 * @param field
	 * @deprecated Use {@link Builder#addRequiredField(Field, String)} instead for better ImportLineError support
	 */
	@Deprecated(forRemoval = true)
	public void addRequiredField(Field field){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_REQUIRED);
		if(fields == null)
			fields = new ArrayList<>();
		fields.add(field);
		notValidFields.put(VALIDATION_FAILED_REQUIRED, fields);
	}

	/**
	 * Use to add a field that failed pattern validation
	 * @param field
	 * @deprecated Use {@link Builder#addPatternField(Field, String, String)} instead for better ImportLineError support
	 */
	@Deprecated(forRemoval = true)
	public void addPatternField(Field field){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_PATTERN);
		if(fields == null)
			fields = new ArrayList<>();
		fields.add(field);
        notValidFields.put(VALIDATION_FAILED_PATTERN, fields);
	}

	/**
	 * Use to add a field that failed because it has an unknown type
	 * @param field
	 * @deprecated Use {@link Builder#addBadTypeField(Field, String)} instead for better ImportLineError support
	 */
	@Deprecated(forRemoval = true)
	public void addBadTypeField(Field field){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_BADTYPE);
		if(fields == null)
			fields = new ArrayList<>();
		fields.add(field);
		notValidFields.put(VALIDATION_FAILED_BADTYPE, fields);
	}

	/**
	 * Use to add a field that failed unique field validation
	 * @param field
	 * @deprecated Use {@link Builder#addUniqueField(Field, String)} instead for better ImportLineError support
	 */
	@Deprecated(forRemoval = true)
	public void addUniqueField(Field field){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_UNIQUE);
		if(fields == null)
			fields = new ArrayList<>();
		fields.add(field);
		notValidFields.put(VALIDATION_FAILED_UNIQUE, fields);
	}

	public boolean hasRequiredErrors(){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_REQUIRED);
		if(fields == null || fields.isEmpty())
			return false;
		return true;
	}

	public boolean hasPatternErrors(){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_PATTERN);
		if(fields == null || fields.isEmpty())
			return false;
		return true;
	}

	public boolean hasBadTypeErrors(){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_BADTYPE);
		if(fields == null || fields.isEmpty())
			return false;
		return true;
	}

	/**
	 * use to find out if contentlet has any field validation errors
	 * @return
	 */
	public boolean hasFieldErrors(){
		if(!notValidFields.isEmpty()){
			return true;
		}
		return false;
	}

	/**
	 * use to find out is contentlet has any relationship validation errors
	 * @return
	 */
	public boolean hasRelationshipErrors(){
		if(!notValidRelationships.isEmpty()){
			return true;
		}
		return false;
	}

	/**
	 * use to return the relationships that have errors
	 */

	public Map<String,Map<Relationship,List<Contentlet>>> getNotValidRelationship()
	{
		return notValidRelationships;
	}


	/**
	 * use to find out is contentlet has any relationship validation errors
	 * @return
	 */
	public boolean hasUniqueErrors(){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_UNIQUE);
		if(fields == null || fields.isEmpty())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return toString(true);
	}

	/**
	 * Returns a string representation of the list of errors found during the validation of a
	 * new or existing Contentlet.
	 *
	 * @param parentException If the parent exception error message must be included as well, set
	 *                        this to {@code true}.
	 *
	 * @return The list of validation errors, if any.
	 */
	public String toString(final boolean parentException) {
		final StringBuilder builder = new StringBuilder();
		if (parentException) {
			builder.append(super.toString());
		}
		//Print the Field errors
		Set<String> keys = notValidFields.keySet();
		if (!keys.isEmpty()) {
			builder.append("Fields: ");
			for (final String key : keys) {
				builder.append("[").append(key.toUpperCase()).append("]").append(": ");
				final List<Field> fields = notValidFields.get(key);

				for (int i = 0; i < fields.size(); i++) {
					final Field field = fields.get(i);
					if (i > 0) {
						builder.append(", ");
					}
					builder.append(field.getFieldName()).append(" (")
							.append(field.getVelocityVarName()).append(")");
				}
				builder.append(SPACE);
			}
		}
		//Print the Relationship errors
		keys = notValidRelationships.keySet();
		if (!keys.isEmpty()) {
			builder.append(UtilMethods.isSet(notValidFields.keySet()) ? " / " : BLANK).append("Relationships: ");
			int i = 0;
			for (final String key : keys) {
				builder.append("[").append(key.toUpperCase()).append("]").append(": ");
				final Map<Relationship, List<Contentlet>> relationshipContentlets = notValidRelationships.get(key);
				for (final Entry<Relationship, List<Contentlet>> relationship : relationshipContentlets.entrySet()) {
					builder.append(relationship.getKey().getRelationTypeValue());
				}
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(SPACE);
				i++;
			}
		}
		return builder.toString().trim();
	}

	@Override
	public String getMessage() {
		final String baseMessage = super.getMessage();
		final String validationDetails = this.toString(false);
		
		// If no validation details or base message already contains validation info, return base message
		if (!UtilMethods.isSet(validationDetails) || 
			(baseMessage.contains("Fields:") || baseMessage.contains("Relationships:"))) {
			return baseMessage;
		}
		
		// Otherwise, append validation details to base message
		return baseMessage + " - " + validationDetails;
	}

	@Override
	public String getCode() {
		if (null != importLineError) {
			return importLineError.getCode();
		}
		return ImportLineError.super.getCode();
	}

	@Override
	public Optional<Map<String, ? extends Object>> getContext() {
		if (null != importLineError) {
			return importLineError.getContext();
		}
		return ImportLineError.super.getContext();
	}

	@Override
	public Optional<String> getField() {
		if (null != importLineError) {
			return importLineError.getField();
		}
		return ImportLineError.super.getField();
	}

	@Override
	public Optional<String> getValue() {
		if (null != importLineError) {
			return importLineError.getValue();
		}
		return ImportLineError.super.getValue();
	}

	/**
	 * Generic builder class to create DotContentletValidationException and its subclasses with multiple validation errors.
	 * This builder allows accumulating multiple field validation errors and automatically 
	 * creates ImportLineError information from the first error for ImportUtil consumption.
	 * 
	 * @param <T> The exception type extending DotContentletValidationException
	 */
	public static class Builder<T extends DotContentletValidationException> {
		protected final T exception;
        private String firstErrorCode = ImportLineValidationCodes.UNKNOWN_ERROR.name();
        private Field firstErrorField = null;
        private String firstErrorValue = null;
		private String firstErrorPattern = null;
        private String firstErrorHint = null;

		public Builder(T exception) {
			this.exception = exception;
		}

		/**
		 * Add a required field validation error
		 */
		public Builder<T> addRequiredField(Field field, String value) {
			exception.addRequiredField(field);
			captureFirstError(field, value, ImportLineValidationCodes.REQUIRED_FIELD_MISSING.name(), null, null);
			return this;
		}

		/**
		 * Add a pattern validation error
		 */
		public Builder<T> addPatternField(Field field, String value, String pattern) {
			exception.addPatternField(field);
			captureFirstError(field, value, ImportLineValidationCodes.VALIDATION_FAILED_PATTERN.name(), pattern, null);
			return this;
		}

		/**
		 * Add a bad type field validation error
		 */
		public Builder<T> addBadTypeField(Field field, String value) {
			exception.addBadTypeField(field);
			captureFirstError(field, value, ImportLineValidationCodes.INVALID_FIELD_TYPE.name(), null, null);
			return this;
		}

		/**
		 * Add a unique field validation error
		 */
		public Builder<T> addUniqueField(Field field, String value) {
			exception.addUniqueField(field);
			captureFirstError(field, value, ImportLineValidationCodes.DUPLICATE_UNIQUE_VALUE.name(), null, null);
			return this;
		}

		/**
		 * Add a required relationship validation error
		 */
        public Builder<T> addRequiredRelationship(Relationship relationship,
                List<Contentlet> contentlets) {
            exception.addRequiredRelationship(relationship, contentlets);
            final String value = relationship.getRelationTypeValue();
            final Field dummy = new Field();
            dummy.setFieldName(value);
            dummy.setVelocityVarName(value);
            dummy.setFieldType(FieldType.RELATIONSHIP.toString());
            captureFirstError(dummy, toInodeString(contentlets),
                    ImportLineValidationCodes.RELATIONSHIP_VALIDATION_ERROR.name(), null,
                    "The relationship is required for the selected contentlets");
            return this;
        }

		/**
		 * Add an invalid content relationship validation error
		 */
        public Builder<T> addInvalidContentRelationship(Relationship relationship,
                List<Contentlet> contentlets) {
            exception.addInvalidContentRelationship(relationship, contentlets);
            final String value = relationship.getRelationTypeValue();
            final Field dummy = new Field();
            dummy.setFieldName(value);
            dummy.setVelocityVarName(value);
            dummy.setFieldType(FieldType.RELATIONSHIP.toString());
            captureFirstError(dummy, toInodeString(contentlets),
                    ImportLineValidationCodes.RELATIONSHIP_VALIDATION_ERROR.name(), null,
                    "The relationship is not valid for the selected contentlets");
            return this;
        }

		/**
		 * Add a bad relationship validation error
		 */
		public Builder<T> addBadRelationship(Relationship relationship, List<Contentlet> contentlets) {
			exception.addBadRelationship(relationship, contentlets);
            final String value = relationship.getRelationTypeValue();
            final Field dummy = new Field();
            dummy.setFieldName(value);
            dummy.setVelocityVarName(value);
            dummy.setFieldType(FieldType.RELATIONSHIP.toString());
            captureFirstError(dummy,toInodeString(contentlets),ImportLineValidationCodes.RELATIONSHIP_VALIDATION_ERROR.name(), null, null);
			return this;
		}

		/**
		 * Add a bad cardinality relationship validation error
		 */
        public Builder<T> addBadCardinalityRelationship(Relationship relationship,
                List<Contentlet> contentlets) {
            exception.addBadCardinalityRelationship(relationship, contentlets);
            final String value = relationship.getRelationTypeValue();
            final Field dummy = new Field();
            dummy.setFieldName(value);
            dummy.setVelocityVarName(value);
            dummy.setFieldType(FieldType.RELATIONSHIP.toString());
            captureFirstError(dummy, toInodeString(contentlets),
                    ImportLineValidationCodes.RELATIONSHIP_CARDINALITY_ERROR.name(), null,
                    "The relationship cardinality is not valid for the selected contentlets. " +
                         "Check if the assigment is breaking a One-to-One or One-to-Many relationship rule."
            );
            return this;
        }

		/**
		 * Capture first error information for ImportLineError
		 */
		private void captureFirstError(Field field, String value, String errorCode, String pattern, String hint) {
			if (firstErrorField == null) {
				firstErrorField = field;
				firstErrorValue = value;
				firstErrorCode = errorCode;
				firstErrorPattern = pattern;
                firstErrorHint = hint;
			}
		}

		/**
		 * Build the exception with ImportLineError information from the first validation error
		 */
		@SuppressWarnings("unchecked")
		public T build() {
			if (firstErrorField != null) {
				final ImportLineError lineError = new ImportLineError() {
					@Override
					public String getCode() {
						return firstErrorCode;
					}

					@Override
					public Optional<String> getField() {
						return Optional.ofNullable(firstErrorField.getVelocityVarName());
					}

					@Override
					public Optional<String> getValue() {
						return Optional.ofNullable(firstErrorValue);
					}

					@Override
					public Optional<Map<String, ?>> getContext() {
						final Map<String, String> ctx = new HashMap<>();
						ctx.put("fieldType", convertFieldClassName(firstErrorField.getFieldType()));
						if (UtilMethods.isSet(firstErrorPattern)) {
							ctx.put("expectedPattern", firstErrorPattern);
						}
                        if (UtilMethods.isSet(firstErrorHint)) {
                            ctx.put("errorHint", firstErrorHint);
                        }
						return Optional.of(ctx);
					}
				};
				
				// Create a new exception instance with ImportLineError and validation details using the specialized constructor
				final T newException;
				if (exception instanceof FileAssetValidationException) {
					newException = (T) new FileAssetValidationException(exception.getMessage(), lineError,
							exception.getNotValidFields(), exception.getNotValidRelationship());
				} else {
					newException = (T) new DotContentletValidationException(exception.getMessage(), lineError,
							exception.getNotValidFields(), exception.getNotValidRelationship());
				}
				
				return newException;
			}
			return exception;
		}
	}

	/**
	 * Create a new Builder for DotContentletValidationException
	 */
	public static Builder<DotContentletValidationException> builder(String message) {
		return new Builder<>(new DotContentletValidationException(message));
	}

	/**
	 * Create a new Builder for FileAssetValidationException
	 */
	public static Builder<FileAssetValidationException> fileAssetBuilder(String message) {
		return new Builder<>(new FileAssetValidationException(message));
	}

	/**
	 * Converts a list of contentlets to a comma-separated string of their inodes.
	 * Filters out contentlets with empty inodes and returns "N/A" if no valid inodes exist.
	 *
	 * @param contentlets List of contentlets to extract inodes from
	 * @return Comma-separated string of valid inodes, or "N/A" if none exist
	 */
	public static String toInodeString(List<Contentlet> contentlets) {
		if (contentlets == null || contentlets.isEmpty()) {
			return "N/A";
		}

		String result = contentlets.stream()
			.map(Contentlet::getInode)
			.filter(UtilMethods::isSet)
			.collect(Collectors.joining(","));

		return UtilMethods.isSet(result) ? result : "N/A";
	}

}
