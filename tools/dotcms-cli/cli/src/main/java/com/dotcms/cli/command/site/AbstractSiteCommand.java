package com.dotcms.cli.command.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.cli.common.AuthenticationMixin;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
import org.apache.commons.lang3.BooleanUtils;
import picocli.CommandLine;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

public abstract class AbstractSiteCommand {

    @Inject
    protected RestClientFactory clientFactory;

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected AuthenticationMixin authenticationMixin;

    @CommandLine.Mixin
    protected HelpOptionMixin helpOptionMixin;

     String shortFormat(final Site site) {
        return String.format(
                "name: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] inode: [@|bold,underline,green %s|@] live:[@|bold,yellow %s|@] default: [@|bold,yellow %s|@] archived: [@|bold,yellow %s|@]",
                site.hostName(),
                site.identifier(),
                site.inode(),
                BooleanUtils.toStringYesNo(site.isLive()),
                BooleanUtils.toStringYesNo(site.isDefault()),
                BooleanUtils.toStringYesNo(site.isArchived())
        );
    }

      String shortFormat(final SiteView site) {
        return String.format(
                "name: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] inode: [@|bold,underline,green %s|@] live:[@|bold,yellow %s|@] default: [@|bold,yellow %s|@] archived: [@|bold,yellow %s|@]",
                site.hostName(),
                site.identifier(),
                site.inode(),
                BooleanUtils.toStringYesNo(site.isLive()),
                BooleanUtils.toStringYesNo(site.isDefault()),
                BooleanUtils.toStringYesNo(site.isArchived())
        );
    }

    SiteView findSite(String siteNameOrId) throws NotFoundException {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        if (siteNameOrId.replace("-", "").matches("[a-fA-F0-9]{32}")) {
            final ResponseEntityView<SiteView> byId = siteAPI.findById(siteNameOrId);
            return byId.entity();
        }

        final ResponseEntityView<SiteView> byId = siteAPI.findByName(
                GetSiteByNameRequest.builder().siteName(siteNameOrId).build());
        return byId.entity();


    }

}
