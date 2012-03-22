package net.sourceforge.squirrel_sql.plugins.mysql.tokenizer;
/*
 * Copyright (C) 2007 Rob Manning
 * manningr@users.sourceforge.net
 * 
 * Based on initial work from Johan Compagner.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.fw.sql.IQueryTokenizer;
import net.sourceforge.squirrel_sql.fw.sql.ITokenizerFactory;
import net.sourceforge.squirrel_sql.fw.sql.QueryTokenizer;
import net.sourceforge.squirrel_sql.fw.util.StringUtilities;

import com.dotmarketing.util.Logger;

/**
 * This class is loaded by the MySQL Plugin and registered with all MySQL 
 * Sessions as the query tokenizer if the plugin is loaded.  It handles some
 * of the syntax allowed in MySQL scripts that would be hard to parse in a 
 * generic way for any database.  It handles create statements for stored 
 * procedures, triggers, and functions.  It can also
 * handle "/" as the statement terminator in leiu of or in addition to the 
 * default statement separator which is ";". 
 *  
 * @author manningr
 */
public class MysqlQueryTokenizer extends QueryTokenizer implements IQueryTokenizer
{
    /** Logger for this class. */
//    private final static ILogger s_log =
//        LoggerController.createLogger(MysqlQueryTokenizer.class);
    
    private static final String PROCEDURE_PATTERN = 
        "^\\s*CREATE\\s+PROCEDURE.*";

    private static final String FUNCTION_PATTERN = 
        "^\\s*CREATE\\s+FUNCTION.*";    

    private static final String TRIGGER_PATTERN = 
        "^\\s*CREATE\\s+TRIGGER.*";    
    
    private Pattern procPattern = Pattern.compile(PROCEDURE_PATTERN, Pattern.DOTALL);
    
    private Pattern funcPattern = Pattern.compile(FUNCTION_PATTERN, Pattern.DOTALL);
    
    private Pattern triggerPattern = Pattern.compile(TRIGGER_PATTERN, Pattern.DOTALL);
    
    
    private IQueryTokenizerPreferenceBean _prefs = null;
    
	public MysqlQueryTokenizer(IQueryTokenizerPreferenceBean prefs)
	{
        super(prefs.getStatementSeparator(),
              prefs.getLineComment(), 
              prefs.isRemoveMultiLineComments());
        _prefs = prefs;
	}

    public void setScriptToTokenize(String script) {
        super.setScriptToTokenize(script);
        
        // Since it is likely to have the procedure separator on it's own line, 
        // and it is key to letting us know that proceeding statements form a 
        // multi-statement procedure or function, it deserves it's own place in 
        // the _queries arraylist.  If it is followed by other procedure or 
        // function creation blocks, we may fail to detect that, so this just 
        // goes through the list and breaks apart statements on newline so that 
        // this cannot happen.
        breakApartNewLines();
        
        // MySQL allows statement separators in procedure blocks.  The process
        // of tokenizing above renders these procedure blocks as separate 
        // statements, which are not valid to be executed separately.  Here, we 
        // re-assemble any create procedure/function/trigger statements that we 
        // find using the beginning procedure block pattern and the procedure 
        // separator. 
        joinFragments(procPattern, false);
        joinFragments(funcPattern, false);
        joinFragments(triggerPattern, false);
        
        _queryIterator = _queries.iterator();
    }
    
    /**
     * Sets the ITokenizerFactory which is used to create additional instances
     * of the IQueryTokenizer - this is used for handling file includes
     * recursively.  
     */    
	protected void setFactory() {
	    _tokenizerFactory = new ITokenizerFactory() {
	        public IQueryTokenizer getTokenizer() {
	            return new MysqlQueryTokenizer(_prefs);
            }
        };
    }
        
    
    /** 
     * This will loop through _queries and break apart lines that look like
     * 
     *   <sep>\n\ncreate proc...
     * into
     * 
     *   <sep>
     *   create proc...
     */
    private void breakApartNewLines() {
        ArrayList<String> tmp = new ArrayList<String>();
        String procSep = _prefs.getProcedureSeparator();
        for (Iterator<String> iter = _queries.iterator(); iter.hasNext();) {
            String next = iter.next();
            if (next.startsWith(procSep)) {
                tmp.add(procSep);
                String[] parts = next.split(procSep+"\\n+");
                for (int i = 0; i < parts.length; i++) {
                    if (!"".equals(parts[i]) && !procSep.equals(parts[i])) {
                        tmp.add(parts[i]);
                    }
                }
            } else if (!next.toLowerCase().startsWith("insert") && !next.toLowerCase().startsWith("update") && !next.toLowerCase().startsWith("delete") && next.endsWith(procSep)) { 
                String chopped = StringUtilities.chop(next);
                tmp.add(chopped);
                tmp.add(procSep);
            } else if (!next.toLowerCase().startsWith("insert") && !next.toLowerCase().startsWith("update") && !next.toLowerCase().startsWith("delete") && next.indexOf(procSep) != -1 ) {
                String[] parts = next.split("\\"+procSep);
                for (int i = 0; i < parts.length; i++) {
                    tmp.add(parts[i]);
                    if (i < parts.length - 1) {
                        tmp.add(procSep);
                    }
                }
            } else {
                tmp.add(next);
            }
        }
        _queries = tmp;
    }
    
    /**
     * This will scan the _queries list looking for fragments matching the 
     * specified pattern and will combine successive fragments until the 
     * procedure separator is found, indicating the end of the code block.    
     * 
     * @param skipStraySep if we find a procedure separator before matching a 
     *                     pattern and this is true, we will exclude it from 
     *                     our list of sql queries.
     */
    private void joinFragments(Pattern pattern, boolean skipStraySep) {
        
        boolean inMultiSQLStatement = false;
        StringBuilder collector = null;
        ArrayList<String> tmp = new ArrayList<String>();
        String procSep = _prefs.getProcedureSeparator();
        String stmtSep = _prefs.getStatementSeparator();
        for (Iterator<String> iter = _queries.iterator(); iter.hasNext();) {
            String next = iter.next();
            
            // DELIMITER sets the separator that tells us when a procedure.  
            // This is MySQL-specific
            if (next.startsWith("DELIMITER")) {
                String[] parts = StringUtilities.split(next, ' ', true);
                if (parts.length == 2) {
                    procSep = parts[1];
                } else {
                    Logger.error(this,
                        "Found DELIMITER keyword, followed by "+
                        (parts.length-1)+" elements; expected only one: "+next+
                        "\nSkipping DELIMITER directive.");
                }
            }
            
            if (pattern.matcher(next.toUpperCase()).matches()) {
                inMultiSQLStatement = true;
                collector = new StringBuilder(next);
                collector.append(stmtSep);
                continue;
            } 
            if (next.startsWith(procSep)) {
                inMultiSQLStatement = false;
                if (collector != null) {
                    tmp.add(collector.toString());
                    collector = null;
                } else {
                    if (skipStraySep) {
                        // Stray sep - or we failed to find pattern
                        //if (s_log.isDebugEnabled()) {
                            Logger.debug(this,
                                "Detected stray proc separator("+procSep+"). Skipping");
                        //}
                    } else {
                        tmp.add(next);
                    }
                }
                continue;
            }
            if (inMultiSQLStatement) {
                collector.append(next);
                collector.append(stmtSep);
                continue;
            } 
            tmp.add(next);
        }
        // We got to the end of the script without finding a proc separator.
        // Just add it as if we had.
        if (collector != null && inMultiSQLStatement) {
            tmp.add(collector.toString());
        }
        _queries = tmp;
    }    
}
