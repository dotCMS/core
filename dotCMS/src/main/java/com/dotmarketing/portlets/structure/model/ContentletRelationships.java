package com.dotmarketing.portlets.structure.model;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dotmarketing.comparators.ContentComparator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;


/**
 * This is a class used to store (in session) contentlets relationships matches during a contentlet edition
 * @author David
 *
 */
public class ContentletRelationships 
{
	
	private static final long serialVersionUID = 1L;
	
	private Contentlet contentlet;
	private List<ContentletRelationshipRecords> relationshipsRecords;
	
	
	/**
	 * @param contentlet
	 * @param relationshipsRecords
	 */
	public ContentletRelationships(Contentlet contentlet, List<ContentletRelationshipRecords> relationshipsRecords) {
		super();
		this.contentlet = contentlet;
		this.relationshipsRecords = relationshipsRecords;
	}
	
	
	/**
	 * @param contentlet
	 */
	public ContentletRelationships(Contentlet contentlet) {
		this.contentlet = contentlet;
		this.relationshipsRecords = new ArrayList<ContentletRelationshipRecords> ();
	}


	/**
	 * @return Returns the relationshipsRecords.
	 */
	public List<ContentletRelationshipRecords> getRelationshipsRecords() {
		return relationshipsRecords;
	}

	/**
	 * Given a legacy field returns its relationship records
	 *
	 * @return List of ContentletRelationshipRecords that belong to a field.
	 */
	public List<ContentletRelationshipRecords> getRelationshipsRecordsByField(final Field field) {

		com.dotcms.contenttype.model.field.Field newField = new LegacyFieldTransformer(field)
				.from();

		return getRelationshipsRecordsByField(newField);

	}

	/**
	 * Given a field returns its relationship records
	 *
	 * @return List of ContentletRelationshipRecords that belong to a field.
	 */
	public List<ContentletRelationshipRecords> getRelationshipsRecordsByField(
			final com.dotcms.contenttype.model.field.Field field) {

		final ContentType contentType;
		try {
			contentType = APILocator
					.getContentTypeAPI(APILocator.getUserAPI().getSystemUser())
					.find(field.contentTypeId());
		} catch (DotSecurityException | DotDataException e) {
			throw new RuntimeException(e);
		}

		final String relationTypeValue = field.relationType().contains(StringPool.PERIOD)
				? field.relationType()
				: contentType.variable() + StringPool.PERIOD + field.variable();

		return relationshipsRecords.stream()
				.filter(record -> record.relationship.getRelationTypeValue()
						.equals(relationTypeValue) && isShowUpField(record,
						field.variable())).collect(CollectionsUtils.toImmutableList());
	}

	/**
	 * Defines if relationship records should be displayed as fields in UI. Useful for self-related
	 * content to avoid duplicates
	 * @param records - ContentletRelationshipRecords that specifies the relationship, isHasParent and related content
	 * @param fieldVar - Velocity var name of the field
	 * @return boolean indicating if the relationship records have to be displayed as fields in UI
	 */
	private boolean isShowUpField(ContentletRelationshipRecords records, String fieldVar){

		final Relationship relationship = records.getRelationship();

		//When it is a non-self relationship must always be displayed in UI
		if(!relationship.getParentStructureInode().equals(relationship.getChildStructureInode())){
			return true;
		}else{

			//Self-related case
			//Evaluates if the field is used to list children or parents
			//In case children are listed, parent records will be discarded and vice versa
			if ((relationship.getChildRelationName() != null && relationship.getChildRelationName()
					.equals(fieldVar) && records.isHasParent())
					|| (relationship.getParentRelationName() != null && relationship.getParentRelationName()
					.equals(fieldVar) && !records.isHasParent())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return Returns the relationshipRecords that do not belong to any field
	 */
	public List<ContentletRelationshipRecords> getLegacyRelationshipsRecords() {

		//Filter legacy relationship records, which do not have a relationship field
		return relationshipsRecords.stream()
				.filter(record -> !record.getRelationship().isRelationshipField())
				.collect(CollectionsUtils.toImmutableList());
	}

	/**
	 * @param relationshipsRecords The relationshipsRecords to set.
	 */
	public void setRelationshipsRecords(
			List<ContentletRelationshipRecords> relationshipsRecords) {
		this.relationshipsRecords = relationshipsRecords;
	}
	/**
	 * @return Returns the contentlet.
	 */
	public Contentlet getContentlet() {
		return contentlet;
	}
	/**
	 * @param contentlet The contentlet to set.
	 */
	public void setContentlet(Contentlet contentlet) {
		this.contentlet = contentlet;
	}
	
	public class ContentletRelationshipRecords {

		private Relationship relationship;
		private List<Contentlet> records;

		//When hasParent=true, the current side of the relationship will be the parent and the returned records will be its children
		private boolean hasParent;

		/**
		 * @param relationship
		 */
		public ContentletRelationshipRecords(Relationship relationship, boolean hasParent) {
			super();
			this.relationship = relationship;
			this.records = new ArrayList<>();
			this.hasParent = hasParent;
		}


		/**
		 * @return Returns the hasParent.
		 */
		public boolean isHasParent() {
			return hasParent;
		}


		/**
		 * @param hasParent The hasParent to set.
		 */
		public void setHasParent(boolean hasParent) {
			this.hasParent = hasParent;
		}


		/**
		 * @return Returns the records.
		 */
		public List<Contentlet> getRecords() {
			return records;
		}

		/**
		 * @param records The records to set.
		 */
		public void setRecords(List<Contentlet> records) {
			this.records = records;
		}

		/**
		 * @return Returns the relationship.
		 */
		public Relationship getRelationship() {
			return relationship;
		}

		/**
		 * @param relationship The relationship to set.
		 */
		public void setRelationship(Relationship relationship) {
			this.relationship = relationship;
		}

		public void reorderRecords(String field) {

			String fieldContentletName = null;

			Structure st = contentlet.getStructure();
			List<Field> fields = st.getFields();
			for (Field f : fields) {
				if (f.getFieldName().equals(field)) {
					fieldContentletName = f.getFieldContentlet();
					break;
				}
			}

			if (fieldContentletName != null)
				Collections.sort(records, new ContentComparator(fieldContentletName));
		}

		/**
		 * Checks if a relationship side only allows a single entry
		 * @return
		 */
		public boolean doesAllowOnlyOne() {
			return (!this.hasParent)
					? this.relationship.getCardinality()
					!= WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()
					: relationship.getCardinality()
							== WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal();

		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}

			ContentletRelationshipRecords records;

			try {
				records = (ContentletRelationshipRecords) obj;
			} catch (ClassCastException cce) {
				return false;
			}

			return this.getRelationship().getRelationTypeValue()
					.equals(records.getRelationship().getRelationTypeValue())
					&& this.isHasParent() == records.isHasParent();
		}
	}
}
