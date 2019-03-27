/**
 * 
 */
package com.dotmarketing.business;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;



/**
 * @author jtesser
 * All methods in the BaseInodeAPI should be protected or private. The BaseInodeAPI is intended to be extended by other APIs for Inode Objects. 
 */
public abstract class BaseInodeAPI {
	


	protected void saveInode(Inode inode) throws DotDataException {
		HibernateUtil.save(inode);
	}
}
