package com.dotcms.rendering;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.rendering.RenderModeHandler.Function;
import com.dotcms.rendering.velocity.rendermode.VelocityAdminMode;
import com.dotcms.rendering.velocity.rendermode.VelocityEditMode;
import com.dotcms.rendering.velocity.rendermode.VelocityLiveMode;
import com.dotcms.rendering.velocity.rendermode.VelocityNavigateEditMode;
import com.dotcms.rendering.velocity.rendermode.VelocityPreviewMode;
import com.dotcms.repackage.jersey.repackaged.com.google.common.collect.ImmutableMap;
import com.dotmarketing.util.PageMode;

final class VelocityModeMap implements RenderModeMapper {
    private static final Map<PageMode, Function> pageModeVelocityMap =ImmutableMap.<PageMode, RenderModeHandler.Function>builder()
            .put(PageMode.PREVIEW_MODE, VelocityPreviewMode::new)
            .put(PageMode.EDIT_MODE, VelocityEditMode::new)
            .put(PageMode.LIVE, VelocityLiveMode::new)
            .put(PageMode.ADMIN_MODE, VelocityAdminMode::new)
            .put(PageMode.NAVIGATE_EDIT_MODE, VelocityNavigateEditMode::new)
            .build();
    @Override
    public Map<PageMode, Function> getModMop() {
        return pageModeVelocityMap;
    }

    @Override
    public boolean useModes(HttpServletRequest request) {
        return true;
    }

}
