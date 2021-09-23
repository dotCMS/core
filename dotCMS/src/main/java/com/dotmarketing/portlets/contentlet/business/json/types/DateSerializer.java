package com.dotmarketing.portlets.contentlet.business.json.types;

import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.time.FastDateFormat;

public class DateSerializer implements DataTypeSerializer<Date,String>{

    public static final String PATTERN = "MM-dd-yyyy hh:mm:ss z";

    @Override
    public String write(Date in) {
        return FastDateFormat.getInstance(PATTERN).format(in);
    }

    @Override
    public Date read(String in) {
        return Try.of(()->new SimpleDateFormat(PATTERN).parse(in)).getOrElseThrow(DotRuntimeException::new);
    }
}
