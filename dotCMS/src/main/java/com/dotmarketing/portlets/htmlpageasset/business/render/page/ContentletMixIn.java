package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * Restricts the JSON conversion of specific data in the Contentlet object.
 *
 * @author Will Ezell
 * @version 4.2
 * @since Oct 9, 2017
 */
abstract class ContentletMixIn {

  @JsonIgnore
  public abstract Permissionable getParentPermissionable();

  @JsonIgnore
  public abstract Structure getStructure();

  @JsonIgnore
  public abstract Structure getMap();
}
