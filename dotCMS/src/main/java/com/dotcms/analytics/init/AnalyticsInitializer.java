package com.dotcms.analytics.init;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

/**
 * Does the initialization of the analytics
 * Subscribes to the system table update events and automatically retrieves Analytics Key
 * when valid environment variables are present
 * @author jsanca
 */
public class AnalyticsInitializer implements DotInitializer {

    @Override
    public void init() {

        Logger.debug(this, ()-> "Initializing AnalyticsInitializer");
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class,
                WebAPILocator.getAnalyticsWebAPI());
        
        // Automatically retrieve Analytics Key if valid environment variables are configured
        initializeAnalyticsKeyFromEnvironment();
    }
    
    /**
     * Attempts to automatically retrieve and store the Analytics Key during startup
     * if valid CLIENT_ID and CLIENT_SECRET environment variables are present
     */
    private void initializeAnalyticsKeyFromEnvironment() {
        Try.run(() -> {
            // First, try to ensure analytics app configuration exists from environment variables
            ensureAnalyticsConfigurationFromEnvironment();
            
            final AnalyticsApp analyticsApp = AnalyticsHelper.get().appFromHost(APILocator.systemHost());
            
            // Check if analytics app is properly configured with environment variables
            if (analyticsApp.isConfigValid()) {
                final String currentAnalyticsKey = analyticsApp.getAnalyticsProperties().analyticsKey();
                
                // Only retrieve Analytics Key if not already present
                if (currentAnalyticsKey == null || currentAnalyticsKey.trim().isEmpty()) {
                    Logger.info(this, "Valid analytics credentials found in environment variables. Retrieving Analytics Key automatically...");
                    APILocator.getAnalyticsAPI().resetAnalyticsKey(analyticsApp, false);
                    Logger.info(this, "Analytics Key successfully retrieved and stored during startup");
                } else {
                    Logger.debug(this, "Analytics Key already present, skipping automatic retrieval");
                }
            } else {
                Logger.debug(this, "Analytics app configuration incomplete, skipping automatic Analytics Key retrieval");
            }
        }).onFailure(throwable -> 
            Logger.warn(this, "Could not automatically retrieve Analytics Key during startup: " + throwable.getMessage())
        );
    }
    
    /**
     * Creates analytics app configuration from environment variables if it doesn't exist
     * and valid environment variables are present
     */
    private void ensureAnalyticsConfigurationFromEnvironment() {
        Try.run(() -> {
            final com.dotcms.security.apps.AppsAPI appsAPI = APILocator.getAppsAPI();
            final com.dotmarketing.beans.Host host = APILocator.systemHost();
            final com.liferay.portal.model.User user = APILocator.systemUser();
            
            // Check if configuration already exists
            final com.dotcms.security.apps.AppSecrets existingSecrets = appsAPI
                .getSecrets(AnalyticsApp.ANALYTICS_APP_KEY, true, host, user)
                .orElse(com.dotcms.security.apps.AppSecrets.empty());
                
            if (existingSecrets.getSecrets().isEmpty()) {
                // No configuration exists, check if we have environment variables to create one
                final String clientId = com.dotmarketing.util.Config.getStringProperty(
                    AnalyticsApp.ANALYTICS_APP_CLIENT_ID_KEY, null);
                final String clientSecret = com.dotmarketing.util.Config.getStringProperty(
                    AnalyticsApp.ANALYTICS_APP_CLIENT_SECRET_KEY, null);
                final String configUrl = com.dotmarketing.util.Config.getStringProperty(
                    AnalyticsApp.ANALYTICS_APP_CONFIG_URL_KEY, null);
                final String writeUrl = com.dotmarketing.util.Config.getStringProperty(
                    AnalyticsApp.ANALYTICS_APP_WRITE_URL_KEY, null);
                final String readUrl = com.dotmarketing.util.Config.getStringProperty(
                    AnalyticsApp.ANALYTICS_APP_READ_URL_KEY, null);
                    
                if (clientId != null && clientSecret != null && configUrl != null) {
                    Logger.info(this, "Creating analytics app configuration from environment variables...");
                    
                    // Create minimal app configuration from environment variables
                    createAnalyticsAppConfig(appsAPI, host, user, clientId, clientSecret, configUrl, writeUrl, readUrl);
                    
                    Logger.info(this, "Analytics app configuration created successfully from environment variables");
                } else {
                    Logger.debug(this, "Insufficient environment variables to auto-create analytics configuration");
                }
            } else {
                Logger.debug(this, "Analytics app configuration already exists");
            }
        }).onFailure(throwable -> 
            Logger.warn(this, "Could not ensure analytics configuration from environment: " + throwable.getMessage())
        );
    }
    
    /**
     * Creates the analytics app configuration using AppsAPI
     */
    private void createAnalyticsAppConfig(final com.dotcms.security.apps.AppsAPI appsAPI,
                                        final com.dotmarketing.beans.Host host,
                                        final com.liferay.portal.model.User user,
                                        final String clientId,
                                        final String clientSecret,
                                        final String configUrl,
                                        final String writeUrl,
                                        final String readUrl) throws Exception {
        
        // Get the app descriptor to access parameter descriptors for proper secret creation
        final java.util.Optional<com.dotcms.security.apps.AppDescriptor> appDescriptorOpt = 
            appsAPI.getAppDescriptor(AnalyticsApp.ANALYTICS_APP_KEY, user);
        
        if (!appDescriptorOpt.isPresent()) {
            throw new Exception("Analytics app descriptor not found");
        }
        
        final com.dotcms.security.apps.AppDescriptor appDescriptor = appDescriptorOpt.get();
        final java.util.Map<String, com.dotcms.security.apps.ParamDescriptor> paramDescriptors = 
            appDescriptor.getParams();
        
        // Create and save CLIENT_ID secret
        createAndSaveSecret(appsAPI, host, user, "clientId", clientId.toCharArray(), paramDescriptors);
        
        // Create and save CLIENT_SECRET secret  
        createAndSaveSecret(appsAPI, host, user, "clientSecret", clientSecret.toCharArray(), paramDescriptors);
        
        // Create and save CONFIG_URL secret
        createAndSaveSecret(appsAPI, host, user, "analyticsConfigUrl", configUrl.toCharArray(), paramDescriptors);
        
        // Create and save WRITE_URL secret if provided
        if (writeUrl != null) {
            createAndSaveSecret(appsAPI, host, user, "analyticsWriteUrl", writeUrl.toCharArray(), paramDescriptors);
        }
        
        // Create and save READ_URL secret if provided  
        if (readUrl != null) {
            createAndSaveSecret(appsAPI, host, user, "analyticsReadUrl", readUrl.toCharArray(), paramDescriptors);
        }
    }
    
    /**
     * Creates and saves a single secret using the proper AppsUtil.paramSecret method
     */
    private void createAndSaveSecret(final com.dotcms.security.apps.AppsAPI appsAPI,
                                   final com.dotmarketing.beans.Host host,
                                   final com.liferay.portal.model.User user,
                                   final String paramName,
                                   final char[] value,
                                   final java.util.Map<String, com.dotcms.security.apps.ParamDescriptor> paramDescriptors) throws Exception {
        
        final com.dotcms.security.apps.ParamDescriptor paramDescriptor = paramDescriptors.get(paramName);
        if (paramDescriptor == null) {
            Logger.warn(this, String.format("Parameter descriptor not found for %s, skipping", paramName));
            return;
        }
        
        // Use AppsUtil.paramSecret to create properly typed secret
        final java.util.Optional<com.dotcms.security.apps.Secret> secretOpt = 
            com.dotcms.security.apps.AppsUtil.paramSecret(
                AnalyticsApp.ANALYTICS_APP_KEY,
                paramName,
                value,
                paramDescriptor
            );
        
        if (secretOpt.isPresent()) {
            appsAPI.saveSecret(
                AnalyticsApp.ANALYTICS_APP_KEY,
                new io.vavr.Tuple2<>(paramName, secretOpt.get()),
                host,
                user
            );
            Logger.debug(this, String.format("Successfully created and saved secret for %s", paramName));
        } else {
            Logger.warn(this, String.format("Failed to create secret for %s", paramName));
        }
    }
}
