package com.dotmarketing.portlets.languagesmanager.business;

import com.dotmarketing.portlets.languagesmanager.model.LanguageVariable;
import java.util.Comparator;

public class LanguageVariableComparator implements Comparator<LanguageVariable> {
    public int compare(LanguageVariable o1, LanguageVariable o2) {
        return o1.getKey().compareTo(o2.getKey());
    }

}
