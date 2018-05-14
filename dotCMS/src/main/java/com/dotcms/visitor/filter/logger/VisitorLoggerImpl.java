package com.dotcms.visitor.filter.logger;


import com.dotcms.visitor.business.VisitorAPIImpl;

import com.dotcms.visitor.filter.characteristics.*;
import com.dotmarketing.exception.DotRuntimeException;

import com.dotmarketing.util.Logger;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;

public class VisitorLoggerImpl implements VisitorLogger {


    private List<Class> charactersToApply = ImmutableList.of(VisitorCharacter.class, GeoCharacter.class, HeaderCharacter.class,
            ParamsCharacter.class, RulesEngineCharacter.class, GDPRCharacter.class);

    final List<Constructor<AbstractCharacter>> constructors;


    public VisitorLoggerImpl() {
        try {
            constructors = getConstructors(charactersToApply);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    public VisitorLoggerImpl(List<Class> applyThese) {
        try {
            constructors = getConstructors(applyThese);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    List<Constructor<AbstractCharacter>> getConstructors(List<Class> applyThese) throws NoSuchMethodException, SecurityException {
        List<Constructor<AbstractCharacter>> cons = new ArrayList<>();
        for (Class clazz : applyThese) {
            cons.add(clazz.getConstructor(AbstractCharacter.class));
        }
        return ImmutableList.copyOf(cons);
    }



    private static ObjectMapper mapper;


    private ObjectMapper mapper() {
        if (mapper == null) {
            ObjectMapper newMapper = new ObjectMapper();
            newMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper = newMapper;
        }
        return mapper;
    }



    @Override
    public void log(final HttpServletRequest request, final HttpServletResponse response) {

        if (!shouldLog(request, response)) {
            return;
        }
        // clear after every request
        try {
            logInternal(request, response);
        } catch (Exception e) {
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
            doLog(mapper().writeValueAsString(base.getMap()));
        } catch (JsonProcessingException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void doLog(String message) {
        Logger.info(this.getClass(), message);
    }


    private boolean shouldLog(HttpServletRequest request, HttpServletResponse response) {
        return 500 != response.getStatus() && new VisitorAPIImpl().getVisitor(request, false).isPresent();
    }



}
