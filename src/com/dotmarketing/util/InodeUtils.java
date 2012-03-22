package com.dotmarketing.util;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.model.Click;
import com.dotmarketing.portlets.campaigns.model.Recipient;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.communications.model.Communication;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.report.model.Report;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;

public class InodeUtils {

	/**
	 * Checks whether the inodeStr isSet or not.
	 * 
	 * @param inodeStr
	 *            Inode.inode which is a String.
	 * 
	 * @return the value <code>True</code> if the inodeStr is a in valid
	 *         UUID.toString() format or inodeStr is a positive long. else the
	 *         value <code>False</code>.
	 * 
	 */
	public static final boolean isSet(String inodeStr) {

		if (inodeStr == null)
			return false;

		inodeStr = inodeStr.toLowerCase();
		inodeStr = inodeStr.trim();

		if (inodeStr.equals("null") || inodeStr.equals("0") ||
			inodeStr.equals("") || inodeStr.equals("''")) {
			return false;
		}

		return (inodeStr.length() > 0);
	}

	/**
	 * Compares two inodes for ordering.
	 * 
	 * @param inodeStr1
	 *            Inode.inode which is a String.
	 * @param inodeStr2
	 *            Inode.inode which is a String.
	 * 
	 * @return the value <code>0</code> if the Inode corresponding to inodeStr1
	 *         is created at the same time as Inode corresponding to inodeStr2.
	 *         a value less than <code>0</code> if the Inode corresponding to
	 *         inodeStr1 is created before the Inode corresponding to inodeStr2.
	 *         and a value greater than <code>0</code> if the Inode
	 *         corresponding to inodeStr1 is created after the Inode
	 *         corresponding to inodeStr2.
	 * 
	 */
	public static final int compareInodes(String inodeStr1, String inodeStr2) {

		int result = 0;

		if (isSet(inodeStr1) && isSet(inodeStr2)) {
			Inode inodeObj1 = InodeFactory.getInode(inodeStr1, Inode.class);
			Inode inodeObj2 = InodeFactory.getInode(inodeStr2, Inode.class);
			result = inodeObj1.getiDate().compareTo(inodeObj2.getiDate());
		}

		return result;
	}

	public static Class getClassByDBType(String type) {

		if (type == null) {
			return Inode.class;
		}
		if (type.equals("contentlet")) {
			return Contentlet.class;
		}
		if (type.equals("identifier")) {
			return Identifier.class;
		}

		else if (type.equals("template")) {
			return Template.class;
		} else if (type.equals("virtual_link")) {
			return VirtualLink.class;
		} else if (type.equals("user_proxy")) {
			return UserProxy.class;
		} else if (type.equals("structure")) {
			return Structure.class;
		} else if (type.equals("workflow_task")) {
			return WorkflowTask.class;
		} else if (type.equals("file_asset")) {
			return com.dotmarketing.portlets.files.model.File.class;
		} else if (type.equals("relationship")) {
			return Relationship.class;
		}

		else if (type.equals("mailing_list")) {
			return MailingList.class;
		} else if (type.equals("field")) {
			return Field.class;
		} else if (type.equals("htmlpage")) {
			return HTMLPage.class;
		} else if (type.equals("workflow_comment")) {
			return WorkflowComment.class;
		}		

		else if (type.equals("workflow_history")) {
			return WorkflowHistory.class;
		}

		else if (type.equals("folder")) {
			return Folder.class;
		}

		else if (type.equals("category")) {
			return Category.class;
		} else if (type.equals("click")) {
			return Click.class;
		} else if (type.equals("recipient")) {
			return Recipient.class;
		} else if (type.equals("communication")) {
			return Communication.class;
		} else if (type.equals("campaign")) {
			return Campaign.class;
		} else if (type.equals("template")) {
			return Template.class;
		} else if (type.equals("containers")) {
			return Container.class;
		} else if (type.equals("links")) {
			return Link.class;
		} else if (type.equals("report_asset")) {
			return Report.class;
		} else {

			return Inode.class;
		}

	}
}
