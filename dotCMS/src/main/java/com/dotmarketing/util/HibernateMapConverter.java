package com.dotmarketing.util;

import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.mapper.Mapper;
import com.dotcms.repackage.net.sf.hibernate.collection.Map;

public class HibernateMapConverter extends MapConverter {

    public HibernateMapConverter(Mapper mapper) {
        super(mapper);
    }

    public boolean canConvert(Class type) {
        return super.canConvert(type) || type == Map.class; 
    }
}