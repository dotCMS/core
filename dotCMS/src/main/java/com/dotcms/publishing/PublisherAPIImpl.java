package com.dotcms.publishing;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.type.pushpublish.PushPublishEndEvent;
import com.dotcms.system.event.local.type.pushpublish.PushPublishStartEvent;
import com.dotcms.system.event.local.type.staticpublish.StaticPublishEndEvent;
import com.dotcms.system.event.local.type.staticpublish.StaticPublishStartEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

public class PublisherAPIImpl implements PublisherAPI {

    private final PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();
    private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
    private final Map<String,FilterDescriptor> loadedFilters = new ConcurrentHashMap<>();


    @Override
    final public PublishStatus publish ( PublisherConfig config ) throws DotPublishingException {

        return publish( config, new PublishStatus() );
    }

    @CloseDBIfOpened
    @Override
    final public PublishStatus publish ( PublisherConfig config, PublishStatus status ) throws DotPublishingException {

        PushPublishLogger.log( this.getClass(), "Started Publishing Task", config.getId() );

        //Triggering event listener when the publishing process starts
        localSystemEventsAPI.asyncNotify(new PushPublishStartEvent(config.getAssets()));
        localSystemEventsAPI.asyncNotify(new StaticPublishStartEvent(config.getAssets()));

        try {

            List<IBundler> confBundlers = new ArrayList<>();

            // init publishers
            for ( Class<Publisher> c : config.getPublishers() ) {
                // Process config
                Publisher p = c.newInstance();
                config = p.init( config );

                if ( config.isIncremental() && config.getEndDate() == null && config.getStartDate() == null ) {
                    // if its incremental and start/end dates aren't se we take it from latest bundle
                    if ( BundlerUtil.bundleExists( config ) ) {
                        PublisherConfig pc = BundlerUtil.readBundleXml( config );
                        if ( pc.getEndDate() != null ) {
                            config.setStartDate( pc.getEndDate() );
                            config.setEndDate( new Date() );
                        } else {
                            config.setStartDate( null );
                            config.setEndDate( new Date() );
                        }
                    } else {
                        config.setStartDate( null );
                        config.setEndDate( new Date() );
                    }
                }

                // Find out if we already have the bundle. It is important to note that we
                // get this info before calling BundlerUtil.writeBundleXML() (code below)
                // cause that logic will create the bundle folder and BundlerUtil.bundleExists
                // will return true after that always.
                final boolean bundleExists = BundlerUtil.bundleExists(config);

                // Run bundlers
                File bundleRoot = BundlerUtil.getBundleRoot( config );

                if (config.isStatic()) {
                    //If static we just want to save the things that we need,
                    // at this point only the id, static and operation.
                	PublisherConfig pcClone = new PublisherConfig();
                	pcClone.setId(config.getId());
                	pcClone.setStatic(true);
                	pcClone.setOperation(config.getOperation());
                    Logger.info(this, "Writing bundle.xml file");
                	BundlerUtil.writeBundleXML( pcClone );
                } else {
                    Logger.info(this, "Writing bundle.xml file");
                    BundlerUtil.writeBundleXML( config );
                }

                // If the bundle exists and we are retrying to push the bundle
                // there is no need to run all the bundlers again.
                if (!bundleExists || !publishAuditAPI.isPublishRetry(config.getId())) {
                    PublishAuditStatus currentStatus = publishAuditAPI
                            .getPublishAuditStatus(config.getId());
                    PublishAuditHistory currentStatusHistory = null;
                    if(currentStatus != null) {
                        currentStatusHistory = currentStatus.getStatusPojo();
                        if(currentStatusHistory != null) {
                            currentStatusHistory.setBundleStart(new Date());
                        }
                    }

                    for ( Class<IBundler> clazz : p.getBundlers() ) {
                        IBundler bundler = clazz.newInstance();
                        confBundlers.add( bundler );
                        bundler.setConfig( config );
                        bundler.setPublisher(p);
                        BundlerStatus bs = new BundlerStatus( bundler.getClass().getName() );
                        status.addToBs( bs );
                        //Generate the bundler
                        Logger.info(this, "Start of Bundler: " + clazz.getSimpleName());
                        bundler.generate( bundleRoot, bs );
                        Logger.info(this, "End of Bundler: " + clazz.getSimpleName());
                    }

                    if(currentStatusHistory != null) {
                        currentStatusHistory.setBundleEnd(new Date());
                        publishAuditAPI
                                .updatePublishAuditStatus(config.getId(),
                                        PublishAuditStatus.Status.BUNDLING,
                                        currentStatusHistory);
                    }
                } else {
                    Logger.info(this, "Retrying bundle: " + config.getId()
                            + ", we don't need to run bundlers again");
                }

                p.process( status );
            }

            config.setBundlers( confBundlers );

            //Triggering event listener when the publishing process ends
            localSystemEventsAPI.asyncNotify(new PushPublishEndEvent(config.getAssets()));
            localSystemEventsAPI.asyncNotify(new StaticPublishEndEvent(config.getAssets()));

            PushPublishLogger.log( this.getClass(), "Completed Publishing Task", config.getId() );
        } catch ( Exception e ) {
            Logger.error( PublisherAPIImpl.class, e.getMessage(), e );
            throw new DotPublishingException( e.getMessage(), e );
        }

        return status;
    }

    @Override
    public void addFilterDescriptor(final FilterDescriptor filterDescriptor) {
        this.loadedFilters.put(filterDescriptor.getKey(),filterDescriptor);
    }

    @Override
    public List<FilterDescriptor> getFiltersDescriptorsByRole(final User user) throws DotDataException {
        if(user.isAdmin()){
            return new ArrayList<>(this.loadedFilters.values());
        }
        final List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), true);
        Logger.info(this,"User Roles: " + roles.toString());
        final List<FilterDescriptor> filters = new ArrayList<>();
        for(final Map.Entry<String,FilterDescriptor> filterDescriptorMap : this.loadedFilters.entrySet()){
            final String filterRoles = filterDescriptorMap.getValue().getRoles();
            Logger.info(PublisherAPI.class,"File: " +filterDescriptorMap.getKey() + " Roles: " + filterRoles );
            for(final Role role : roles){
                if(UtilMethods.isSet(role.getRoleKey()) && filterRoles.contains(role.getRoleKey())){
                    filters.add(filterDescriptorMap.getValue());
                }
            }
        }

        return filters;
    }

    @VisibleForTesting
    public Map<String, FilterDescriptor> getFilterDescriptorMap() {
        return this.loadedFilters;
    }
    
    @CloseDBIfOpened
    @Override
    public FilterDescriptor getFilterDescriptorByKey(final String filterKey) {
        final FilterDescriptor defaultFilter = getDefaultFilter();
        return !UtilMethods.isSet(filterKey) ? defaultFilter : this.loadedFilters.getOrDefault(filterKey,defaultFilter);
    }
    
    @CloseDBIfOpened
    @Override
    public PublisherFilter createPublisherFilter(final String bundleId)
            throws DotDataException, DotSecurityException {

        final String filterKey = APILocator.getBundleAPI().getBundleById(bundleId).getFilterKey();
        final FilterDescriptor filterDescriptor = this.getFilterDescriptorByKey(filterKey);

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl((Boolean)filterDescriptor.getFilters().getOrDefault(FilterDescriptor.DEPENDENCIES_KEY,true),
                (Boolean)filterDescriptor.getFilters().getOrDefault(FilterDescriptor.RELATIONSHIPS_KEY,true));

        if(filterDescriptor.getFilters().containsKey(FilterDescriptor.EXCLUDE_CLASSES_KEY)){
            List.class.cast(filterDescriptor.getFilters().get(FilterDescriptor.EXCLUDE_CLASSES_KEY)).stream().forEach(type -> publisherFilter.addTypeToExcludeClassesSet(type.toString()));

        }

        if(filterDescriptor.getFilters().containsKey(FilterDescriptor.EXCLUDE_DEPENDENCY_CLASSES_KEY)){
            List.class.cast(filterDescriptor.getFilters().get(FilterDescriptor.EXCLUDE_DEPENDENCY_CLASSES_KEY)).stream().forEach(type -> publisherFilter.addTypeToExcludeDependencyClassesSet(type.toString()));
        }

        if(filterDescriptor.getFilters().containsKey(FilterDescriptor.EXCLUDE_QUERY_KEY)){
            final String query = filterDescriptor.getFilters().get(FilterDescriptor.EXCLUDE_QUERY_KEY).toString();
            APILocator.getContentletAPI().search(query, 0, 0, "moddate", APILocator.systemUser(), false)
                .stream().forEach(contentlet -> publisherFilter.addContentletIdToExcludeQueryAssetIdSet(contentlet.getIdentifier()));
        }

        if(filterDescriptor.getFilters().containsKey(FilterDescriptor.EXCLUDE_DEPENDENCY_QUERY_KEY)){
            final String query = filterDescriptor.getFilters().get(FilterDescriptor.EXCLUDE_DEPENDENCY_QUERY_KEY).toString();
            APILocator.getContentletAPI().search(query, 0, 0, "moddate", APILocator.systemUser(), false)
                .stream().forEach(contentlet -> publisherFilter.addContentletIdToExcludeDependencyQueryAssetIdSet(contentlet.getIdentifier()));
        }

        return publisherFilter;
    }

    public FilterDescriptor getDefaultFilter(){
        return this.loadedFilters.values().stream().filter(FilterDescriptor::isDefaultFilter).findFirst().get();
    }
}