package com.dotcms.api.client.pull.site;

import com.dotcms.api.client.pull.GeneralPullHandler;
import com.dotcms.api.client.util.NamingUtils;
import com.dotcms.model.pull.PullOptions;
import com.dotcms.model.site.SiteView;
import java.util.List;
import jakarta.enterprise.context.Dependent;
import org.apache.commons.lang3.BooleanUtils;

@Dependent
public class SitePullHandler extends GeneralPullHandler<SiteView> {

    @Override
    public String title() {
        return "Sites";
    }

    @Override
    public String startPullingHeader(final List<SiteView> contents) {

        return String.format("\r@|bold,green [%d]|@ %s to pull",
                contents.size(),
                title()
        );
    }

    @Override
    public String displayName(final SiteView site) {
        return site.hostName();
    }

    @Override
    public String fileName(final SiteView site) {
        return NamingUtils.siteFileName(site);
    }

    @Override
    public String shortFormat(final SiteView site, final PullOptions pullOptions) {
        
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

}
