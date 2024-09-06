/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundler;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtil.HandlerType;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.ContainerWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.thoughtworks.xstream.XStream;
import io.vavr.control.Try;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Container-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link Container} data
 * files.
 * <p>
 * Containers in dotCMS allow you to specify both what types of Content can be added to a Page and how content of each
 * of those Content Types is displayed.
 *
 * @author root
 * @since Mar 7, 2013
 */
public class ContainerHandler implements IHandler {

  private final UserAPI userAPI = APILocator.getUserAPI();
  private final ContainerAPI containerAPI = APILocator.getContainerAPI();
  private final List<String> infoToRemove = new ArrayList<>();
  private final PublisherConfig config;

  public ContainerHandler(final PublisherConfig config) {
    this.config = config;
  }

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  private void deleteContainer(final Container container) throws DotPublishingException, DotDataException {
    try {
      final Container workingContainer =
          APILocator.getContainerAPI().getWorkingContainerById(container.getIdentifier(), APILocator.getUserAPI().getSystemUser(), false);

      if (workingContainer != null && InodeUtils.isSet(workingContainer.getInode())) {

        APILocator.getContainerAPI().delete(workingContainer, APILocator.getUserAPI().getSystemUser(), false);
      }
    } catch (final Exception e) {
      Logger.error(ContainerHandler.class, String.format("An error occurred when deleting Container '%s' [%s]: %s",
              container.getTitle(), container.getIdentifier(), e.getMessage()), e);
    }
  }

  @Override
  public void handle(final File bundleFolder) throws Exception {

    if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

      throw new RuntimeException("need an enterprise pro license to run this");
    }

    final Collection<File> containers = FileUtil.listFilesRecursively(bundleFolder, new ContainerBundler().getFileFilter());
    handleContainers(containers);
  }

  private void handleContainers(final Collection<File> containers) throws DotPublishingException, DotDataException {

    if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

      throw new RuntimeException("need an enterprise pro license to run this");
    }
    Container container = null;
    boolean unpublish;
    final User systemUser = userAPI.getSystemUser();
    File workingOn = null;
    try {
      XStream xstream = XStreamHandler.newXStreamInstance();
      // Handle folders
      for (final File containerFile : containers) {
        workingOn = containerFile;
        if (containerFile.isDirectory()) {
          continue;
        }
        ContainerWrapper containerWrapper;
        try (InputStream input = Files.newInputStream(containerFile.toPath())) {
          containerWrapper = (ContainerWrapper) xstream.fromXML(input);
        }

        // skip file based containers
        container = containerWrapper.getContainer();
        if (container instanceof FileAssetContainer) {
          continue;
        }
        
        String modUser = container.getModUser();
        Identifier containerId = containerWrapper.getContainerId();

        unpublish = containerWrapper.getOperation().equals(Operation.UNPUBLISH);

        Host localHost = APILocator.getHostAPI().find(containerId.getHostId(), systemUser, false);

        if (containerWrapper.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH)) {
          String containerIden = container.getIdentifier();
          deleteContainer(container);

          PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.UNPUBLISH, containerIden, container.getInode(),
              container.getName(), config.getId());
        } else {
          // save if it doesn't exists
          final Container existing = containerAPI.find(container.getInode(), systemUser, false);
          if (existing == null || !InodeUtils.isSet(existing.getIdentifier())) {
            containerAPI.save(container, containerWrapper.getCsList(), localHost, systemUser, false);
            PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.PUBLISH_CREATE, container.getIdentifier(),
                container.getInode(), container.getName(), config.getId());
          } else {
            containerAPI.save(existing, containerWrapper.getCsList(), localHost, systemUser, false);
            PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.PUBLISH_UPDATE, container.getIdentifier(),
                container.getInode(), container.getName(), config.getId());
          }
        }

        HandlerUtil.setModUser(container.getInode(), modUser, HandlerType.CONTAINERS);
        CacheLocator.getContainerCache().remove(container);
        new ContainerLoader().invalidate(container);

        if (!unpublish) {
          final VersionInfo info = containerWrapper.getCvi();
          if (info.isLocked() && info.getLockedBy() != null) {
            final User user = Try.of(()-> APILocator.getUserAPI().loadUserById(info.getLockedBy())).getOrElse(systemUser);
            info.setLockedBy(user.getUserId());
          }
          infoToRemove.add(info.getIdentifier());
          APILocator.getVersionableAPI().saveVersionInfo(info);
        }
        String identifierToDelete = StringPool.BLANK;
        try {
          for (final String ident : infoToRemove) {
            identifierToDelete = ident;
            APILocator.getVersionableAPI().removeVersionInfoFromCache(ident);
          }
        } catch (final Exception e) {
            throw new DotPublishingException(String.format("Unable to remove Container Version Info with ID '%s' from" +
                    " cache: %s", identifierToDelete, e.getMessage()), e);
        }

      }

    } catch (final Exception e) {
        final String errorMsg = String.format("An error occurred when processing Container in '%s' with ID '%s': %s",
                workingOn, (null == container ? "(empty)" : container.getTitle()), (null == container ? "(empty)" :
                        container.getIdentifier()), e.getMessage());
        Logger.error(this.getClass(), errorMsg, e);
        throw new DotPublishingException(errorMsg, e);
    }
  }


}
