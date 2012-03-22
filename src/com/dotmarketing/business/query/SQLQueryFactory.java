package com.dotmarketing.business.query;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.chemistry.cmissql.CmisSqlLexer;
import org.apache.chemistry.cmissql.CmisSqlParser;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


public class SQLQueryFactory extends GenericQueryFactory {

	
	
	public SQLQueryFactory(String sql) throws ValidationException {
		
		CharStream input;
		try {
			input = new ANTLRInputStream(new ByteArrayInputStream(sql.getBytes("UTF-8")));
		} catch (Exception e) {
			Logger.error(SQLQueryFactory.class,e.getMessage(),e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
        TokenSource lexer = new CmisSqlLexer(input);
        
        TokenStream tokens = new CommonTokenStream(lexer);
        CommonTree tree;
		try {
			tree = (CommonTree) new CmisSqlParser(tokens).query().getTree();
		} catch (RecognitionException e) {
			Logger.error(SQLQueryFactory.class,e.getMessage(),e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
//		new Bufferedt
        nodes.setTokenStream(tokens);
//        CmisSqlSimpleWalker walker = new CmisSqlSimpleWalker(nodes);
//        CmisSqlSimpleWalker.query_return ret = walker.query(new SQLWalkerDummyData("DUMMY", BaseType.DOCUMENT));
        query = new Query();
        String tableName = tree.getFirstChildWithType(CmisSqlParser.FROM).getChild(0).getChild(0).toString();
//        String tableName = ret.tableName;
        query.setFromClause(tableName);
        int selectType = ((CommonTree)(tree.getChildren().get(0))).getType();
        if(selectType == CmisSqlParser.LIST){
        	List<String> attributeList = new ArrayList<String>();
        	List<CommonTree> cols = ((CommonTree)(tree.getChildren().get(0))).getChildren();
        	for (CommonTree col : cols) {
				attributeList.add((col.getChildren().get(0).toString()));
			}
        	query.setSelectAttributes(attributeList);
        }
        
        buildCriteria((CommonTree)tree.getFirstChildWithType(CmisSqlParser.WHERE), query);
        
//        ((CommonTree)ret.getStart()).getChildren()
        if(tableName.equalsIgnoreCase(BuilderType.FILE_ASSET.toString())){
        	query.setBuilderType(BuilderType.FILE_ASSET);
        }else if(tableName.equalsIgnoreCase(BuilderType.HTMLPAGE.toString())){
        	query.setBuilderType(BuilderType.HTMLPAGE);
        }
        else if(tableName.equalsIgnoreCase(BuilderType.STRUCTURE.toString())){
        	query.setBuilderType(BuilderType.STRUCTURE);	
        }
        else if(tableName.equalsIgnoreCase(BuilderType.MENU_LINK.toString())){
        	query.setBuilderType(BuilderType.MENU_LINK);	
        }
        else if(tableName.equalsIgnoreCase(BuilderType.FOLDER.toString())){
        	query.setBuilderType(BuilderType.FOLDER);
        }else {
        	Structure s = StructureCache.getStructureByVelocityVarName(tableName);
        	if(!UtilMethods.isSet(s)){
        		Logger.error(this, "table name doesn't exist");
        		throw new ValidationException("table name doesn't exist");
        	}
        	query.setBuilderType(BuilderType.CONTENTLET);
        }
	}

	private void buildCriteria(CommonTree whereClause, Query query)throws ValidationException{
		if(whereClause == null){
			return;
		}
		if(whereClause.getChild(0).getChildCount() < 2){
			query.setCriteria(buildSimpleCriteria(((CommonTree)whereClause.getChild(0)).getChildren()));
		}else{
			query.setCriteria(buildComplexCriteria((whereClause.getChildren()), null));
		}
	}
	
	private SimpleCriteria buildSimpleCriteria(List<CommonTree> simpleClause) throws ValidationException{
		String attribute;
		Operator op;
		Object val;
		attribute = simpleClause.get(1).getChild(0).getText();
		op = Operator.getOperator(simpleClause.get(0).getText());
		if(op == null){
			throw new ValidationException("The operator cannot be found");
		}
		if(simpleClause.get(2).getType() == CmisSqlParser.NUM_LIT){
			val = new Long(simpleClause.get(2).getText());
		}else if(simpleClause.get(2).getText().equalsIgnoreCase("true") || simpleClause.get(2).getText().equalsIgnoreCase("false")) {
			val = new Boolean(simpleClause.get(2).getText());
		}else{
			//val = simpleClause.get(2).getText().substring(1, simpleClause.get(2).getText().length() - 1);
			val = simpleClause.get(2).getText();
			if(((String)val).startsWith(new Character('"').toString()) || ((String)val).startsWith("'")){
				val = ((String)val).substring(1);
			}
			if(((String)val).endsWith(new Character('"').toString()) || ((String)val).endsWith("'")){
				val = ((String)val).substring(0,((String)val).length() - 1);
			}
		}
		return  new SingleCriteria(attribute, op, val);
	}
	
	private ComplexCriteria buildComplexCriteria(List<CommonTree> complexClause, GroupingCriteria complexCriteria){
		int previousType = 0;
		for (CommonTree commonTree : complexClause) {
			if(commonTree.getType() == CmisSqlParser.BIN_OP){
				if(complexCriteria == null){
					complexCriteria = new GroupingCriteria(buildSimpleCriteria(commonTree.getChildren()));
				}else{
					if(commonTree.getParent().getType() == CmisSqlParser.AND){
						complexCriteria.addAndCriteria(buildSimpleCriteria(commonTree.getChildren()));
					}else if(commonTree.getParent().getType() == CmisSqlParser.OR){
						complexCriteria.addOrCriteria(buildSimpleCriteria(commonTree.getChildren()));
					}else{
						if(previousType == CmisSqlParser.AND){
							complexCriteria.addAndCriteria(buildSimpleCriteria(commonTree.getChildren()));
						}else if(previousType == CmisSqlParser.OR){
							complexCriteria.addOrCriteria(buildSimpleCriteria(commonTree.getChildren()));
						}
					}
				}
			}else{
				if(complexCriteria == null){
					complexCriteria = new GroupingCriteria(buildComplexCriteria(commonTree.getChildren(), null));
				}else{
					if(commonTree.getType() == CmisSqlParser.AND || commonTree.getType() == CmisSqlParser.OR){
						previousType = commonTree.getType();
						continue;
					}else if(commonTree.getParent().getType() == CmisSqlParser.AND){
						complexCriteria.addAndCriteria(buildComplexCriteria(commonTree.getChildren(), null));
					}else if(commonTree.getParent().getType() == CmisSqlParser.OR){
						complexCriteria.addOrCriteria(buildComplexCriteria(commonTree.getChildren(), null));
					}
				}
			}
		}
		return complexCriteria;
	}
}
