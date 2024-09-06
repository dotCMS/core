package com.dotcms.cli.command;

import static org.apache.commons.lang3.BooleanUtils.toStringYesNo;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.model.annotation.SecuredPassword;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@ActivateRequestContext
@CommandLine.Command(
        name = ConfigCommand.NAME,
        header = "@|bold,blue Configure Create Update and Activate dotCMS instances.|@",
        description = {
                " Creates the essential configuration file for the dotCMS CLI.",
                " The file is created in the user's home directory.",
                " The file is named [dot-service.yml]",
                " This file is used to store the configuration of the dotCMS instances.",
                " An instance can also be activated by using the command @|yellow instance -act <instanceName>|@",
                " Typically an instance holds the API URL, the user and the profile-name.",
                " Running this command is mandatory before using the CLI.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class ConfigCommand implements Callable<Integer>, DotCommand {

    public static final String NAME = "config";
    public static final String INSTANCE_NAME = "name";
    public static final String INSTANCE_URL = "url";

    /**
     Let's give the user a chance to get it right.
     No seriously, 100 attempts is a crazy high number,
     but I rather have a limit rather than use a while-true construct.
    */
    public static final int MAX_ATTEMPTS = 100;

    public static final Comparator<ServiceBean> comparator = Comparator.comparing(
                    ServiceBean::active).reversed()
            .thenComparing(ServiceBean::name);
    public static final String NO_PROFILE_WAS_SELECTED = "No profile was selected.";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @Option(names = {"-d","--delete"}) boolean delete = false;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public String getName() {
        return NAME;
    }

    @Inject
    @SecuredPassword
    ServiceManager serviceManager;

    @Inject
    Prompt prompt;

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

    Map<String,String> values = Map.of(
        INSTANCE_NAME, "local",
        INSTANCE_URL, "http://localhost:8080"
    );

    @Override
    public Integer call() throws Exception {
        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());
        //Default action prints the current configuration
        final List<ServiceBean> services = serviceManager.services();
        if(services.isEmpty()){
            //Running the command with the -d option and no configuration file is a no-op
            if(delete){
                output.info("No configuration file found. Nothing to delete. Re-run the command without the -d option.");
                return ExitCode.OK;
            }
            //We're running the command for the first time
            freshInit();
        } else {
            //Running the command with the -d option and a configuration file will delete the file
           if(delete) {
               performDelete();
               return ExitCode.OK;
           }
           //We have a configuration file, let's update it
           final List<ServiceBean> updated = performUpdate(services);
           //Now let's check if we need to add new instances passing the list of updated services
           addNewInstances(updated);
        }

        return ExitCode.OK;
    }

    /**
     * This method is used to add new instances
     * @param updated the updated list of services
     * @throws IOException if an error occurs
     */
     void addNewInstances(List<ServiceBean> updated) throws IOException {
        final boolean yes = prompt.yesOrNo(false, "Do you want to add a new dotCMS instance? (Or press enter to exit)");
        if(yes){
             final Map<String, ServiceBean> capturedValues = updated.stream().collect(
                     Collectors.toMap(ServiceBean::name, Function.identity()));
             final Map<String, ServiceBean> newValues = captureAndValidate(capturedValues);
             if(!newValues.isEmpty()){
                 persistAndMakeActive(newValues.values().stream().sorted(
                         comparator).collect(Collectors.toList()));
             }
        }
    }

    /**
     * This method is used to perform the update
     * @param services the services
     * @throws IOException if an error occurs
     */
    List<ServiceBean> performUpdate(List<ServiceBean> services) throws IOException {
        List<ServiceBean> beansForUpdate = new ArrayList<>(services);
        output.info("The CLI has been already configured.");
        int count = 0;
        while (true) {
            printServices(beansForUpdate);
            final int index = prompt.readInput(-1,
                    "Select the number of the profile you want to @|bold edit|@ or press enter. ");
            if (-1 == index) {
                break;
            }
            if (index < 0 || index >= beansForUpdate.size()) {
                output.error(NO_PROFILE_WAS_SELECTED);
            } else {
                beansForUpdate = captureValuesThenUpdate(beansForUpdate, index);
            }
            if (count++ >= maxCaptureAttempts()) {
                throw new IllegalStateException("Too many attempts to capture the values.");
            }
        }
        return beansForUpdate;
    }

    /**
     * Capture the values and update the existing configuration
     * @param beansForUpdate the beans to update
     * @param index the index to update
     * @return the updated list of beans
     * @throws IOException if an error occurs
     */
    private List<ServiceBean> captureValuesThenUpdate(List<ServiceBean> beansForUpdate, final int index) throws IOException {
        final ServiceBean selected = beansForUpdate.get(index);
        final String name = selected.name();
        final String url = selected.url().toString();
        //Remove it from the list we're going to update, so we don't get a false positive when checking for duplicates
        final Map<String, ServiceBean> validationMap = beansForUpdate.stream()
                .filter(bean -> !bean.name().equals(name))
                .collect(Collectors.toMap(ServiceBean::name, Function.identity()));
        int count = 0;
        while (true) {
            final String newName = prompt.readInput(name, "Enter the new name for the profile [%s]. ", name);
            final String newUrl = prompt.readInput(url, "Enter the new URL for the profile [%s]. ", url);
            final boolean valuesOK = prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ");
            if (valuesOK) {
                try {
                    final ServiceBean newService = createService(selected, newName, newUrl, validationMap);
                    beansForUpdate.set(index, newService);
                    persist(beansForUpdate);
                    output.info("The profile [" + newName + "] has been updated.");
                    beansForUpdate = new ArrayList<>(serviceManager.services());
                    final boolean yes = prompt.yesOrNo(false, "Do you want to update the @|bold current active|@ instance? ");
                    if(yes && !beansForUpdate.isEmpty()) {
                        persistAndMakeActive(beansForUpdate);
                    }
                    beansForUpdate = new ArrayList<>(serviceManager.services());
                    break;
                } catch (IllegalArgumentException e) {
                    output.error("There are errors in the captured values : " + e.getMessage());
                }
            }
            if (count++ >= maxCaptureAttempts()) {
                throw new IllegalStateException("Too many attempts to capture the values.");
            }
        }
        return beansForUpdate;
    }

    /**
     * This method is used to perform a fresh init
     * @throws IOException if an error occurs
     */
     void freshInit() throws IOException {
         //We don't have a configuration file, let's create one
         try {
             final Map<String, ServiceBean> capturedValues = captureFreshValues();
             if(!capturedValues.isEmpty()) {
                 persistAndMakeActive(capturedValues.values().stream().sorted(
                         comparator).collect(Collectors.toList()));
             }
         } catch (IllegalStateException e) {
             output.error("Too many attempts to capture the values. Exiting.");
         }
    }

    /**
     * This method is used to capture the fresh values
     * @return the captured values
     */
    private Map<String, ServiceBean> captureFreshValues() {
         return captureAndValidate(new HashMap<>());
    }

    /**
     * This method serves as the loop to capture the values for the dotCMS instance
     * @param capturedValues any values that were already captured
     * @return the captured values
     */
    private Map<String, ServiceBean> captureAndValidate(final Map<String,ServiceBean> capturedValues) {
        final Map<String, String> suggestedValues = new HashMap<>(values);
        int count = 0;
        int captures = 0; //We're going to use this to append a number to the suggested name
        while (true) {
            final Pair<String, String> captured = captureValues(suggestedValues);
            final String name = captured.getKey();
            final String baseURL = captured.getValue();
            ServiceBean service = null;
            try {
                service = createService(name, baseURL, capturedValues);
                //if we got here, the service is valid the values we captured are good. they're error free
                if(null != service){
                    capturedValues.put(name, service);
                    suggestedValues.put(INSTANCE_NAME, String.format("%s#%d" , cleanSuggestions(name), ++captures));
                    suggestedValues.put(INSTANCE_URL, baseURL);
                    final boolean yes = prompt.yesOrNo(true, "Do you want to continue adding another dotCMS instance? ");
                    if (!yes) {
                        break;
                    }
                }
            } catch (IllegalArgumentException e) {
                output.error("There are errors in the captured values : " +e.getMessage());
            }
            if (count++ >= maxCaptureAttempts()) {
                throw new IllegalStateException("Too many attempts to capture the values.");
            }
        }
        return capturedValues;
    }

    /**
     * This method is used to capture the values for the dotCMS instance
     * @param suggestedValues the suggested values
     * @return the captured values
     */
    Pair<String,String> captureValues(final Map<String, String> suggestedValues){

        String name;
        String baseURL;
        int count = 0;
        while (true) {
            final String suggestedName = suggestedValues.get(INSTANCE_NAME);
            final String suggestedUrl = suggestedValues.get(INSTANCE_URL);
            name = prompt.readInput(suggestedName,
                    "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ",
                    suggestedName);
            output.info(String.format("The name is [@|bold, %s|@]", name));

            baseURL = prompt.readInput(suggestedUrl,
                    "Enter the dotCMS base URL (must be a valid URL starting protocol http or https) [%s] ", suggestedUrl);
            output.info(String.format("The URL is [@|bold, %s|@]", baseURL));

            final boolean valuesOK = prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ");
            if (valuesOK) {
                break;
            }
            //This allows us to break the loop if we've tried too many and this gives us a chance to exit gracefully but also makes this code testable too
            if(count++ >= maxCaptureAttempts()){
                throw new IllegalStateException("Too many attempts to capture the values.");
            }
        }
        return  Pair.of(name, baseURL);
    }

    /**
     * This method is used to select the active profile
     * @param capturedValues the captured values
     * @throws IOException if an error occurs
     */
     void persistAndMakeActive(final List<ServiceBean> capturedValues) throws IOException {
        if (capturedValues.isEmpty()) {
            return;
        }

        if(1 == capturedValues.size()){
            final ServiceBean bean = capturedValues.get(0);
            final ServiceBean activeBean = ServiceBean.builder().from(bean)
                    .active(true)
                    .build();
            persist(List.of(activeBean));
            output.info("The current active profile is now [" + activeBean.name() + "]");
        } else {
            boolean capture = true;
            while (capture) {
                final List<ServiceBean> serviceBeans = new ArrayList<>(capturedValues);
                printServices(serviceBeans);
                output.info("One of these profiles needs to be made the current active one. Please select the number of the profile you want to activate.");
                final int index = prompt.readInput(-1,
                        "Enter the number of the profile to be made default or press enter to exit. ");
                if (-1 == index) {
                    persist(serviceBeans);
                    capture = false;
                } else {
                    if (index < 0 || index >= capturedValues.size()) {
                        output.error(NO_PROFILE_WAS_SELECTED);
                    } else {
                        final ServiceBean selectedService = serviceBeans.get(index);
                        final ServiceBean activeBean = ServiceBean.builder().from(selectedService)
                                .active(true)
                                .build();
                        serviceBeans.set(index, activeBean);
                        persist(serviceBeans);
                        output.info("The current active profile is now [" + activeBean.name() + "]");
                        capture = false;
                    }
                }
            }
        }
    }

    /**
     * This method is used to delete a profile
     * @throws IOException if an error occurs
     */
    private void performDelete() throws IOException {
        boolean capture = true;
        while (capture) {
            final List<ServiceBean> serviceBeans = new ArrayList<>(serviceManager.services());
            if (serviceBeans.isEmpty()) {
                output.info("All configurations were removed.");
                capture = false;
            } else {
                printServices(serviceBeans);
                capture = captureOptionThenDelete(capture, serviceBeans);
            }
        }
    }

    /**
     * This method is used to capture option to delete and perform the delete operation
     * @param capture the running flag
     * @param serviceBeans
     * @return
     * @throws IOException
     */
    private boolean captureOptionThenDelete(boolean capture, List<ServiceBean> serviceBeans) throws IOException {
        final int index = prompt.readInput(-1,
                "Enter the number of the profile to @|bold delete|@ press enter to exit. ");
        if (index == -1) {
            capture = false;
        } else {
            if (index < 0 || index >= serviceBeans.size()) {
                output.error(NO_PROFILE_WAS_SELECTED);
            } else {
                serviceBeans.remove(index);
                persist(serviceBeans);
                if (!prompt.yesOrNo(false, "Do you want to delete another profile? ")) {
                    capture = false;
                }
            }
        }
        return capture;
    }

    /**
     * This method Re-writes the configuration file entirely with the new values
     * @param serviceBeans the service beans
     * @throws IOException if an error occurs
     */
    void persist(final List<ServiceBean> serviceBeans) throws IOException {
        serviceManager.removeAll();
        for ( ServiceBean serviceBean : serviceBeans) {
            serviceManager.persist(serviceBean);
        }
    }

    /**
     * This method is used to print the services
     * @param services the services
     */
    private void printServices(List<ServiceBean> services) {
        int index = 0;
        for (ServiceBean service : services) {
            final String message = shortFormat(service, index++);
            output.info(message);
        }
    }

    /**
     * This method is used to print the short format of the service bean
     * @param serviceBean the service bean
     * @param index the index
     * @return the short format
     */
    String shortFormat(final ServiceBean serviceBean, final int index) {
        final String color = serviceBean.active() ? "green" : "blue";
        return String.format(
                " %d. Profile [@|bold,underline,%s %s|@], Uri [@|bold,underline,%s %s|@], active [@|bold,underline,%s %s|@]. ",
                index,color, serviceBean.name(), color, serviceBean.url(), color,
                toStringYesNo(serviceBean.active()));
    }

    /**
     * This method is used to create a brand-new service bean
     * @param name the name
     * @param url the url
     * @param capturedValues the captured values map to check for duplicates etc.
     * @return the service bean
     */
    ServiceBean createService(String name, String url, final Map<String,ServiceBean> capturedValues) {
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("The name cannot be empty.");
        }
        String nameLower = name.toLowerCase();
        if(capturedValues.containsKey(nameLower)){
            throw new IllegalArgumentException("The name [" + name + "] is already in use.");
        }
         return ServiceBean.builder().name(nameLower).url(createURL(url)).build();

    }

    /**
     * This method is used to create a service bean from the saved bean
     * @param savedBean the saved bean to merge with
     * @param name the name
     * @param url the url
     * @param capturedValues the captured values map to check for duplicates etc.
     * @return the service bean
     */
    ServiceBean createService(final ServiceBean savedBean, final String name, final String url, final Map<String,ServiceBean> capturedValues) {
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("The name cannot be empty.");
        }
        String nameLower = name.toLowerCase();
        if(capturedValues.containsKey(nameLower)){
            throw new IllegalArgumentException("The name [" + name + "] is already in use.");
        }

        //Don't forget that when merging we need to copy credentials and active status
        return ServiceBean.builder().from(savedBean).name(nameLower).url(createURL(url)).build();

    }

    /**
     * This method is used to create a URL
     * @param urlString the url string
     * @return the url
     */
    URL createURL(final String urlString) {
        try {
            final URL url = new URL(urlString);
            url.toURI(); //We're not going to use the URI, but this will throw an exception if the URL is invalid
            return url;
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: "+urlString, e);
        }
    }

    /**
     * This method is used to remove the index # from the suggested name
     * @param suggestedName the name to be cleaned
     * @return the cleaned name
     */
    String cleanSuggestions(String suggestedName) {
        return suggestedName.replaceAll("#\\d+$", "");
    }

    /**
     * Test friendly method to set the max attempts
     * We can easily override this method in a test to set the max attempts to 1 and avoid infinite loops
     * @return the max attempts
     */
    int maxCaptureAttempts() {
        return MAX_ATTEMPTS;
    }

}
