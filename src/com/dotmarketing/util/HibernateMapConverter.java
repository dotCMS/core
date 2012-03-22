package com.dotmarketing.util;

import net.sf.hibernate.collection.Map;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.mapper.Mapper;

public class HibernateMapConverter extends MapConverter {

    public HibernateMapConverter(Mapper mapper) {
        super(mapper);
    }

    public boolean canConvert(Class type) {
        return super.canConvert(type) || type == Map.class; 
    }
}