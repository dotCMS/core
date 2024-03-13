package com.dotcms.util.xstream;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.mapper.Mapper;

public class ContentletHashMapConverter extends MapConverter {

    public ContentletHashMapConverter(Mapper mapper) {
        super(mapper, Contentlet.ContentletHashMap.class);
    }

    @Override
    public boolean canConvert(Class type) {
       return super.canConvert(type);
    }
}
