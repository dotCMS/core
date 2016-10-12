package com.dotcms.util.marshal;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.repackage.com.google.gson.Gson;
import com.dotmarketing.exception.DotRuntimeException;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import org.apache.velocity.runtime.parser.ParseException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static com.dotcms.util.CollectionsUtils.list;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;


public class MarshalFactoryTest {

    @Test
    public void marshalMapTest() throws ParseException, JSONException {

        final MarshalFactory marshalFactory =
                MarshalFactory.getInstance();

        assertNotNull(marshalFactory);

        final MarshalUtils marshalUtils =
                marshalFactory.getMarshalUtils();

        assertNotNull(marshalUtils);

        Contentlet contentlet = new Contentlet();
        contentlet.setIdentifier("1");
        contentlet.setLowIndexPriority(true);
        contentlet.setDisabledWysiwyg(list("AAAA", "BBBB"));
        contentlet.setInode("iiii");
        contentlet.setProperty("hostName", "WWWWW");

        String json = marshalUtils.marshal(contentlet);

        Contentlet contentlet1 = marshalUtils.unmarshal(json, Contentlet.class);
        assertEquals("1", contentlet.getIdentifier());
        assertEquals(true, contentlet.isLowIndexPriority());
        assertEquals("iiii", contentlet.getInode());
        assertEquals("WWWWW", contentlet.getStringProperty("hostName"));


        List<String> disabledWysiwyg = contentlet1.getDisabledWysiwyg();
        assertEquals("AAAA", disabledWysiwyg.get(0));
        assertEquals("BBBB", disabledWysiwyg.get(1));
    }
}
