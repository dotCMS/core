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
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@ActivateRequestContext
@CommandLine.Command(
        name = InitCommand.NAME,
        header = "@|bold,blue Initialize the cli and Configures dotCMS instances.|@",
        description = {
                " Creates the initial configuration file for the dotCMS CLI.",
                " The file is created in the user's home directory.",
                " The file is named [dot-service.yml]",
                " This file is used to store the configuration of the dotCMS instances.",
                " An instance can be activated by using the command @|yellow instance -act <instanceName>|@",
                " Typically an instance holds the API URL, the user and the profile-name.",
                " Running this command is mandatory before using the CLI.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class InitCommand implements Callable<Integer>, DotCommand {

    public static final String NAME = "init";
    public static final String INSTANCE_NAME = "name";
    public static final String INSTANCE_URL = "url";

    public static final Comparator<ServiceBean> comparator = Comparator.comparing(
                    ServiceBean::active).reversed()
            .thenComparing(ServiceBean::name);

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

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
            if(delete){
                output.info("No configuration file found. Nothing to delete. Re-run the command without the -d option.");
                return ExitCode.OK;
            }
            freshInit();
        } else {
           if(delete) {
               performDelete();
               return ExitCode.OK;
           }
           //We have a configuration file, let's update it
            performUpdate(services);
        }

        return ExitCode.OK;
    }

    /**
     * This method is used to perform the update
     * @param services the services
     * @throws IOException if an error occurs
     */
    private void performUpdate(List<ServiceBean> services) throws IOException {
        List<ServiceBean> beansForUpdate = new ArrayList<>(services);
        output.info("The CLI has been already initialized.");
        while (true) {
            printServices(beansForUpdate);
            final int index = Prompt.readInput(-1,
                    "Select the number of the profile you want to @|bold edit|@ or press enter to exit. ");
            if (-1 == index) {
                break;
            }
            if (index < 0 || index >= beansForUpdate.size()) {
                output.error("No profile was selected.");
            } else {
                beansForUpdate = captureValuesThenUpdate(beansForUpdate, index);
            }
        }
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
        while (true) {
            final String newName = Prompt.readInput(name, "Enter the new name for the profile [%s]. ", name);
            final String newUrl = Prompt.readInput(url, "Enter the new URL for the profile [%s]. ", url);
            final boolean valuesOK = Prompt.yesOrNo(true, "Are these values OK? (Enter to confirm or N to cancel) ");
            if (valuesOK) {
                try {
                    final ServiceBean newService = createService(selected, newName, newUrl, validationMap);
                    beansForUpdate.set(index, newService);
                    persist(beansForUpdate);
                    output.info("The profile [" + newName + "] has been updated.");
                    beansForUpdate = new ArrayList<>(serviceManager.services());
                    final boolean yes = Prompt.yesOrNo(false, "Do you want to update the @|bold current active|@ instance? ");
                    if(yes) {
                        makeActive(beansForUpdate);
                    }
                    beansForUpdate = new ArrayList<>(serviceManager.services());
                    break;
                } catch (IllegalArgumentException e) {
                    output.error("There are errors in the captured values : " + e.getMessage());
                }
            }
        }
        return beansForUpdate;
    }

    private void freshInit() throws IOException {
        //We don't have a configuration file, let's create one
        final Map<String, String> suggestedValues = new HashMap<>(values);
        final Map<String,ServiceBean> capturedValues = new HashMap<>();

        int capturedCount = 0;
        while (true) {
            final Map<String, String> captured = captureValues(suggestedValues);
            String name = captured.get(INSTANCE_NAME);
            String baseURL = captured.get(INSTANCE_URL);
            ServiceBean service = null;
            try {
                service = createService(name, baseURL, capturedValues);
            } catch (IllegalArgumentException e) {
                output.error("There are errors in the captured values : " +e.getMessage());
            }
            if(null != service){
                capturedValues.put(name, service);
                capturedCount++;
                suggestedValues.put(INSTANCE_NAME, String.format("%s#%d" , cleanSuggestions(name), ++capturedCount));
                suggestedValues.put(INSTANCE_URL, baseURL);

                final boolean yes = Prompt.yesOrNo(true, "Do you want to continue adding another dotCMS instance? ");
                if (!yes) {
                    break;
                }   
            }
        }
        makeActive(capturedValues.values().stream().sorted(
                comparator).collect(Collectors.toList()));
    }

    /**
     * This method is used to capture the values for the dotCMS instance
     * @param suggestedValues the suggested values
     * @return the captured values
     */
    Map<String,String> captureValues(final Map<String, String> suggestedValues){
        String name;
        String baseURL;
        while (true) {
            final String suggestedName = suggestedValues.get(INSTANCE_NAME);
            final String suggestedUrl = suggestedValues.get(INSTANCE_URL);
            name = Prompt.readInput(suggestedName,
                    "Enter the key/name that will serve to identify the dotCMS instance (must be unique) [%s].  ",
                    suggestedName);
            output.info(String.format("The name is [@|bold, %s|@]", name));

            baseURL = Prompt.readInput(suggestedUrl,
                    "Enter the dotCMS base URL (must be a valid URL starting protocol http or https) [%s] ", suggestedUrl);
            output.info(String.format("The URL is [@|bold, %s|@]", baseURL));

            final boolean valuesOK = Prompt.yesOrNo(true, "Are these values OK? ");
            if (valuesOK) {
                break;
            }
        }
        return Map.of(INSTANCE_NAME, name, INSTANCE_URL, baseURL);
    }

    /**
     * This method is used to select the active profile
     * @param capturedValues the captured values
     * @throws IOException if an error occurs
     */
    private void makeActive(final List<ServiceBean> capturedValues) throws IOException {
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
            boolean running = true;
            while (running) {
                final List<ServiceBean> serviceBeans = new ArrayList<>(capturedValues);
                printServices(serviceBeans);
                output.info("One of these profiles needs to be made the current active one. Please select the number of the profile you want to activate.");
                final int index = Prompt.readInput(-1,
                        "Enter the number of the profile to be made default or press enter to exit. ");
                if (-1 == index) {
                    running = false;
                } else {
                    if (index < 0 || index >= capturedValues.size()) {
                        output.error("No profile was selected.");
                    } else {
                        final ServiceBean selectedService = serviceBeans.get(index);
                        final ServiceBean activeBean = ServiceBean.builder().from(selectedService)
                                .active(true)
                                .build();
                        serviceBeans.set(index, activeBean);
                        persist(serviceBeans);
                        output.info("The current active profile is now [" + activeBean.name() + "]");
                        running = false;
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
        boolean running = true;
        while (running) {
            final List<ServiceBean> serviceBeans = new ArrayList<>(serviceManager.services());
            printServices(serviceBeans);
            final int index = Prompt.readInput(-1, "Enter the number of the profile to @|bold delete|@ press enter to exit. ");
            if (index == -1) {
                running = false;
            } else {
                if (index < 0 || index >= serviceBeans.size()) {
                    output.error("No profile was selected.");
                } else {
                    serviceBeans.remove(index);
                    persist(serviceBeans);
                    if (!Prompt.yesOrNo(false, "Do you want to delete another profile? ")) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * This method Re-writes the configuration file entirely with the new values
     * @param serviceBeans the service beans
     * @throws IOException if an error occurs
     */
    private void persist(final List<ServiceBean> serviceBeans) throws IOException {
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
        try {
            return ServiceBean.builder().name(nameLower).url(new URL(url)).build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    /**
     * This method is used to create a service bean from the saved bean
     * @param savedBean the saved bean to merge with
     * @param name the name
     * @param url the url
     * @param capturedValues the captured values map to check for duplicates etc.
     * @return the service bean
     */
    ServiceBean createService(ServiceBean savedBean, String name, String url, final Map<String,ServiceBean> capturedValues) {
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("The name cannot be empty.");
        }
        String nameLower = name.toLowerCase();
        if(capturedValues.containsKey(nameLower)){
            throw new IllegalArgumentException("The name [" + name + "] is already in use.");
        }
        try {
            //Don't forget that when merging we need to copy credentials and active status
            return ServiceBean.builder().from(savedBean).name(nameLower).url(new URL(url)).build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL", e);
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
}
