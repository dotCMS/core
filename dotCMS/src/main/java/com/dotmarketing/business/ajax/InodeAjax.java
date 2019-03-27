package com.dotmarketing.business.ajax;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

public class InodeAjax {
	
	public int compareInodes(String inodeStr1,String inodeStr2){
		return InodeUtils.compareInodes(inodeStr1, inodeStr2);
	}
	
	public String getInodeUrl(String inode){


		
		try {

			DotConnect dc = new DotConnect();
			dc.setSQL("select * from inode where inode = ?");
			dc.addParam(inode);
			Identifier id = null;
			IdentifierAPI iapi = APILocator.getIdentifierAPI();
			id= 	iapi.findFromInode(dc.getString("identifier"));
			return id.getURI();
		} catch (Exception e) {
			Logger.warn(this, "Unable to find identifier : " + inode);

		}
		
		
		return "";
		
		
		
	}
	
	

}
