package net.sourceforge.squirrel_sql.plugins.mssql.prefs;
/*
 * Copyright (C) 2007 Rob Manning
 * manningr@users.sourceforge.net
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
import java.io.Serializable;

import net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean;

/**
 * A bean class to store preferences for the MS SQL-Server plugin.
 */
public class MSSQLPreferenceBean implements Cloneable, 
                                             Serializable,
                                             IQueryTokenizerPreferenceBean {    
    
	static final long serialVersionUID = -8870273703050156986L;

    static final String UNSUPPORTED = "Unsupported";

    /** Client Name. */
	private String _clientName;

	/** Client version. */
	private String _clientVersion;
    
    private String statementSeparator = "GO";
    
    private String lineComment = "--";
    
    private boolean removeMultiLineComments = false;
    
    private boolean installCustomQueryTokenizer = true;
    
    
	public MSSQLPreferenceBean() {
		super();
	}

	/**
	 * Return a copy of this object.
	 */
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new InternalError(ex.getMessage()); // Impossible.
		}
	}

	/**
	 * Retrieve the client to use. This is only
	 * used if <TT>useAnonymousClient</TT> is false.
	 *
	 * @return	Client name.
	 */
	public String getClientName() {
		return _clientName;
	}

	/**
	 * Set the client name.
	 *
	 * @param	value	Client name
	 */
	public void setClientName(String value) {
		_clientName = value;
	}

	/**
	 * Retrieve the client version to use. This is only
	 * used if <TT>useAnonymousLogon</TT> is false.
	 *
	 * @return	Client version.
	 */
	public String getClientVersion() {
		return _clientVersion;
	}

	/**
	 * Set the client version.
	 *
	 * @param	value	Client version
	 */
	public void setClientVersion(String value) {
		_clientVersion = value;
	}

    /**
     * @param statementSeparator the statementSeparator to set
     */
    public void setStatementSeparator(String statementSeparator) {
        this.statementSeparator = statementSeparator;
    }

    /**
     * @return the statementSeparator
     */
    public String getStatementSeparator() {
        return statementSeparator;
    }

    /**
     * @param lineComment the lineComment to set
     */
    public void setLineComment(String lineComment) {
        this.lineComment = lineComment;
    }

    /**
     * @return the lineComment
     */
    public String getLineComment() {
        return lineComment;
    }

    /**
     * @param removeMultiLineComments the removeMultiLineComments to set
     */
    public void setRemoveMultiLineComments(boolean removeMultiLineComments) {
        this.removeMultiLineComments = removeMultiLineComments;
    }

    /**
     * @return the removeMultiLineComments
     */
    public boolean isRemoveMultiLineComments() {
        return removeMultiLineComments;
    }

    /**
     * @param installCustomQueryTokenizer the installCustomQueryTokenizer to set
     */
    public void setInstallCustomQueryTokenizer(boolean installCustomQueryTokenizer) {
        this.installCustomQueryTokenizer = installCustomQueryTokenizer;
    }

    /**
     * @return the installCustomQueryTokenizer
     */
    public boolean isInstallCustomQueryTokenizer() {
        return installCustomQueryTokenizer;
    }

    /**
     * This is not implemented at the moment, but will be soon.
     * @see net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean#getProcedureSeparator()
     */
    public String getProcedureSeparator() {
        return null;
    }

    /**
     * This is not implemented at the moment, but will be soon. 
     * @see net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean#setProcedureSeparator(java.lang.String)
     */
    public void setProcedureSeparator(String procedureSeparator) {
        
    }
	
}

