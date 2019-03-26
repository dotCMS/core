package com.dotmarketing.portlets.languagesmanager.business;

import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import java.util.Comparator;

public class LanguageKeyComparator implements Comparator<LanguageKey> {

  public int compare(LanguageKey o1, LanguageKey o2) {
    return o1.getKey().compareTo(o2.getKey());
  }
}
