package net.sourceforge.squirrel_sql.plugins.oracle.tokenizer;

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

/**
 * This class is loaded by the Oracle Plugin and registered with all Oracle Sessions as the query tokenizer if
 * the plugin is loaded. It handles some of the syntax allowed in SQL-Plus scripts that would be hard to parse
 * in a generic way for any database. It handles create statements for stored procedures, triggers, functions
 * and anonymous procedure blocks. It can also handle "/" as the statement terminator in leiu of or in
 * addition to the default statement separator which is ";". This class is not meant to fully replicate all of
 * the syntax available in the highly expressive and venerable SQL-Plus reporting tool.
 * 
 * @author manningr
 */
public class OracleQueryTokenizer extends QueryTokenizer implements IQueryTokenizer
{
	/** Logger for this class. */
//	private final static ILogger s_log = LoggerController.createLogger(OracleQueryTokenizer.class);

	private static final String PROCEDURE_PATTERN =
		"^\\s*CREATE\\s+PROCEDURE.*|^\\s*CREATE\\s+OR\\s+REPLACE\\s+PROCEDURE\\s+.*";

	private static final String FUNCTION_PATTERN =
		"^\\s*CREATE\\s+FUNCTION.*|^\\s*CREATE\\s+OR\\s+REPLACE\\s+FUNCTION\\s+.*";

	private static final String TRIGGER_PATTERN =
		"^\\s*CREATE\\s+TRIGGER.*|^\\s*CREATE\\s+OR\\s+REPLACE\\s+TRIGGER\\s+.*";

	private static final String PACKAGE_PATTERN =
		"^\\s*CREATE\\s+PACKAGE.*|^\\s*CREATE\\s+OR\\s+REPLACE\\s+PACKAGE\\s+.*";

	private static final String DECLARE_PATTERN = "^\\s*DECLARE\\s*.*";

	private static final String BEGIN_PATTERN = "^\\s*BEGIN\\s*.*";

	/** Finds any "\n/" (slash) characters on their own line (no sep) */
	private static final String SLASH_PATTERN = ".*\\n/\\n.*";

	/** Finds any "\n/" (slash) characters on their own line (no sep) */
	private static final String SLASH_SPLIT_PATTERN = "\\n/\\n";

	private final String SET_COMMAND_PATTERN = "^\\s*SET\\s+\\w+\\s+\\w+\\s*$";

	private Pattern procPattern = Pattern.compile(PROCEDURE_PATTERN, Pattern.DOTALL);

	private Pattern funcPattern = Pattern.compile(FUNCTION_PATTERN, Pattern.DOTALL);

	private Pattern triggerPattern = Pattern.compile(TRIGGER_PATTERN, Pattern.DOTALL);

	private Pattern packagePattern = Pattern.compile(PACKAGE_PATTERN, Pattern.DOTALL);

	private Pattern declPattern = Pattern.compile(DECLARE_PATTERN, Pattern.DOTALL);

	private Pattern beginPattern = Pattern.compile(BEGIN_PATTERN, Pattern.DOTALL);

	private Pattern slashPattern = Pattern.compile(SLASH_PATTERN, Pattern.DOTALL);

	private Pattern setPattern = Pattern.compile(SET_COMMAND_PATTERN, Pattern.DOTALL);

	private static final String ORACLE_SCRIPT_INCLUDE_PREFIX = "@";

	private IQueryTokenizerPreferenceBean _prefs = null;

	public OracleQueryTokenizer(IQueryTokenizerPreferenceBean prefs)
	{
		super(prefs.getStatementSeparator(), prefs.getLineComment(), prefs.isRemoveMultiLineComments());
		_prefs = prefs;
	}

	public void setScriptToTokenize(String script)
	{
		super.setScriptToTokenize(script);

		removeSqlPlusSetCommands();

		// Since it is likely to have "/" on it's own line, and it is key to
		// letting us know that proceeding statements form a multi-statement
		// procedure or function, it deserves it's own place in the _queries
		// arraylist. If it is followed by other procedure or function creation
		// blocks, we may fail to detect that, so this just goes through the
		// list and breaks apart statements on newline so that this cannot
		// happen.
		breakApartNewLines();

		// Oracle allows statement separators in PL/SQL blocks. The process
		// of tokenizing above renders these procedure blocks as separate
		// statements, which is invalid for Oracle. Since "/" is the way
		// in SQL-Plus to denote the end of a procedure or function, we
		// re-assemble any create procedure/function/trigger statements that we
		// find. This should be done before expanding file includes. Otherwise,
		// any create sql found in files will already be joined, causing this to
		// find create SQL without matching "/". The process of
		// expanding 'file includes' already joins the sql fragments that it
		// finds.
		joinFragments(procPattern, false);
		joinFragments(funcPattern, false);
		joinFragments(triggerPattern, false);
		joinFragments(packagePattern, false);
		joinFragments(declPattern, false);
		joinFragments(beginPattern, true);

		expandFileIncludes(ORACLE_SCRIPT_INCLUDE_PREFIX);

		removeRemainingSlashes();

		_queryIterator = _queries.iterator();
	}

	/**
	 * Bug #1902611: Don't fail on "set" commands in SQL script
	 * SQL-Plus allows various "SET ... " commands that have nothing to do with SQL, but customize the behavior
	 * of SQL-Plus.  For example, you can "SET TIMING ON" to print the time that every sql statement took to 
	 * execute after executing it.  We may want support a subset of these commands in the future, but for now, 
	 * just strip them out so that they don't get sent to Oracle. 
	 */
	private void removeSqlPlusSetCommands()
	{
		ArrayList<String> tmp = new ArrayList<String>();
		for (Iterator<String> iter = _queries.iterator(); iter.hasNext();)
		{
			String next = iter.next();
			String[] parts = next.split("\\n");
			StringBuilder noCommandStr = new StringBuilder();
			for (String part : parts)
			{
				if (!setPattern.matcher(part.toUpperCase()).matches())
				{
					noCommandStr.append(part).append("\n");
				}
			}
			tmp.add(noCommandStr.toString());
		}
		_queries = tmp;
	}

	/**
	 * Sets the ITokenizerFactory which is used to create additional instances of the IQueryTokenizer - this is
	 * used for handling file includes recursively.
	 */
	protected void setFactory()
	{
		_tokenizerFactory = new ITokenizerFactory()
		{
			public IQueryTokenizer getTokenizer()
			{
				return new OracleQueryTokenizer(_prefs);
			}
		};
	}

	/**
	 * This is to take care of scripts that have no statement separators. Like : select * from sometable1 /
	 * select * from sometable2 / SQL-Plus allows "/" to terminate statements as well as multiple statement
	 * blocks, so someone will probably have written a script that does this.
	 */
	private void removeRemainingSlashes()
	{

		ArrayList<String> tmp = new ArrayList<String>();
		boolean foundEOLSlash = false;
		for (Iterator<String> iter = _queries.iterator(); iter.hasNext();)
		{
			String next = iter.next();
			if (slashPattern.matcher(next).matches())
			{
				foundEOLSlash = true;
				String[] parts = next.split(SLASH_SPLIT_PATTERN);
				for (int i = 0; i < parts.length; i++)
				{
					String part = parts[i];
					if (slashPattern.matcher(part).matches())
					{
						int lastIndex = part.lastIndexOf("/");
						tmp.add(part.substring(0, lastIndex));
					} else
					{
						if (part.endsWith("/"))
						{
							part = part.substring(0, part.lastIndexOf("/"));
						}
						tmp.add(part);
					}
				}
			} else if (next.endsWith("/"))
			{
				foundEOLSlash = true;
				int lastIndex = next.lastIndexOf("/");
				tmp.add(next.substring(0, lastIndex));
			} else
			{
				tmp.add(next);
			}
		}
		if (foundEOLSlash == true)
		{
			_queries = tmp;
		}

	}

	/**
	 * This will loop through _queries and break apart lines that look like /\n\ncreate proc... into / create
	 * proc...
	 */
	private void breakApartNewLines()
	{
		ArrayList<String> tmp = new ArrayList<String>();
		String sep = _prefs.getProcedureSeparator();
		for (Iterator<String> iter = _queries.iterator(); iter.hasNext();)
		{
			String next = iter.next();
			if (next.startsWith(sep))
			{
				tmp.add(sep);
				String[] parts = next.split(sep + "\\n+");
				for (int i = 0; i < parts.length; i++)
				{
					if (!"".equals(parts[i]) && !sep.equals(parts[i]))
					{
						tmp.add(parts[i]);
					}
				}
			} else
			{
				tmp.add(next);
			}
		}
		_queries = tmp;
	}

	/**
	 * This will scan the _queries list looking for fragments matching the specified pattern and will combine
	 * successive fragments until the "/" is indicating the end of the code block. This is Oracle-specific.
	 * 
	 * @param skipStraySlash
	 *           if we find a slash before matching a pattern and this is true, we will exclude it from our
	 *           list of sql queries.
	 */
	private void joinFragments(Pattern pattern, boolean skipStraySlash)
	{

		boolean inMultiSQLStatement = false;
		StringBuffer collector = null;
		ArrayList<String> tmp = new ArrayList<String>();
		String sep = _prefs.getProcedureSeparator();
		for (Iterator<String> iter = _queries.iterator(); iter.hasNext();)
		{
			String next = iter.next();
			if (pattern.matcher(next.toUpperCase()).matches())
			{
				inMultiSQLStatement = true;
				collector = new StringBuffer(next);
				collector.append(";");
				continue;
			}
			if (next.startsWith(sep))
			{
				inMultiSQLStatement = false;
				if (collector != null)
				{
					tmp.add(collector.toString());
					collector = null;
				} else
				{
					if (skipStraySlash)
					{
						// Stray sep - or we failed to find pattern
//						if (s_log.isDebugEnabled())
//						{
//							s_log.debug("Detected stray proc separator(" + sep + "). Skipping");
//						}
					} else
					{
						tmp.add(next);
					}
				}
				continue;
			}
			if (inMultiSQLStatement)
			{
				collector.append(next);
				collector.append(";");
				continue;
			}
			tmp.add(next);
		}
		_queries = tmp;
	}
}
