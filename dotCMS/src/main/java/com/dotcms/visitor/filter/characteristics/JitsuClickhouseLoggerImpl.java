package com.dotcms.visitor.filter.characteristics;

import com.dotcms.clickhouse.util.BundleConfigProperties;
import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.tinylog.Logger;

public class JitsuClickhouseLoggerImpl implements ClickHouseLogger {

    private final String DOTVISITOR_IS_LOGGED = "DOTVISITOR_IS_LOGGED";

    private final static List<Class> charactersToApply =
                    ImmutableList.of(GDPRCharacter.class, 
                                    VisitorCharacter.class, 
                                    GeoCharacter.class, 
                                    HeaderCharacter.class,
                                    ParamsCharacter.class, 
                                    IsBotCharacter.class, 
                                    CookieCharacter.class,
                                    RulesEngineCharacter.class);

    final List<Constructor<AbstractCharacter>> constructors;

    private final String[] ignores;



    public JitsuClickhouseLoggerImpl() {
        this(charactersToApply);

    }

    public JitsuClickhouseLoggerImpl(List<Class> applyThese) {
        this.ignores = getIgnoredPatterns();
        constructors = Try.of(() -> getConstructors(applyThese)).getOrElseThrow(e -> new DotRuntimeException(e));



    }

    List<Constructor<AbstractCharacter>> getConstructors(List<Class> applyThese) throws NoSuchMethodException, SecurityException {
        List<Constructor<AbstractCharacter>> cons = new ArrayList<>();
        for (Class clazz : applyThese) {
            cons.add(clazz.getConstructor(AbstractCharacter.class));
        }
        return ImmutableList.copyOf(cons);
    }

    private static Lazy<ObjectMapper> mapper = Lazy.of(() -> {
        ObjectMapper newMapper = new ObjectMapper();
        newMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        newMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        newMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        return newMapper;

    });

    private final static String IGNORE_STARTSWITH_PATTERNS = "IGNORE_STARTSWITH_PATTERNS";

    private String[] getIgnoredPatterns() {
        String startsWiths = BundleConfigProperties.getProperty(IGNORE_STARTSWITH_PATTERNS, null);
        if (startsWiths == null) {
            return new String[] {};
        }
        return Arrays.asList(startsWiths.split(",")).stream().map(s -> s.trim()).collect(Collectors.toList()).stream()
                        .toArray(String[]::new);

    }



    @Override
    public void log(final HttpServletRequest request, final HttpServletResponse response) {

        if (!shouldLog(request, response)) {
            return;
        }
        try {
            logInternal(request, response);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

        }
    }

    private void logInternal(final HttpServletRequest request, final HttpServletResponse response)
                    throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException {

        AbstractCharacter base = new BaseCharacter(request, response);
        try {
            for (Constructor<AbstractCharacter> con : constructors) {
                base = con.newInstance(base);
            }

            doLog(mapper.get().writeValueAsString(new ClickHouseCharacter(base).getMap()));

        } catch (JsonProcessingException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void doLog(Object message) {
        System.out.println(message);
        Logger.info(message);
    }

    private boolean shouldLog(HttpServletRequest request, HttpServletResponse response) {
        if (request == null || response.equals(null)) {
            return false;
        } else if (500 == response.getStatus()) {
            return false;
        } else if (request.getAttribute(DOTVISITOR_IS_LOGGED) != null) {
            return false;
        } else if (ignores.length > 0) {
            for (String ignore : ignores) {
                if (request.getRequestURI().startsWith(ignore)) {
                    return false;
                }
            }
        }
        request.setAttribute(DOTVISITOR_IS_LOGGED, true);
        return true;

    }

}
