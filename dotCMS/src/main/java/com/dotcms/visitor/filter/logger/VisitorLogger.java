package com.dotcms.visitor.filter.logger;


import com.dotcms.visitor.business.VisitorAPIImpl;

import com.dotcms.visitor.filter.characteristics.*;
import com.dotmarketing.exception.DotRuntimeException;

import com.dotmarketing.logConsole.model.LogMapper;
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

public class VisitorLogger {

    public static final String VISITOR_LOG_BASE_NAME = "visitor-v3.log";

    private static List<Class> charactersToApply = ImmutableList.of(VisitorCharacter.class, GeoCharacter.class, HeaderCharacter.class,
            ParamsCharacter.class, RulesEngineCharacter.class, GDPRCharacter.class);

    private static List<Constructor<AbstractCharacter>> constructors;

    private static List<Constructor<AbstractCharacter>> customConstructors;

    private VisitorLogger() {

    }

    static {
        try {
            constructors = getConstructors(charactersToApply);
            customConstructors = new ArrayList<>();
        } catch (NoSuchMethodException | SecurityException e) {
            throw new DotRuntimeException(e);
        }
    }


    private static List<Constructor<AbstractCharacter>> getConstructors(List<Class> applyThese) throws NoSuchMethodException, SecurityException {
        List<Constructor<AbstractCharacter>> cons = new ArrayList<>();
        for (Class clazz : applyThese) {
            cons.add(clazz.getConstructor(AbstractCharacter.class));
        }
        return ImmutableList.copyOf(cons);
    }



    private static ObjectMapper mapper;


    private static ObjectMapper mapper() {
        if (mapper == null) {
            ObjectMapper newMapper = new ObjectMapper();
            newMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper = newMapper;
        }
        return mapper;
    }

    /**
     * Method that logs info included on each character implementation
     * @param request
     * @param response
     */
    public static void log(final HttpServletRequest request, final HttpServletResponse response) {

        if (!shouldLog(request, response) || !LogMapper.getInstance()
                .isLogEnabled(VisitorLogger.VISITOR_LOG_BASE_NAME)) {
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

    /**
     * Method that adds a character to the constructor list
     * @param character
     * @return
     * @throws NoSuchMethodException
     */
    public static List<Constructor<AbstractCharacter>> addConstructor(
            Class character) throws NoSuchMethodException {
        customConstructors.add(character.getConstructor(AbstractCharacter.class));
        return ImmutableList.copyOf(customConstructors);
    }

    /**
     * Method that removes a character from the constructor list
     * @param character
     * @return
     * @throws NoSuchMethodException
     */
    public static List<Constructor<AbstractCharacter>> removeConstructor(
            Class character) throws NoSuchMethodException {
        customConstructors.remove(character.getConstructor(AbstractCharacter.class));
        return ImmutableList.copyOf(customConstructors);
    }

    private static void logInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {

        AbstractCharacter base = new BaseCharacter(request, response);
        try {
            for (Constructor<AbstractCharacter> con : constructors) {
                base = con.newInstance(base);
            }

            for (Constructor<AbstractCharacter> con : customConstructors) {
                base = con.newInstance(base);
            }
            doLog(mapper().writeValueAsString(base.getMap()));
        } catch (JsonProcessingException e) {
            throw new DotRuntimeException(e);
        }
    }

    private static void doLog(String message) {
        Logger.info(VisitorLogger.class, message);
    }


    private static boolean shouldLog(HttpServletRequest request, HttpServletResponse response) {
        return 500 != response.getStatus() && new VisitorAPIImpl().getVisitor(request, false).isPresent();
    }



}
