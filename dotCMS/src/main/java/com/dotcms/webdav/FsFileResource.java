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

import io.milton.common.*;
import io.milton.http.*;
import io.milton.resource.*;
import io.milton.http.entity.PartialEntity;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.http.webdav.PropPatchHandler.Fields;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 */
public class FsFileResource extends FsResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource, PropFindableResource {

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
        return ContentTypeUtils.findAcceptableContentType(mime, preferredList);
    }

    @Override
    public String checkRedirect(Request arg0) {
        return null;
    }

    public class RangeInputStream extends InputStream
    {
        private InputStream parent;
        private long remaining;

        public RangeInputStream(InputStream parent, long start, long end) throws IOException
        {
            if (end < start)
            {
                throw new IllegalArgumentException("end < start");
            }

            if (parent.skip(start) < start)
            {
                throw new IOException("Unable to skip leading bytes");
            }

            this.parent=parent;
            remaining = end - start;
        }

        @Override
        public int read() throws IOException
        {
            return --remaining >= 0 ? parent.read() : -1;
        }
    }
    
    
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType)
                    throws IOException, NotFoundException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (range != null) {
                RangeInputStream rin = new RangeInputStream(in, range.getStart(), range.getFinish());
                IOUtils.copy(rin, out);
                Logger.debug(this.getClass(), "sendContent: ranged content: " + file.getAbsolutePath());
                return;
            }

            Logger.debug(this.getClass(), "sendContent: send whole file " + file.getAbsolutePath());
            IOUtils.copy(in, out);

            out.flush();
        } catch (Exception e) {
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

    @Override
    public LockResult refreshLock(String token, LockTimeout timeout) throws NotAuthorizedException, PreConditionFailedException {
        // TODO Auto-generated method stub
        return null;
    }


}
