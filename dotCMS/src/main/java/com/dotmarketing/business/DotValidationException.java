package com.dotmarketing.business;

import static com.dotmarketing.util.importer.ImportLineValidationCodes.RELATIONSHIP_VALIDATION_ERROR;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.importer.exception.ImportLineError;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class DotValidationException extends DotStateException implements ImportLineError {

	public static final String VALIDATION_FAILED_BADTYPE = "badType";
	public static final String VALIDATION_FAILED_REQUIRED = "required";
	public static final String VALIDATION_FAILED_PATTERN = "pattern";

	public static final String VALIDATION_FAILED_REQUIRED_REL = "reqRel";
	public static final String VALIDATION_FAILED_INVALID_REL_CONTENT = "badRelCon";
	public static final String VALIDATION_FAILED_BAD_REL = "badRel";
	public static final String VALIDATION_FAILED_UNIQUE = "unique";
	
	private static final long serialVersionUID = 1L;
	private final Map<String, List<Field>> notValidFields = new HashMap<>();
	private final Map<String, Map<Relationship, List<Contentlet>>> notValidRelationships = new HashMap<>();

	private ImportLineError importLineError = null;

	/**
	 * Used for throwing contentlet validation problems
	 * @param x
	 */
	public DotValidationException(String x) {
		super(x);
	}
	
	/**
	 * Used for throwing contentlet validation problems
	 * @param x
	 * @param e
	 */
	public DotValidationException(String x, Exception e) {
		super(x, e);
	}

	/**
	 *
	 * @param x
	 * @param importLineError
	 */
	private DotValidationException(String x, ImportLineError importLineError) {
       super(x);
	   this.importLineError = importLineError;
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
        return fields != null && !fields.isEmpty();
    }
	
	public boolean hasPatternErrors(){
		List<Field> fields = notValidFields.get(VALIDATION_FAILED_PATTERN);
        return fields != null && !fields.isEmpty();
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
        return !notValidFields.isEmpty();
    }
	
	/**
	 * use to find out is contentlet has any relationship validation errors
	 * @return
	 */
	public boolean hasRelationshipErrors(){
        return !notValidRelationships.isEmpty();
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
        return fields != null && !fields.isEmpty();
    }
	
	@Override
	public String toString()
	{
		boolean parentException = true;
		return toString(parentException);
	}
	
	public String toString(boolean parentException)
	{
		StringBuffer sb = new StringBuffer();
		if(parentException)
		{
			sb.append(super.toString() + "\n");
		}				
		//Print the Field errors
		Set<String> keys = notValidFields.keySet();
		if(keys.size() > 0)
		{
			sb.append("List of non valid fields\n");
			for(String key : keys)
			{
				sb.append(key.toUpperCase() + ": ");
				List<Field> fields = notValidFields.get(key);
				for(Field field : fields)
				{
					sb.append(field.getVelocityVarName() + "/" + field.getFieldName() + ", ");
				}
				sb.append("\n");			
			}
		}
		//Print the Relationship errors
		keys = notValidRelationships.keySet();
		if(keys.size() > 0)
		{
			sb.append("List of non valid relationships\n");
			for(String key : keys)
			{
				sb.append(key.toUpperCase() + ": ");
				Map<Relationship,List<Contentlet>> relationshipContentlets = notValidRelationships.get(key);			
				for(Entry<Relationship,List<Contentlet>> relationship : relationshipContentlets.entrySet())
				{
					sb.append(relationship.getKey().getRelationTypeValue() + ", ");
				}
				sb.append("\n");
			}
		}
		sb.append("\n");
		return sb.toString();
	}


	@Override
	public String getCode() {
		if(null != importLineError){
			return importLineError.getCode();
		}
		return ImportLineError.super.getCode();
	}

	@Override
	public Optional<Map<String, ? extends Object>> getContext() {
		if(null != importLineError){
			return importLineError.getContext();
		}
		return ImportLineError.super.getContext();
	}

	@Override
	public Optional<String> getField() {
		if(null != importLineError){
			return importLineError.getField();
		}
		return ImportLineError.super.getField();
	}

	@Override
	public Optional<String> getValue() {
		if(null != importLineError){
			return importLineError.getValue();
		}
		return ImportLineError.super.getValue();
	}

	public static DotValidationException relationshipInvalidFilterValue(
			final String filter){

		final ImportLineError lineError = new ImportLineError(){

			@Override
			public String getCode() {
				return RELATIONSHIP_VALIDATION_ERROR.name();
			}

			@Override
			public Optional<String> getField() {
				return Optional.of("N/A");
			}

			@Override
			public Optional<String> getValue() {
				return Optional.ofNullable(filter);
			}

		};
		final String message = "The field has a value (" + filter + ") that is not an identifier nor a lucene query";
		return new DotValidationException(message, lineError);
	}

	public static DotValidationException nonMatchingRelationship(
			final Relationship relationship,
			final boolean isParent,
			final ContentType contentType,
			final ContentType relatedContentType){

		final ImportLineError lineError = new ImportLineError(){

			@Override
			public String getCode() {
				return RELATIONSHIP_VALIDATION_ERROR.name();
			}

			@Override
			public Optional<String> getField() {
				return Optional.ofNullable(relationship.getRelationTypeValue());
			}

			@Override
			public Optional<String> getValue() {
				return Optional.ofNullable(relationship.getInode());
			}

			@Override
			public Optional<Map<String,?>> getContext() {
				final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(
						APILocator.systemUser());

				final String parentTypeName = Try.of(()->contentTypeAPI.find(relationship.getParentStructureInode()).name()).getOrElse("UNK");
				final String childTypeName = Try.of(()->contentTypeAPI.find(relationship.getChildStructureInode()).name()).getOrElse("UNK");
				final String errorHint = isParent ?
						String.format("Related content of type [%s] is not a compatible child of [%s].",relatedContentType.name(),contentType.name()) :
						String.format("Related content of type [%s] is not a compatible parent of [%s].", relatedContentType.name(),contentType.name());

				final Map<String,String> ctx =  new HashMap<>();
				ctx.put("Parent-Content-Type", parentTypeName);
				ctx.put("Child-Content-Type", childTypeName);
				ctx.put("errorHint", errorHint);
				ctx.put("Cardinality", Try.of(()->
						RELATIONSHIP_CARDINALITY.fromOrdinal(relationship.getCardinality()).name()
				).getOrElse("UNK"));
				return Optional.of(ctx);
			}
		};
		final String message = "The structure does not match the relationship " + relationship
				.getRelationTypeValue();
		return new DotValidationException(message, lineError);
	}


}
