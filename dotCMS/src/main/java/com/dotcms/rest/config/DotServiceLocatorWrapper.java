package com.dotcms.rest.config;

import com.dotcms.repackage.org.glassfish.hk2.api.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

public class DotServiceLocatorWrapper implements ServiceLocator {

    private final ServiceLocator serviceLocator;

    public DotServiceLocatorWrapper(final ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public <T> T getService(Class<T> aClass, Annotation... annotations) throws MultiException {
        return serviceLocator.getService(aClass,  annotations);
    }

    @Override
    public <T> T getService(Type type, Annotation... annotations) throws MultiException {
        return serviceLocator.getService(type,  annotations);
    }

    @Override
    public <T> T getService(Class<T> aClass, String s, Annotation... annotations) throws MultiException {
        return serviceLocator.getService(aClass, s,  annotations);
    }

    @Override
    public <T> T getService(Type type, String s, Annotation... annotations) throws MultiException {
        return serviceLocator.getService(type, s,  annotations);
    }

    @Override
    public <T> List<T> getAllServices(Class<T> aClass, Annotation... annotations) throws MultiException {
        return serviceLocator.getAllServices(aClass,  annotations);
    }

    @Override
    public <T> List<T> getAllServices(Type type, Annotation... annotations) throws MultiException {
        return serviceLocator.getAllServices(type,  annotations);
    }

    @Override
    public <T> List<T> getAllServices(Annotation annotation, Annotation... annotations) throws MultiException {
        return serviceLocator.getAllServices(annotation,  annotations);
    }

    @Override
    public List<?> getAllServices(Filter filter) throws MultiException {
        return serviceLocator.getAllServices(filter);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Class<T> aClass, Annotation... annotations) throws MultiException {
        return serviceLocator.getServiceHandle(aClass, annotations);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Type type, Annotation... annotations) throws MultiException {
        return serviceLocator.getServiceHandle(type, annotations);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Class<T> aClass, String s, Annotation... annotations) throws MultiException {
        return serviceLocator.getServiceHandle(aClass, s, annotations);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Type type, String s, Annotation... annotations) throws MultiException {
        return serviceLocator.getServiceHandle(type, s, annotations);
    }

    @Override
    public <T> List<ServiceHandle<T>> getAllServiceHandles(Class<T> aClass, Annotation... annotations) throws MultiException {
        return serviceLocator.getAllServiceHandles(aClass, annotations);
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Type type, Annotation... annotations) throws MultiException {
        return serviceLocator.getAllServiceHandles(type, annotations);
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Annotation annotation, Annotation... annotations) throws MultiException {
        return serviceLocator.getAllServiceHandles(annotation, annotations);
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Filter filter) throws MultiException {
        return serviceLocator.getAllServiceHandles(filter);
    }

    @Override
    public List<ActiveDescriptor<?>> getDescriptors(Filter filter) {
        return serviceLocator.getDescriptors(filter);
    }

    @Override
    public ActiveDescriptor<?> getBestDescriptor(Filter filter) {
        return serviceLocator.getBestDescriptor(filter);
    }

    @Override
    public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor, Injectee injectee) throws MultiException {
        return serviceLocator.reifyDescriptor(descriptor, injectee);
    }

    @Override
    public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor) throws MultiException {
        return serviceLocator.reifyDescriptor(descriptor);
    }

    @Override
    public ActiveDescriptor<?> getInjecteeDescriptor(Injectee injectee) throws MultiException {
        return serviceLocator.getInjecteeDescriptor(injectee);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor, Injectee injectee) throws MultiException {
        return serviceLocator.getServiceHandle(activeDescriptor, injectee);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor) throws MultiException {
        return serviceLocator.getServiceHandle(activeDescriptor);
    }

    @Override
    public <T> T getService(ActiveDescriptor<T> activeDescriptor, ServiceHandle<?> serviceHandle) throws MultiException {
        return serviceLocator.getService(activeDescriptor, serviceHandle);
    }

    @Override
    public <T> T getService(ActiveDescriptor<T> activeDescriptor, ServiceHandle<?> serviceHandle, Injectee injectee) throws MultiException {
        return serviceLocator.getService(activeDescriptor, serviceHandle, injectee);
    }

    @Override
    public String getDefaultClassAnalyzerName() {
        return serviceLocator.getDefaultClassAnalyzerName();
    }

    @Override
    public void setDefaultClassAnalyzerName(String s) {
        serviceLocator.setDefaultClassAnalyzerName(s);
    }

    @Override
    public String getName() {
        return serviceLocator.getName();
    }

    @Override
    public long getLocatorId() {
        return serviceLocator.getLocatorId();
    }

    @Override
    public ServiceLocator getParent() {
        return serviceLocator.getParent();
    }

    @Override
    public void shutdown() {
        serviceLocator.shutdown();
    }

    @Override
    public ServiceLocatorState getState() {
        return serviceLocator.getState();
    }

    @Override
    public boolean getNeutralContextClassLoader() {
        return serviceLocator.getNeutralContextClassLoader();
    }

    @Override
    public void setNeutralContextClassLoader(boolean b) {
        serviceLocator.setNeutralContextClassLoader(b);
    }

    @Override
    public <T> T create(Class<T> aClass) {
        return serviceLocator.create(aClass);
    }

    @Override
    public <T> T create(Class<T> aClass, String s) {
        return serviceLocator.create(aClass, s);
    }

    @Override
    public void inject(Object o) {
        serviceLocator.inject(o);
    }

    @Override
    public void inject(Object o, String s) {
        serviceLocator.inject(o, s);
    }

    @Override
    public void postConstruct(Object o) {
        serviceLocator.postConstruct(o);
    }

    @Override
    public void postConstruct(Object o, String s) {
        serviceLocator.postConstruct(o, s);
    }

    @Override
    public void preDestroy(Object o) {
        serviceLocator.preDestroy(o);
    }

    @Override
    public void preDestroy(Object o, String s) {
        serviceLocator.preDestroy(o, s);
    }

    @Override
    public <U> U createAndInitialize(Class<U> aClass) {
        return serviceLocator.createAndInitialize(aClass);
    }

    @Override
    public <U> U createAndInitialize(Class<U> aClass, String s) {
        return serviceLocator.createAndInitialize(aClass, s);
    }
}
