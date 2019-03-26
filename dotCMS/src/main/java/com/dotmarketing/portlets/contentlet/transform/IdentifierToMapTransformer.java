package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.HashMap;
import java.util.Map;

/** DBTransformer that converts DB objects into Contentlet instances */
public class IdentifierToMapTransformer implements FieldsToMapTransformer {
  final Map<String, Object> mapOfMaps;

  public IdentifierToMapTransformer(final Contentlet con) {
    if (con.getInode() == null || con.getIdentifier() == null) {
      throw new DotStateException("Contentlet needs an identifier to get properties");
    }

    final Map<String, Object> map = new HashMap<>();
    try {

      final Identifier id = APILocator.getIdentifierAPI().find(con.getIdentifier());
      map.put("id", id.getId());
      map.put("parentPath", id.getParentPath());
      map.put("path", id.getPath());
      map.put("hostId", id.getHostId());

    } catch (DotDataException e) {
      throw new DotStateException(
          String.format(
              "Unable to get the Identifier for given contentlet with id= %s", con.getIdentifier()),
          e);
    }
    final Map<String, Object> newMap = new HashMap<>();
    newMap.put("identifier", con.getIdentifier());
    newMap.put("identifierMap", map);

    this.mapOfMaps = newMap;
  }

  @Override
  public Map<String, Object> asMap() {
    return this.mapOfMaps;
  }
}
