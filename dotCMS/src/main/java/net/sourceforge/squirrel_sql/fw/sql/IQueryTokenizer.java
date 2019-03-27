package net.sourceforge.squirrel_sql.fw.sql;


/**
 * This should be implemented to provide script tokenizing behavior that is 
 * specific to a database.
 * 
 * @author rmmannin
 */
public interface IQueryTokenizer {
    
    /**
     * Returns a boolean value indicating whether or not there are more 
     * statements to be sent to the server.
     * 
     * @return true if nextQuery can be called to get the next statement; 
     *         false otherwise.
     */
    boolean hasQuery();
    
    /**
     * Returns the next statement, or null if there are no more statements to 
     * get.
     * 
     * @return the next statement or null if there is no next statement.
     */
    String nextQuery();
    
    /**
     * Sets the script to be tokenized into one or more queries that should be 
     * sent to the database.  Query here means statement, so this includes more
     * than just select statements.
     * 
     * @param script a string representing one or more SQL statements.
     */
    void setScriptToTokenize(String script);
    
    /**
     * Returns the number of queries that the tokenizer found in the script 
     * given in the last call to setScriptToTokenize, or 0 if 
     * setScriptToTokenize has not yet been called.
     */
    int getQueryCount();
    
    /**
     * Returns the statement separator being used for this session.  This may be
     * the user's preference, or in the case of a plugin that provides a custom
     * IQueryTokenizer implementation, this will be the plugin-specific setting
     * which is also configurable.
     * 
     * @return the string of characters representing a delimiter between multiple
     *         statements. 
     */
    String getSQLStatementSeparator();
    
    /**
     * Returns the string that identifies a line comment.
     *  
     * @return the start of line comment
     */
    String getLineCommentBegin();
    
    /**
     * Returns whether or not the query tokenizer should remove multi-line 
     * comments from the statements while tokenizing them.
     * 
     * @return true if remove; falso otherwise.
     */
    boolean isRemoveMultiLineComment();
}
