/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundler;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtil.HandlerType;
import com.dotcms.exception.ExceptionUtil;
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
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
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
import java.util.Objects;

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

  private void deleteContainer(final Container container) {
    try {
      final Container workingContainer =
          APILocator.getContainerAPI().getWorkingContainerById(container.getIdentifier(), APILocator.getUserAPI().getSystemUser(), false);

      if (workingContainer != null && InodeUtils.isSet(workingContainer.getInode())) {

        APILocator.getContainerAPI().delete(workingContainer, APILocator.getUserAPI().getSystemUser(), false);
      }
    } catch (final Exception e) {
      Logger.error(ContainerHandler.class, String.format("An error occurred when deleting Container '%s' [%s]: %s",
              container.getTitle(), container.getIdentifier(), ExceptionUtil.getErrorMessage(e)), e);
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
        if(UtilMethods.isEmpty(localHost::getIdentifier)){
            Logger.warn(this.getClass(), "Ignoring container on non-existing site. Id:" + container.getIdentifier() + ", title:" + Try.of(
                  container::getTitle).getOrElse("unknown") + ". Unable to find referenced host id:" + containerId.getHostId());
          continue;
        }

        if (containerWrapper.getOperation().equals(PushPublisherConfig.Operation.UNPUBLISH)) {
          String containerIden = container.getIdentifier();
          deleteContainer(container);

          PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.UNPUBLISH, containerIden, container.getInode(),
              container.getName(), config.getId());
        } else {
          // save if it doesn't exists
          final Container existing = containerAPI.find(container.getInode(), systemUser, false);
          if (UtilMethods.isEmpty(()->existing.getIdentifier())) {
            containerAPI.save(container, containerWrapper.getCsList(), localHost, systemUser, false);
            PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.PUBLISH_CREATE, container.getIdentifier(),
                container.getInode(), container.getName(), config.getId());
          } else {
            container= containerAPI.save(existing, containerWrapper.getCsList(), localHost, systemUser, false);
            PushPublishLogger.log(getClass(), PushPublishHandler.CONTAINER, PushPublishAction.PUBLISH_UPDATE, container.getIdentifier(),
                container.getInode(), container.getName(), config.getId());
          }
        }

        HandlerUtil.setModUser(container.getInode(), modUser, HandlerType.CONTAINERS);
        CacheLocator.getContainerCache().remove(container);
        new ContainerLoader().invalidate(container);

        if (!unpublish) {
          final VersionInfo info = containerWrapper.getCvi();
          if(!Objects.equals(info.getWorkingInode(), info.getLiveInode())){
            boolean workingNotExists = new DotConnect()
                    .setSQL("select inode from dot_containers where inode=?")
                    .addParam(info.getWorkingInode())
                    .loadResults()
                    .isEmpty();
            if(workingNotExists){
              info.setWorkingInode(container.getInode());
            }
          }

          
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
                    " cache: %s", identifierToDelete, ExceptionUtil.getErrorMessage(e)), e);
        }

      }

    } catch (final Exception e) {
        final String errorMsg = String.format("An error occurred when processing Container in '%s' with Title '%s' [%s]: %s",
                workingOn, (null == container ? "(empty)" : container.getTitle()), (null == container ? "(empty)" :
                        container.getIdentifier()), ExceptionUtil.getErrorMessage(e));
        Logger.error(this.getClass(), errorMsg, e);
        throw new DotPublishingException(errorMsg, e);
    }
  }

}
