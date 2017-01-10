package net.sourceforge.squirrel_sql.plugins.mssql.tokenizer;

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
import net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.fw.sql.IQueryTokenizer;
import net.sourceforge.squirrel_sql.fw.sql.ITokenizerFactory;
import net.sourceforge.squirrel_sql.fw.sql.QueryTokenizer;

public class MSSQLQueryTokenizer extends QueryTokenizer implements
		IQueryTokenizer {
	/** Logger for this class. */
	// @SuppressWarnings("unused")
	// private final static ILogger s_log =
	// LoggerController.createLogger(MSSQLQueryTokenizer.class);
	/** the preference bean */
	private IQueryTokenizerPreferenceBean _prefs = null;

	public MSSQLQueryTokenizer(IQueryTokenizerPreferenceBean prefs) {
		super(prefs);
		_prefs = prefs;
	}

	public void setScriptToTokenize(String script) {
		super.setScriptToTokenize(script);

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
				return new MSSQLQueryTokenizer(_prefs);
			}
		};
	}
}
