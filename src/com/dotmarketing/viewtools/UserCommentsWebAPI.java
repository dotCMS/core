package com.dotmarketing.viewtools;

/**
 * This is the webAPI thatr let the developer call the UserCommentsFactory from the velocity pages
 * @author Salvador Di Nardo
 * since 1.5
 * @version 1.5
 * @see com.dotmarketing.portlets.user.factories.UserCommentsFactory
 * @see com.dotmarketing.portlets.user.model.UserComment
 */

import java.util.List;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.portlets.user.factories.UserCommentsFactory;
import com.dotmarketing.portlets.user.model.UserComment;

public class UserCommentsWebAPI implements ViewTool
{

	public void init(Object arg0) 
	{
	}

	/*
	 * Returns all the UserComments of an user order by date desc
	 * @param the user inode as a long
	 * @return a List<UserComments> that represent the UserComments of the user
	 * 
	 */
    @Deprecated 
	public List<UserComment> getUserComments(long userInode)
	{
		String userInodeString = Long.toString(userInode);
		return getUserComments(userInodeString);
	}
	
	/*
	 * Returns all the UserComments of an user order by date desc
	 * @param the user inode as a String
	 * @return a List<UserComments> that represent the UserComments of the user
	 */

	public List<UserComment> getUserComments(String userInode)
	{
		return UserCommentsFactory.getUserCommentsByUserInode(userInode);
	}

}
