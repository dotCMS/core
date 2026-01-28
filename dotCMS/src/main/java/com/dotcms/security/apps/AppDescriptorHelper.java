package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppsAPI.APPS_DIR_NAME;
import static com.dotcms.security.apps.AppsAPI.APPS_DIR_PATH_KEY;
import static com.dotcms.security.apps.AppsAPI.DESCRIPTOR_NAME_MAX_LENGTH;
import static com.dotcms.security.apps.AppsAPI.SERVER_DIR_NAME;
import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppDescriptorHelper {

    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            .enable(Feature.STRICT_DUPLICATE_DETECTION)
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .findAndRegisterModules();

    /**
     * There's a version of the method readValue on the ymlMapper which takes a file and internally creates directly a FileInputStream
     * According to https://dzone.com/articles/fileinputstream-fileoutputstream-considered-harmful
     * that's very harmful
     * @param file
     * @return
     * @throws DotDataException
     */
    AppSchema readAppFile(final Path file) throws DotDataException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return ymlMapper.readValue(inputStream, AppSchema.class);
        }catch (Exception e){
            throw new DotDataException(e.getMessage(), e);
        }
    }

    /**
     * There's a version of the method writeValue on the ymlMapper which takes a file and internally creates directly a FileOutputStream
     * According to https://dzone.com/articles/fileinputstream-fileoutputstream-considered-harmful
     * that's very harmful
     * @param file
     * @return
     * @throws DotDataException
     */
    void writeAppFile(final File file, final AppSchema appSchema) throws DotDataException {
        try (OutputStream outputStream = Files.newOutputStream(Paths.get(file.getPath()))) {
            ymlMapper.writeValue(outputStream, appSchema);
        }catch (Exception e){
            throw new DotDataException(e.getMessage(), e);
        }
    }

    /**
     * AppDescriptors loading entry point
     * None of this use cache.
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
     public List<AppDescriptor> loadAppDescriptors()
            throws IOException, URISyntaxException {

        final ImmutableList.Builder<AppDescriptor> builder = new ImmutableList.Builder<>();
        final Set<Tuple2<Path, Boolean>> filePaths = listAvailableYamlFiles();
        for (final Tuple2<Path, Boolean> filePath : filePaths) {
            try {
                final Path path = filePath._1;
                final boolean systemApp = filePath._2;
                final AppSchema appSchema = readAppFile(path);
                if (validateAppDescriptor(appSchema)) {
                    builder.add(new AppDescriptorImpl(path.getFileName().toString(), systemApp, appSchema));
                }
            } catch (Exception e) {
                Logger.error(AppsAPIImpl.class,
                        String.format("Error reading yml file `%s`.", filePath), e);
            }
        }

        return builder.build();
    }

    /**
     * internal descriptor validator
     * @param appDescriptor
     * @return
     * @throws DotDataValidationException
     */
     boolean validateAppDescriptor(final AppSchema appDescriptor)
            throws DotDataValidationException {

        final List<String> errors = new ArrayList<>();

        if(isNotSet(appDescriptor.getName())){
            errors.add("The required field `name` isn't set on the incoming file.");
        }

        if(isNotSet(appDescriptor.getDescription())){
            errors.add("The required field `description` isn't set on the incoming file.");
        }

        if(isNotSet(appDescriptor.getIconUrl())){
            errors.add("The required field `iconUrl` isn't set on the incoming file.");
        }

        if(!isSet(appDescriptor.getAllowExtraParameters())){
            errors.add("The required boolean field `allowExtraParameters` isn't set on the incoming file.");
        }

        if(!isSet(appDescriptor.getParams())){
            errors.add("The required field `params` isn't set on the incoming file.");
        }

        for (final Map.Entry<String, ParamDescriptor> entry : appDescriptor.getParams().entrySet()) {
            errors.addAll(validateParamDescriptor(entry.getKey(), entry.getValue()));
        }

        if(!errors.isEmpty()){
            throw new DotDataValidationException(String.join(" \n", errors));
        }

        return true;

    }

    /**
     * internal param validator
     * @param name
     * @param descriptor
     * @return
     */
    private List<String> validateParamDescriptor(final String name,
            final ParamDescriptor descriptor) {

        final List<String> errors = new LinkedList<>();

        if (isNotSet(name)) {
            errors.add("Param descriptor is missing required  field `name` .");
        }

        if (DESCRIPTOR_NAME_MAX_LENGTH < name.length()) {
            errors.add(String.format("`%s`: exceeds %d chars length.", name,
                    DESCRIPTOR_NAME_MAX_LENGTH));
        }

        if (null == descriptor.getType()) {
            errors.add(String.format(
                    "Param `%s`: is missing required field `type` (STRING|BOOL|SELECT|BUTTON|GENERATED_STRING|HEADING|INFO) .",
                    name));
            return errors;
        }

        final boolean isStructural = Type.HEADING.equals(descriptor.getType()) || Type.INFO.equals(descriptor.getType());

        if (!isStructural && null == descriptor.getValue()) {
            errors.add(String.format(
                    "`%s`: is missing required field `value` or a value hasn't been set. Value is mandatory. ",
                    name));
        }

        if (!isStructural && isNotSet(descriptor.getHint())) {
            errors.add(String.format("Param `%s`: is missing required field `hint` .", name));
        }

        if (Type.INFO.equals(descriptor.getType()) && isNotSet(descriptor.getHint())) {
            errors.add(String.format("Param `%s`: is missing required field `hint` (content for INFO type).", name));
        }

        if (Type.HEADING.equals(descriptor.getType()) && isNotSet(descriptor.getLabel())) {
            errors.add(String.format("Param `%s`: is missing required field `label` (title for HEADING type).", name));
        }

        if (!isStructural && isNotSet(descriptor.getLabel())) {
            errors.add(String.format("Param `%s`: is missing required field `label` .", name));
        }

        if (!isStructural && !isSet(descriptor.getRequired())) {
            errors.add(
                    String.format("Param `%s`: is missing required field `required` (true|false) .",
                            name));
        }

        if (!isStructural && !isSet(descriptor.getHidden())) {
            errors.add(
                    String.format("Param `%s`: is missing required field `hidden` (true|false) .",
                            name));
        }

        if (!isStructural && isSet(descriptor.getValue()) && StringPool.NULL
                .equalsIgnoreCase(descriptor.getValue().toString()) && descriptor.isRequired()) {
            errors.add(String.format(
                    "Null isn't allowed as the default value on required params see `%s`. ",
                    name)
            );
        }

        if (Type.BOOL.equals(descriptor.getType())) {
            if (isSet(descriptor.getHidden())
                    && descriptor.isHidden()) {
                errors.add(String.format(
                        "Param `%s`: Bool params can not be marked hidden. The combination (Bool + Hidden) isn't allowed.",
                        name));
            }

            if (isSet(descriptor.getValue())
                    && !isBoolString(descriptor.getValue().toString())) {
                errors.add(String.format(
                        "Boolean Param `%s` has a default value `%s` that can not be parsed to bool (true|false).",
                        name, descriptor.getValue()));
            }
        }

        if(Type.STRING.equals(descriptor.getType()) && !(descriptor.getValue() instanceof String)){
            errors.add(String.format(
                    "Value Param `%s` has a default value `%s` that isn't a string .",
                    name, descriptor.getValue()));
        }

        if (Type.SELECT.equals(descriptor.getType())) {

            if (isSet(descriptor.getHidden()) && descriptor.isHidden()) {
                errors.add(String.format(
                        "Param `%s`: List params can not be marked hidden. The combination (List + Hidden) isn't allowed.",
                        name));
            }

            if (!(descriptor.getValue() instanceof List)) {
                errors.add(String.format(
                        " As param `%s`:  is marked as `List` the field value is expected to hold a list of objects. ",
                        name));
            } else {
                final int minSelectedElements = 1;
                int selectedCount = 0;
                final List list = (List) descriptor.getValue();
                for (final Object object : list) {
                    if (!(object instanceof Map)) {
                        errors.add(String.format(
                                "Malformed list. Param: `%s` is marked as `List` therefore field `value` is expected to have a list of objects. ",
                                name));
                    } else {
                        final Map map = (Map) object;
                        if (!map.containsKey("label") || !map.containsKey("value") ) {
                            errors.add(String.format("Malformed list. Param: `%s`. Every entry of the `List` has to have the following fields (`label`,`value`). ", name));
                        }
                        if(map.containsKey("selected")){
                            selectedCount++;
                        }
                    }
                }
                if(selectedCount > minSelectedElements ){
                    errors.add(String.format("Malformed list. Param: `%s`. There must be only 1 item marked as selected ", name));
                }
            }
        }

        if (Type.BUTTON.equals(descriptor.getType())) {
            if (isSet(descriptor.getHidden())
                    && descriptor.isHidden()) {
                errors.add(String.format(
                        "Param `%s`: Button params can not be marked hidden. The combination (Button + Hidden) isn't allowed.",
                        name));
            }

            if(!(descriptor.getValue() instanceof String)){
                errors.add(String.format(
                        "Value Param `%s` has a default value `%s` that isn't a string .",
                        name, descriptor.getValue()));
            }

        }

        return errors;
    }

    /**
     * Verifies if a string can be parsed to boolean safely.
     * @param value
     * @return
     */
    private boolean isBoolString(final String value){
        return Boolean.TRUE.toString().equalsIgnoreCase(value) || Boolean.FALSE.toString().equalsIgnoreCase(value);
    }


    /**
     * This is the Apps-System-Folder which is meant to hold system apps.
     * Those that can not be override and are always available.
     * @return
     */
    static Path getSystemAppsDescriptorDirectory() throws URISyntaxException, IOException {
        final URL res = Thread.currentThread().getContextClassLoader().getResource("apps");
        if(res == null) {
            throw new IOException("Unable to find Apps System folder. It should be at /WEB-INF/classes/apps ");
        } else {
            return Paths.get(res.toURI()).toAbsolutePath();
        }
    }

    /**
     * returns DotCMS server folder
     * @return
     */
    public static Path getServerDirectory() {
        return Paths.get(APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator + SERVER_DIR_NAME + File.separator).normalize();
    }

    /**
     * This is the directory intended for customers use
     * @return
     */
    static Path getAppsDefaultDirectory() {
        return Paths.get(getServerDirectory() + File.separator + APPS_DIR_NAME + File.separator).normalize();
    }

    /**
     * This is the directory intended for customers use.
     * with the option to read an override property from the config
     * @return
     */
    static Path getUserAppsDescriptorDirectory() {
        final Supplier<String> supplier = ()-> getAppsDefaultDirectory().toString();
        final String dirPath = Config
                .getStringProperty(APPS_DIR_PATH_KEY, supplier.get());
        return Paths.get(dirPath).normalize();
    }

    private static DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {

        private static final String ignorePrefix = "_ignore_";
        private static final String yml = "yml";
        private static final String yaml = "yaml";

        @Override
        public boolean accept(final Path path) {
            if (Files.isDirectory(path)) {
                return false;
            }
            final String fileName = path.getFileName().toString();
            return !fileName.startsWith(ignorePrefix) && (fileName.endsWith(yaml) || fileName.endsWith(yml)) ;
        }
    };

    private Set<Path> listFiles(final Path dir) throws IOException {
        final Set<Path> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            stream.forEach(fileList::add);
        }
        return fileList;
    }

    /**
     *  This will get you a list with all the available app-yml files registered in the system.
     *
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    private Set<Tuple2<Path, Boolean>> listAvailableYamlFiles() throws IOException, URISyntaxException {
        final Path systemAppsDescriptorDirectory = getSystemAppsDescriptorDirectory();
        final Set<Path> systemFiles = listFiles(systemAppsDescriptorDirectory);

        final Path appsDescriptorDirectory = getUserAppsDescriptorDirectory();
        final File basePath = appsDescriptorDirectory.toFile();
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        Logger.debug(AppsAPIImpl.class,
                () -> " ymlFiles are set under:  " + basePath.toString());
        final Set<Path> userFiles = listFiles(appsDescriptorDirectory);

        final Set<Path> systemFileNames = systemFiles.stream().map(Path::getFileName)
                .collect(Collectors.toSet());
        final Set<Path> filteredUserFiles = userFiles.stream()
                .filter(path -> systemFileNames.stream().noneMatch(
                        systemPath -> systemPath.toString()
                                .equalsIgnoreCase((path.getFileName().toString().toLowerCase()))))
                .collect(Collectors.toSet());

        return Stream.concat(systemFiles.stream().map(path -> Tuple.of(path, true)),
                filteredUserFiles.stream().map(path -> Tuple.of(path, false)))
                .collect(Collectors.toSet());
    }
}
