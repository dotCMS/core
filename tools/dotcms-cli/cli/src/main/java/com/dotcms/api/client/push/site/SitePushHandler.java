package com.dotcms.api.client.push.site;

import static com.dotcms.cli.command.site.SitePush.SITE_PUSH_OPTION_FORCE_EXECUTION;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.PushHandler;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.SiteView;
import java.io.File;
import java.util.Map;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.BooleanUtils;

@Dependent
public class SitePushHandler implements PushHandler<SiteView> {

    @Inject
    protected RestClientFactory clientFactory;

    @Override
    public Class<SiteView> type() {
        return SiteView.class;
    }

    @Override
    public String title() {
        return "Sites";
    }

    @Override
    public String contentSimpleDisplay(SiteView site) {
        return String.format(
                "name: [%s] id: [%s] inode: [%s] live:[%s] default: [%s] archived: [%s]",
                site.hostName(),
                site.identifier(),
                site.inode(),
                BooleanUtils.toStringYesNo(site.isLive()),
                BooleanUtils.toStringYesNo(site.isDefault()),
                BooleanUtils.toStringYesNo(site.isArchived())
        );
    }

    @ActivateRequestContext
    @Override
    public SiteView add(File localFile, SiteView localSite, Map<String, Object> customOptions) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // Creating the site
        var response = siteAPI.create(
                toRequest(localSite, customOptions)
        );

        // Publishing the site
        if (Boolean.TRUE.equals(localSite.isLive())) {
            response = siteAPI.publish(response.entity().identifier());
        }

        return response.entity();
    }

    @ActivateRequestContext
    @Override
    public SiteView edit(File localFile, SiteView localSite, SiteView serverSite,
            Map<String, Object> customOptions) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        ResponseEntityView<SiteView> response;

        // Unarchiving the site if necessary, this is necessary because the site API doesn't allow
        //  updating an archived site
        if (Boolean.TRUE.equals(serverSite.isArchived())) {
            siteAPI.unarchive(localSite.identifier());
        }

        response = siteAPI.update(
                localSite.identifier(),
                toRequest(localSite, customOptions)
        );

        if (Boolean.TRUE.equals(localSite.isLive()) &&
                Boolean.FALSE.equals(serverSite.isLive())) {
            // Publishing the site
            response = siteAPI.publish(localSite.identifier());
        } else if (Boolean.TRUE.equals(localSite.isArchived()) &&
                Boolean.FALSE.equals(serverSite.isArchived())) {
            // Archiving the site
            response = siteAPI.archive(localSite.identifier());
        } else if (Boolean.FALSE.equals(localSite.isLive()) &&
                Boolean.TRUE.equals(serverSite.isLive())) {
            // Unpublishing the site
            response = siteAPI.unpublish(localSite.identifier());
        }

        return response.entity();
    }

    @ActivateRequestContext
    @Override
    public void remove(SiteView serverSite, Map<String, Object> customOptions) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        siteAPI.archive(
                serverSite.identifier()
        );

        siteAPI.delete(
                serverSite.identifier()
        );
    }

    CreateUpdateSiteRequest toRequest(final SiteView siteView,
            final Map<String, Object> customOptions) {

        var forceExecution = false;
        if (customOptions != null && customOptions.containsKey(SITE_PUSH_OPTION_FORCE_EXECUTION)) {
            forceExecution = (boolean) customOptions.get(SITE_PUSH_OPTION_FORCE_EXECUTION);
        }

        return CreateUpdateSiteRequest.builder()
                .siteName(siteView.siteName())
                .keywords(siteView.keywords())
                .googleMap(siteView.googleMap())
                .addThis(siteView.addThis())
                .aliases(siteView.aliases())
                .identifier(siteView.identifier())
                .inode(siteView.inode())
                .proxyUrlForEditMode(siteView.proxyUrlForEditMode())
                .googleAnalytics(siteView.googleAnalytics())
                .description(siteView.description())
                .tagStorage(siteView.tagStorage())
                .siteThumbnail(siteView.siteThumbnail())
                .embeddedDashboard(siteView.embeddedDashboard())
                .forceExecution(forceExecution)
                .isDefault(Boolean.TRUE.equals(siteView.isDefault()))
                .build();
    }

}