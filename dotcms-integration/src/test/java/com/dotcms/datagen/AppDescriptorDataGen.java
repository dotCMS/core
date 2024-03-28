package com.dotcms.datagen;

import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppDescriptorImpl;
import com.dotcms.security.apps.AppSchema;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Type;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import org.apache.commons.io.FilenameUtils;

/**
 * Test utility to simplify creation of application descriptor yml files
 */
public class AppDescriptorDataGen extends AbstractDataGen<AppDescriptor> {

    private static final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            .findAndRegisterModules();

    private String key = String.format("app_%d", System.currentTimeMillis());
    private String name = key;
    private String fileName = String.format("%s.yml", key);
    private String description = "";
    private Boolean allowExtraParameters;
    private String iconUrl = "none.ico";

    private SortedMap<String, ParamDescriptor> paramMap;

    public SortedMap<String, ParamDescriptor> paramMap(){
       if(null == paramMap){
          paramMap = builder.build();
       }
       return paramMap;
    }

    private final ImmutableSortedMap.Builder<String, ParamDescriptor> builder =
            new ImmutableSortedMap.Builder<>(Ordering.natural());

    /**
     * Next new non-persisted object
     * @return
     */
    @Override
    public AppDescriptor next() {
        return new AppDescriptorImpl(fileName, false, name, description, iconUrl, allowExtraParameters, paramMap());
    }

    /**
     * persist the object passed and generates a file representation.
     * @param object the object to persist
     * @return
     */
    @Override
    public AppDescriptor persist(final AppDescriptor object) {
        final AppsAPI api = APILocator.getAppsAPI();
        try{
         final File input = persistDescriptorAsFile(object);
         return api.createAppDescriptor(input, TestUserUtils.getAdminUser());
        } catch (IOException | DotDataException | AlreadyExistException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Yml File Descriptor write method.
     * @param object
     * @return
     * @throws IOException
     */
    private File persistDescriptorAsFile(final AppDescriptor object) throws IOException {
        final AppSchema schema = new AppSchema(object.getName(), object.getDescription(),
                object.getIconUrl(), object.isAllowExtraParameters(), object.getParams());
        String basePath = System.getProperty("java.io.tmpdir");
        basePath = Paths.get(basePath).normalize().toString();
        final File file = new File(basePath, fileName);
        ymlMapper.writeValue(file, schema);
        return file;
    }

    /**
     * similar to persist but only creates the file.
     * @return
     * @throws IOException
     */
    public File nextPersistedDescriptor() throws IOException {
        return persistDescriptorAsFile(next());
    }

    /**
     * Destroys the file
     * @throws IOException
     */
    public void destroy() throws IOException {
        String basePath = System.getProperty("java.io.tmpdir");
        basePath = Paths.get(basePath).normalize().toString();
        final File file = new File(basePath, fileName);
        Files.delete(Paths.get(file.getPath()));
    }

    /**
     * keys are generated internally by this class they're often needed to validate
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * name can be generated internally by this class they're often needed to validate
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * file name getter
     * @return
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * app name builder method
     * @param name
     * @return
     */
    public AppDescriptorDataGen withName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * app descriptor builder method
     * @param description
     * @return
     */
    public AppDescriptorDataGen withDescription(final String description) {
        this.description = description;
        return this;
    }

    /**
     * file name override  builder method.
     * @param fileName
     * @return
     */
    public AppDescriptorDataGen withFileName(final String fileName) {
        this.fileName = fileName;
        this.key = FilenameUtils.removeExtension(fileName);
        return this;
    }

    /**
     * extra params override builder method
     * @param extraParameters
     * @return
     */
    public AppDescriptorDataGen withExtraParameters(final Boolean extraParameters) {
        this.allowExtraParameters = extraParameters;
        return this;
    }

    /**
     * icon builder method
     * @param iconUrl
     * @return
     */
    public AppDescriptorDataGen withIconUrl(final String iconUrl) {
        this.iconUrl = iconUrl;
        return this;
    }

    /**
     * Param builder method
     * @param name
     * @param paramDescriptor
     * @return
     */
    public AppDescriptorDataGen param(final String name, final ParamDescriptor paramDescriptor){
        builder.put(name, paramDescriptor);
        return this;
    }

    /**
     * Param builder method
     * @param name
     * @param hidden
     * @param type
     * @param label
     * @param hint
     * @param required
     * @return
     */
    public AppDescriptorDataGen param(final String name,
                                      final Boolean hidden,
                                      final Type type,
                                      final String label,
                                      final String hint,
                                      final Boolean required,
                                      final Object value) {
       return param(
               name,
               ParamDescriptor.builder()
                       .withHidden(hidden)
                       .withType(type)
                       .withLabel(label)
                       .withHint(hint)
                       .withRequired(required)
                       .withValue(value)
                       .build());
    }

    /**
     * Param builder method
     * @param name
     * @param hidden
     * @param required
     * @param value
     * @return
     */
    public AppDescriptorDataGen stringParam(final String name,
                                            final Boolean hidden,
                                            final Boolean required,
                                            final String label,
                                            final String hint,
                                            final Object value) {
        return param(name, hidden, Type.STRING, label, hint, required, value);
    }

    /**
     * Param builder method
     * @param name
     * @param hidden
     * @param required
     * @return
     */
    public AppDescriptorDataGen stringParam(final String name, final Boolean hidden, final Boolean required) {
        return stringParam(name, hidden, required, "label", "hint", StringPool.BLANK);
    }

    /**
     * Param builder method
     * @param name
     * @param hidden
     * @param required
     * @return
     */
    public AppDescriptorDataGen boolParam(final String name, final Boolean hidden, final Boolean required) {
        return param(name, hidden, Type.BOOL, "label", "hint", required, Boolean.TRUE.toString());
    }

    /**
     * Param builder method
     * @param name
     * @param required
     * @return
     */
    public AppDescriptorDataGen selectParam(final String name,
                                            final Boolean required,
                                            final List<Map<String,String>> items) {
        return param(name, false, Type.SELECT, "label", "hint", required, items);
    }

    /**
     *
     * @param name
     * @param url
     * @param label
     * @param hint
     * @return
     */
    public AppDescriptorDataGen buttonParam(final String name,
                                            final String url,
                                            final String label,
                                            final String hint) {
        return param(name, false, Type.BUTTON, label, hint, false, url);
    }
}
