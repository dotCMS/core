package net.sourceforge.squirrel_sql.plugins.oracle.prefs;

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
import java.util.TimeZone;

import net.sourceforge.squirrel_sql.fw.preferences.BaseQueryTokenizerPreferenceBean;

/**
 * A bean class to store preferences for the Oracle plugin.
 */
public class OraclePreferenceBean extends BaseQueryTokenizerPreferenceBean implements Cloneable, Serializable {

    static final long serialVersionUID = 5818886723165356478L;

    static final String UNSUPPORTED = "Unsupported";

    private boolean excludeRecycleBinTables = true;
    
    private boolean showErrorOffset = true;
    
    private boolean initSessionTimezone = true;
    
    private String sessionTimezone = TimeZone.getDefault().getID();
    
    public OraclePreferenceBean() {
        super();
        statementSeparator = ";";
        procedureSeparator = "/";
        lineComment = "--";
        removeMultiLineComments = false;
        installCustomQueryTokenizer = true;
    }

    /**
     * Return a copy of this object.
     */
    public OraclePreferenceBean clone() {
   	 return (OraclePreferenceBean) super.clone();
    }

    /**
     * @param excludeRecycleBinTables the excludeRecycleBinTables to set
     */
    public void setExcludeRecycleBinTables(boolean excludeRecycleBinTables) {
        this.excludeRecycleBinTables = excludeRecycleBinTables;
    }

    /**
     * @return the excludeRecycleBinTables
     */
    public boolean isExcludeRecycleBinTables() {
        return excludeRecycleBinTables;
    }

   /**
    * @return the showErrorOffset
    */
   public boolean isShowErrorOffset() {
      return showErrorOffset;
   }

   /**
    * @param showErrorOffset the showErrorOffset to set
    */
   public void setShowErrorOffset(boolean showErrorOffset) {
      this.showErrorOffset = showErrorOffset;
   }

	/**
	 * @param sessionTimezone the sessionTimezone to set
	 */
	public void setSessionTimezone(String sessionTimezone)
	{
		this.sessionTimezone = sessionTimezone;
	}

	/**
	 * @return the sessionTimezone
	 */
	public String getSessionTimezone()
	{
		return sessionTimezone;
	}

	/**
	 * @param initSessionTimezone the initSessionTimezone to set
	 */
	public void setInitSessionTimezone(boolean initSessionTimezone)
	{
		this.initSessionTimezone = initSessionTimezone;
	}

	/**
	 * @return the getInitSessionTimezone
	 */
	public boolean getInitSessionTimezone()
	{
		return initSessionTimezone;
	}

}
