package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
/**
 * Used for throwing contentlet validation problems
 * @author Jason Tesser
 * @since 1.6
 */
public class DotContentletValidationException extends DotContentletStateException {

	public final static String VALIDATION_FAILED_BADTYPE = "badType";
	public final static String VALIDATION_FAILED_REQUIRED = "required";
	public final static String VALIDATION_FAILED_PATTERN = "pattern";

	public final static String VALIDATION_FAILED_REQUIRED_REL = "reqRel";
	public final static String VALIDATION_FAILED_INVALID_REL_CONTENT = "badRelCon";
	public final static String VALIDATION_FAILED_BAD_REL = "badRel";
	public final static String VALIDATION_FAILED_UNIQUE = "unique";
	public final static String VALIDATION_FAILED_BAD_CARDINALITY = "badCar";

	private static final long serialVersionUID = 1L;
	private Map<String, List<Field>> notValidFields = new HashMap<>();
	private Map<String, Map<Relationship, List<Contentlet>>> notValidRelationships = new HashMap<>();

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
	 */
	public void addRequiredRelationship(Relationship relationship, List<Contentlet> contentlets){
		Map<Relationship, List<Contentlet>> m = new HashMap<>();
		m.put(relationship, contentlets);
		notValidRelationships.put(VALIDATION_FAILED_REQUIRED_REL, m);
	}

	/**
	 * If the contentlet doesn't match the proper structure
	 * @param relationship
	 * @param contentlets
	 */
	public void addInvalidContentRelationship(Relationship relationship, List<Contentlet> contentlets){
		Map<Relationship, List<Contentlet>> m = new HashMap<>();
		m.put(relationship, contentlets);
		notValidRelationships.put(VALIDATION_FAILED_INVALID_REL_CONTENT, m);
	}

	/**
	 * Use to add a validation exception if the structure isn't allowed to be related to the content
	 * @param relationship
	 * @param contentlets
	 */
	public void addBadRelationship(Relationship relationship, List<Contentlet> contentlets){
		Map<Relationship, List<Contentlet>> m = new HashMap<>();
		m.put(relationship, contentlets);
		notValidRelationships.put(VALIDATION_FAILED_BAD_REL, m);
	}

	/**
	 * Use to add a validation exception if the structure isn't allowed to be related to the content
	 * @param relationship
	 * @param contentlets
	 */
	public void addBadCardinalityRelationship(Relationship relationship, List<Contentlet> contentlets){
		Map<Relationship, List<Contentlet>> m = new HashMap<>();
		m.put(relationship, contentlets);
		notValidRelationships.put(VALIDATION_FAILED_BAD_CARDINALITY, m);
	}

	/**
	 * Use to add a required field that failed validation
	 * @param field
	 */
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
	 */
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
	 */
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
	 */
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
		if(notValidFields.size()>0){
			return true;
		}
		return false;
	}

	/**
	 * use to find out is contentlet has any relationship validation errors
	 * @return
	 */
	public boolean hasRelationshipErrors(){
		if(notValidRelationships.size()>0){
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

	public String toString(boolean parentException) {
		final StringBuilder builder = new StringBuilder();
		if (parentException) {
			builder.append(super.toString()).append("\n");
		}
		//Print the Field errors
		Set<String> keys = notValidFields.keySet();
		if (keys.size() > 0) {
			builder.append("List of non valid fields\n");
			for (String key : keys) {
				builder.append(key.toUpperCase()).append(": ");
				List<Field> fields = notValidFields.get(key);

				for (int i = 0; i < fields.size(); i++) {
					Field field = fields.get(i);

					if (i > 0) {
						builder.append(", ");
					}

					builder.append(field.getVelocityVarName()).append("/")
							.append(field.getFieldName());
				}
				builder.append("\n");
			}
		}
		//Print the Relationship errors
		keys = notValidRelationships.keySet();
		if (keys.size() > 0) {
			builder.append("List of non valid relationships\n");
			for (String key : keys) {
				builder.append(key.toUpperCase()).append(": ");
				Map<Relationship, List<Contentlet>> relationshipContentlets = notValidRelationships
						.get(key);
				for (Entry<Relationship, List<Contentlet>> relationship : relationshipContentlets
						.entrySet()) {
					builder.append(relationship.getKey().getRelationTypeValue()).append(", ");
				}
				builder.append("\n");
			}
		}
		builder.append("\n");
		return builder.toString();
	}

	@Override
	public String getMessage() {
		final String toString = toString(false);

		if (UtilMethods.isSet(toString)) {
			return super.getMessage() + "\n" + toString;
		} else {
			return super.getMessage();
		}
	}
}
