package com.dotcms.javascript.app.util;

import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Console {

    public void log (final Object... objects) {

        Logger.info(Console.class, ()->
                Arrays.asList(objects).stream().map(Object::toString).collect(Collectors.joining(StringPool.COMMA)));
    }

}
