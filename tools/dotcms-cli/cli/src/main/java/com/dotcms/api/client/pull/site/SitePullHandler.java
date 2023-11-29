package com.dotcms.api.client.pull.site;

import com.dotcms.api.client.pull.GenericPullHandler;
import com.dotcms.model.pull.PullOptions;
import com.dotcms.model.site.SiteView;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.Dependent;
import org.apache.commons.lang3.BooleanUtils;

@Dependent
public class SitePullHandler implements GenericPullHandler<SiteView> {

    @Override
    public String title() {
        return "Sites";
    }

    @Override
    public String startPullingHeader(List<SiteView> contents) {

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
        return site.hostName();
    }

    @Override
    public String shortFormat(final SiteView site, final PullOptions pullOptions,
            final Map<String, Object> customOptions) {
        
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
