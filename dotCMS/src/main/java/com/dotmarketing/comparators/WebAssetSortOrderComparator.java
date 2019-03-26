package com.dotmarketing.comparators;

import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.util.Logger;
import java.util.Comparator;

/**
 * @author Maria
 *     <p>To change this generated comment edit the template variable "typecomment":
 *     Window>Preferences>Java>Templates. To enable and disable the creation of type comments go to
 *     Window>Preferences>Java>Code Generation.
 */
public class WebAssetSortOrderComparator implements Comparator {

  public int compare(Object o1, Object o2) {

    try {
      Logger.debug(this, "Sorting assets per Sort Order!");

      WebAsset w1 = (WebAsset) o1;

      WebAsset w2 = (WebAsset) o2;

      return (w1.getSortOrder() == w2.getSortOrder())
          ? 0
          : (w1.getSortOrder() < w2.getSortOrder()) ? -1 : 1;

    } catch (ClassCastException e) {

    }
    return 0;
  }
}
