package com.dotcms.datagen;

import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Type;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.SortedMap;

public class AppDescriptorDataGen extends AbstractDataGen<AppDescriptor> {

    private static final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
            .findAndRegisterModules();

    private String key = String.format("app_%d", System.currentTimeMillis());
    private String name = key;
    private String fileName = String.format("%s.yml", key);
    private  String description = "";
    private boolean allowExtraParameters = false;
    private String iconUrl = "none";

    private SortedMap<String, ParamDescriptor> paramMap;

    private SortedMap<String, ParamDescriptor> paramMap(){
       if(null == paramMap){
          paramMap = builder.build();
       }
       return paramMap;
    }

    private ImmutableSortedMap.Builder<String, ParamDescriptor> builder = new ImmutableSortedMap.Builder<>(Ordering.natural());

    @Override
    public AppDescriptor next() {
        return new AppDescriptor(key, name, description, iconUrl, allowExtraParameters, paramMap());
    }

    @Override
    public AppDescriptor persist(AppDescriptor object) {
        try (InputStream in = persistDescriptor(object)) {
            Logger.debug(AppDescriptorDataGen.class, in.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return object;
    }

    private InputStream persistDescriptor(final AppDescriptor object) throws IOException {
        String basePath = System.getProperty("java.io.tmpdir");
        basePath = Paths.get(basePath).normalize().toString();
        final File file = new File(basePath, fileName);
        ymlMapper.writeValue(file, object);
        return Files.newInputStream(Paths.get(file.getPath()));
    }

    public InputStream nextPersistedDescriptor() throws IOException {
        return persistDescriptor(next());
    }

    public void destroy() throws IOException {
        String basePath = System.getProperty("java.io.tmpdir");
        basePath = Paths.get(basePath).normalize().toString();
        final File file = new File(basePath, fileName);
        Files.delete(Paths.get(file.getPath()));
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen key(final String key) {
        this.key = key;
        return this;
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen description(final String description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen fileName(final String fileName) {
        this.fileName = fileName;
        return this;
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen allowExtraParameters(final boolean allowExtraParameters) {
        this.allowExtraParameters = allowExtraParameters;
        return this;
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen iconUrl(final String iconUrl) {
        this.iconUrl = iconUrl;
        return this;
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen param(final String name, final ParamDescriptor paramDescriptor){
        builder.put(name, paramDescriptor);
        return this;
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen param(final String name, boolean hidden, final Type type,
            final String label, final String hint, final boolean required) {
       return param(name, ParamDescriptor.newParam(StringPool.BLANK, hidden, type, label, hint, required));
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen boolParam(final String name, boolean hidden, final boolean defValue, final String label,
            final String hint, final boolean required) {
        return  param( name,  hidden, Type.BOOL, label, hint, required);
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen boolParam(final String name, boolean hidden, final boolean defValue,  final boolean required) {
        return boolParam(name, false, defValue, StringPool.BLANK, StringPool.BLANK, required);
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen hiddenBoolParam(final String name, final boolean defValue, final boolean required) {
        return boolParam(name,true, defValue,  required);
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen hiddenRequiredBoolParam(final String name, final boolean defValue) {
       return boolParam(name,true, defValue, true);
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen stringParam(final String name, boolean hidden, final String defValue, final boolean required) {
        builder.put(name, ParamDescriptor
                .newParam(defValue, hidden, Type.STRING, StringPool.BLANK, StringPool.BLANK, required));
        return this;
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen hiddenStringParam(final String name, final String defValue, final boolean required) {
        return stringParam(name,true, defValue, required);
    }

    @SuppressWarnings("unused")
    public AppDescriptorDataGen hiddenRequiredStringParam(final String name, final String defValue) {
        return stringParam(name,true, defValue, true);
    }

}
