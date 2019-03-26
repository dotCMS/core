package com.dotcms.content.business;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;

public interface ContentMappingAPI {

  // public Object buildMapping(Structure struct) throws DotMappingException;

  public Object toMappedObj(Contentlet con) throws DotMappingException;

  /*public Contentlet toContentlet(String string) throws DotMappingException;
  public Contentlet toContentlet(Map<String, Object> map) throws DotMappingException;*/

  public List<String> dependenciesLeftToReindex(Contentlet con)
      throws DotStateException, DotDataException, DotSecurityException;
}
