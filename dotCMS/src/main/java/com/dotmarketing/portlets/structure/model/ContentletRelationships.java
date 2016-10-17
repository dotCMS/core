package com.dotmarketing.portlets.structure.model;

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
		private boolean hasParent; 
		
		/**
		 * @param relationship
		 */
		public ContentletRelationshipRecords(Relationship relationship, boolean hasParent) {
			super();
			this.relationship = relationship;
			this.records = new ArrayList<Contentlet> ();
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
	}
	
	
}
