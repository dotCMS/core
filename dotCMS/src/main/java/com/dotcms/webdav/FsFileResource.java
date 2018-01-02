/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package com.dotcms.webdav;

import com.dotcms.repackage.com.bradmcevoy.common.ContentTypeUtils;
import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.CopyableResource;
import com.dotcms.repackage.com.bradmcevoy.http.DeletableResource;
import com.dotcms.repackage.com.bradmcevoy.http.GetableResource;
import com.dotcms.repackage.com.bradmcevoy.http.MoveableResource;
import com.dotcms.repackage.com.bradmcevoy.http.PropFindableResource;
import com.dotcms.repackage.com.bradmcevoy.http.PropPatchableResource;
import com.dotcms.repackage.com.bradmcevoy.http.Range;
import com.dotcms.repackage.com.bradmcevoy.http.Request;
import com.dotcms.repackage.com.bradmcevoy.http.entity.PartialEntity;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotFoundException;
import com.dotcms.repackage.com.bradmcevoy.http.webdav.PropPatchHandler.Fields;
import com.dotcms.repackage.com.bradmcevoy.io.ReadingException;
import com.dotcms.repackage.com.bradmcevoy.io.WritingException;
import com.dotcms.webdav.FileContentService;


import java.io.*;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class FsFileResource extends FsResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource, PropFindableResource, PropPatchableResource {

    private static final Logger log = LoggerFactory.getLogger(FsFileResource.class);
    
    private final FileContentService contentService;

    /**
     *
     * @param host - the requested host. E.g. www.mycompany.com
     * @param factory
     * @param file
     */
    public FsFileResource(String host, FileSystemResourceFactory factory, File file, FileContentService contentService) {
        super(host, factory, file);
        this.contentService = contentService;
    }

    @Override
    public Long getContentLength() {
        return file.length();
    }

    @Override
    public String getContentType(String preferredList) {
        String mime = ContentTypeUtils.findContentTypes(this.file);
        String s = ContentTypeUtils.findAcceptableContentType(mime, preferredList);
        if (log.isTraceEnabled()) {
            log.trace("getContentType: preferred: {} mime: {} selected: {}", new Object[]{preferredList, mime, s});
        }
        return s;
    }

    @Override
    public String checkRedirect(Request arg0) {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotFoundException {
      try (InputStream in  = contentService.getFileContent(file)){
            if (range != null) {
                log.debug("sendContent: ranged content: " + file.getAbsolutePath());
                PartialEntity.writeRange(in, range, out);
            } else {
                log.debug("sendContent: send whole file " + file.getAbsolutePath());
                IOUtils.copy(in, out);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            throw new NotFoundException("Couldnt locate content");
        } catch (ReadingException e) {
            throw new IOException(e);
        } catch (WritingException e) {
            throw new IOException(e);
        } 
    }

    /**
     * @{@inheritDoc}
     */
    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return factory.maxAgeSeconds(this);
    }

    /**
     * @{@inheritDoc}
     */
    @Override
    protected void doCopy(File dest) {
        try {
            FileUtils.copyFile(file, dest);
        } catch (IOException ex) {
            throw new RuntimeException("Failed doing copy to: " + dest.getAbsolutePath(), ex);
        }
    }

    @Deprecated
    @Override
    public void setProperties(Fields fields) {
        // MIL-50
        // not implemented. Just to keep MS Office sweet
    }
}
