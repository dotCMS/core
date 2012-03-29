package com.dotmarketing.util;

import java.util.Comparator;

import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;



/**
 * @author will
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class AssetsComparator implements Comparator {

	private int direction = 1;
	public AssetsComparator(int direction){
		this.direction = direction;
	}

	public AssetsComparator(){
	}

	public int compare(Object o1, Object o2) {

		if ((o1 instanceof WebAsset) && (o2 instanceof WebAsset)) {

			if (((WebAsset)o1).getSortOrder() < ((WebAsset)o2).getSortOrder()) return -1*direction;
			if (((WebAsset)o1).getSortOrder() == ((WebAsset)o2).getSortOrder()) return 0;
			if (((WebAsset)o1).getSortOrder() > ((WebAsset)o2).getSortOrder()) return 1*direction;

			return -1;
		}
		else if ((o1 instanceof WebAsset) && (o2 instanceof Folder)) {

			if (((WebAsset)o1).getSortOrder() < ((Folder)o2).getSortOrder()) return -1*direction;
			if (((WebAsset)o1).getSortOrder() == ((Folder)o2).getSortOrder()) return 0;
			if (((WebAsset)o1).getSortOrder() > ((Folder)o2).getSortOrder()) return 1*direction;

			return -1;

		}
		else if ((o1 instanceof Folder) && (o2 instanceof WebAsset)) {

			if (((Folder)o1).getSortOrder() < ((WebAsset)o2).getSortOrder()) return -1*direction;
			if (((Folder)o1).getSortOrder() == ((WebAsset)o2).getSortOrder()) return 0;
			if (((Folder)o1).getSortOrder() > ((WebAsset)o2).getSortOrder()) return 1*direction;

			return -1;
		}
		else if ((o1 instanceof Folder) && (o2 instanceof Folder)) {

			if (((Folder)o1).getSortOrder() < ((Folder)o2).getSortOrder()) return -1*direction;
			if (((Folder)o1).getSortOrder() == ((Folder)o2).getSortOrder()) return 0;
			if (((Folder)o1).getSortOrder() > ((Folder)o2).getSortOrder()) return 1*direction;

			return -1;
		}
		else if ((o1 instanceof FileAsset) && (o2 instanceof FileAsset)) {

			if (((FileAsset)o1).getSortOrder() < ((FileAsset)o2).getSortOrder()) return -1*direction;
			if (((FileAsset)o1).getSortOrder() == ((FileAsset)o2).getSortOrder()) return 0;
			if (((FileAsset)o1).getSortOrder() > ((FileAsset)o2).getSortOrder()) return 1*direction;

			return -1;
		}
		else if ((o1 instanceof FileAsset) && (o2 instanceof Folder)) {

			if (((FileAsset)o1).getSortOrder() < ((Folder)o2).getSortOrder()) return -1*direction;
			if (((FileAsset)o1).getSortOrder() == ((Folder)o2).getSortOrder()) return 0;
			if (((FileAsset)o1).getSortOrder() > ((Folder)o2).getSortOrder()) return 1*direction;

			return -1;

		}
		else if ((o1 instanceof Folder) && (o2 instanceof FileAsset)) {

			if (((Folder)o1).getSortOrder() < ((FileAsset)o2).getSortOrder()) return -1*direction;
			if (((Folder)o1).getSortOrder() == ((FileAsset)o2).getSortOrder()) return 0;
			if (((Folder)o1).getSortOrder() > ((FileAsset)o2).getSortOrder()) return 1*direction;

			return -1;
		}
		else if ((o1 instanceof FileAsset) && (o2 instanceof WebAsset)) {

			if (((FileAsset)o1).getSortOrder() < ((WebAsset)o2).getSortOrder()) return -1*direction;
			if (((FileAsset)o1).getSortOrder() == ((WebAsset)o2).getSortOrder()) return 0;
			if (((FileAsset)o1).getSortOrder() > ((WebAsset)o2).getSortOrder()) return 1*direction;

			return -1;

		}
		else if ((o1 instanceof WebAsset) && (o2 instanceof FileAsset)) {

			if (((WebAsset)o1).getSortOrder() < ((FileAsset)o2).getSortOrder()) return -1*direction;
			if (((WebAsset)o1).getSortOrder() == ((FileAsset)o2).getSortOrder()) return 0;
			if (((WebAsset)o1).getSortOrder() > ((FileAsset)o2).getSortOrder()) return 1*direction;

			return -1;
		}

		else {
			return -1;
		}

	}



}
