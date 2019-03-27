package com.dotmarketing.startup.runonce;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * Set the default icons for the basic CMS Tabs layouts
 * @author oswaldogallango
 *
 */
public class Task03740UpdateLayoutIcons implements StartupTask {

	private Map<String, String> layoutIcons = new HashMap<String,String>();
	
	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		//update cms_layout description field to set the layout icon to use
		getLayoutsIcons();
		DotConnect dc=new DotConnect();
		for(String key : layoutIcons.keySet()){
			dc.setSQL("update cms_layout set description=? where layout_name=?");
			dc.addParam(layoutIcons.get(key));
			dc.addParam(key);
			dc.loadResult();
		}	
	}
	
	/**
	 * Default Layout Icons
	 */
	private void getLayoutsIcons(){
		layoutIcons.put("Home","fa-home");
		layoutIcons.put("Content Types","fa-file-text");
		layoutIcons.put("Content","fa-folder-open");
		layoutIcons.put("Forms & Polls","fa-file-text");
		layoutIcons.put("Marketing","fa-shopping-cart");
		layoutIcons.put("Site Editor","fa-sitemap");
		layoutIcons.put("Site Browser","fa-sitemap");
		layoutIcons.put("CMS Admin","fa-cog");
		layoutIcons.put("System","fa-cog");
	}

}
