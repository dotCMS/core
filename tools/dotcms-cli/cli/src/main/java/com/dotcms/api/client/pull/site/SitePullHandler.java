package com.dotcms.api.client.pull.site;

import com.dotcms.api.client.pull.PullHandler;
import com.dotcms.model.site.SiteView;
import javax.enterprise.context.Dependent;
import org.apache.commons.lang3.BooleanUtils;

@Dependent
public class SitePullHandler implements PullHandler<SiteView> {

    @Override
    public String title() {
        return "Sites";
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
    public String shortFormat(final SiteView site) {
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
