package com.dotcms.contenttype.model.relationship;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.Serializable;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ContentletRelationships implements Serializable {

  private static final long serialVersionUID = 1L;

  public abstract Contentlet getContentlet();

  public abstract List<ContentletRelationshipRecords> getRelationshipsRecords();
}
