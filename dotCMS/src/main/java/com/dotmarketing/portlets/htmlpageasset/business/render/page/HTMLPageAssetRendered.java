package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import java.util.Collection;

/** It is a {@link PageView} rendered */
@JsonSerialize(using = HTMLPageAssetRenderedSerializer.class)
public class HTMLPageAssetRendered extends PageView {
  private final String html;

  public HTMLPageAssetRendered(
      final Host site,
      final Template template,
      final Collection<? extends ContainerRaw> containers,
      final HTMLPageAssetInfo page,
      final TemplateLayout layout,
      final String html,
      final boolean canCreateTemplate,
      final boolean canEditTemplate,
      final ViewAsPageStatus viewAs) {

    super(site, template, containers, page, layout, canCreateTemplate, canEditTemplate, viewAs);
    this.html = html;
  }

  public String getHtml() {
    return html;
  }
}
