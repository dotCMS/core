/**
 * 
 */
package com.dotmarketing.business.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;

/**
 * @author Jason Tesser
 *
 */
public abstract class GenericQueryFactory { 

	public enum Operator {
		
		EQUAL("="),
		LIKE("LIKE"),
		NOT_EQUAL("!="),
		LESS_THAN("<"),
		LESS_THAN_OR_EQUAL("<="),
		GREATER_THAN(">"),
		GREATER_THAN_OR_EQUAL(">="),
		IS_NULL("IS NULL"),
		IS_NOT_NULL("IS NOT NULL"),
		IN("IN");	
			
		private String value;
		
		Operator (String value) {
			this.value = value;
		}
		
		public String toString () {
			return value;
		}
			
		public static Operator getOperator (String value) {
			Operator[] ops = Operator.values();
			for (Operator op : ops) {
				if (op.value.equals(value))
					return op;
			}
			return null;
		}
			
	}
	
	public enum BuilderType {
		CONTENTLET("CONTENTLET"),
		FILE_ASSET("FILE_ASSET"),
		FOLDER("FOLDER"),
		HTMLPAGE("HTMLPAGE"),
		STRUCTURE("STRUCTURE"),
		MENU_LINK_TABLE("LINKS"),
		MENU_LINK("MENULINK")
		;
		
		private String value;
		
		BuilderType (String value) {
			this.value = value;
		}
		
		public String toString () {
			return value.toLowerCase();
		}
			
		public static BuilderType getObject (String value) {
			BuilderType[] ojs = BuilderType.values();
			for (BuilderType oj : ojs) {
				if (oj.value.equals(value))
					return oj;
			}
			return null;
		}
	}
	
	protected Query query;
		
	public GenericQueryFactory (BuilderType builderType){
		query = new Query();
		query.setBuilderType(builderType);
	}
	
	public GenericQueryFactory (){
		query = new Query();
	}
		
	public void setCritera(Criteria criteria){
		query.setCriteria(criteria);
	}
	
	public void setSelectAttributes(List<String> attributes){
		query.setSelectAttributes(attributes);
	}
	
	public void addLimit(int limit){
		query.setLimit(limit);
	}
	
	public void start(int start){
		query.setStart(start);
	}
	
	public Query getQuery(){
		return query;
	}
	
	public List<Map<String, Serializable>> execute() throws DotDataException, ValidationException{
		return query.execute();
	}
			
	public class GroupingCriteria implements ComplexCriteria{
		
		private List<Criteria> result = new ArrayList<Criteria>();

		private Map<Criteria,String> operators = new HashMap<Criteria,String>();
		
		private boolean negate = false;
		
		public GroupingCriteria(Criteria criteria) {
			result.add(criteria);
		}
				
		/**
		 * @return the criteria
		 */
		public List<Criteria> getCriteria() {
			return result;
		}
				
		public void addAndCriteria(Criteria criteria){
			result.add(criteria);
			operators.put(criteria, Criteria.LOGICAL_OPERATOR_AND);
		}
		
		public void addOrCriteria(Criteria criteria){
			result.add(criteria);
			operators.put(criteria, Criteria.LOGICAL_OPERATOR_OR);
		}

		public String getPreceedingOperator(Criteria criteria) {
			return operators.get(criteria);
		}

		public boolean isNegate() {
			return negate;
		}

		/**
		 * @param negate the negate to set
		 */
		public void setNegate(boolean negate) {
			this.negate = negate;
		}
		
	}
	
	public class SingleCriteria implements SimpleCriteria{
		private String attribute;
		private Operator operator;
		private Object value;
		
		public SingleCriteria(String attribute, Operator operator, Object value) {
			this.attribute = attribute;
			this.operator = operator;
			this.value = value;
		}
				
		/**
		 * @return the attribute
		 */
		public String getAttribute() {
			return attribute;
		}
		/**
		 * @param attribute the attribute to set
		 */
		public void setAttribute(String attribute) {
			this.attribute = attribute;
		}
		/**
		 * @return the operator
		 */
		public Operator getOperator() {
			return operator;
		}
		/**
		 * @param operator the operator to set
		 */
		public void setOperator(Operator operator) {
			this.operator = operator;
		}
		/**
		 * @return the value
		 */
		public Object getValue() {
			return value;
		}
		/**
		 * @param value the value to set
		 */
		public void setValue(Object value) {
			this.value = value;
		} 
	}
	
	public class Query {
		private Criteria criteria = null;
		private List<String> selectAttributes = null;
		private BuilderType builderType;
		private int limit = 0;
		private int start = 0;
		private String fromClause;
		
		/**
		 * @return the criteria
		 */
		public Criteria getCriteria() {
			return criteria;
		}
		
		/**
		 * @param criteria the criteria to set
		 */
		public void setCriteria(Criteria criteria) {
			this.criteria = criteria;
		}
		
		/**
		 * @return the selectAttributes
		 */
		public List<String> getSelectAttributes() {
			return selectAttributes;
		}
		
		/**
		 * @param selectAttributes the selectAttributes to set
		 */
		public void setSelectAttributes(List<String> selectAttributes) {
			this.selectAttributes = selectAttributes;
		}
		
		/**
		 * @return the builderType
		 */
		public BuilderType getBuilderType() {
			return builderType;
		}
		
		/**
		 * @param builderType the builderType to set
		 */
		public void setBuilderType(BuilderType builderType) {
			this.builderType = builderType;
		}
		
		/**
		 * @return the limit
		 */
		public int getLimit() {
			return limit;
		}
		
		/**
		 * @param limit the limit to set
		 */
		public void setLimit(int limit) {
			this.limit = limit;
		}
		
		/**
		 * @return the start
		 */
		public int getStart() {
			return start;
		}
		
		/**
		 * @param start the start to set
		 */
		public void setStart(int start) {
			this.start = start;
		}
		
		public List<Map<String, Serializable>> execute() throws DotDataException, ValidationException{
			if(builderType.equals(BuilderType.CONTENTLET)){
				return APILocator.getContentletAPI().DBSearch(query, APILocator.getUserAPI().getSystemUser(), false);
			}
			else if (builderType.equals(BuilderType.HTMLPAGE)){
				return APILocator.getHTMLPageAPI().DBSearch(query, APILocator.getUserAPI().getSystemUser(), false);
			}
			else if (builderType.equals(BuilderType.STRUCTURE)){
				return StructureFactory.DBSearch(query, APILocator.getUserAPI().getSystemUser(), false);
			}
			else if (builderType.equals(BuilderType.FOLDER)){
				return APILocator.getFolderAPI().DBSearch(query, APILocator.getUserAPI().getSystemUser(), false);
			}
			else if (builderType.equals(BuilderType.FILE_ASSET)){
				return APILocator.getFileAPI().DBSearch(query, APILocator.getUserAPI().getSystemUser(), false);
			}
			else if (builderType.equals(BuilderType.MENU_LINK)){
				return LinkFactory.DBSearch(query, APILocator.getUserAPI().getSystemUser(), false);
			}
			return null;
		}

		/**
		 * @return the fromClause
		 */
		public String getFromClause() {
			return fromClause;
		}

		/**
		 * @param fromClause the fromClause to set
		 */
		public void setFromClause(String fromClause) {
			this.fromClause = fromClause;
		}
		
	}
	
}
