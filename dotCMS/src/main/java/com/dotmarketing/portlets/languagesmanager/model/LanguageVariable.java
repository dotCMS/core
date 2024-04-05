package com.dotmarketing.portlets.languagesmanager.model;

import com.dotcms.keyvalue.model.KeyValue;

public interface LanguageVariable extends KeyValue, Comparable<LanguageVariable> {

    String getLanguageCode();

    String getCountryCode();

    default int compareTo(LanguageVariable o) {
        return this.getKey().compareTo(o.getKey());
    }

}
