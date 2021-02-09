package com.dotcms.datagen;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.publishing.remote.bundler.AssignableFromMap;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.function.Function;

import static org.jgroups.util.Util.assertTrue;

public class BundleDataGen extends AbstractDataGen<Bundle> {
    public static AssignableFromMap<BundleDataGen.MetaData> howAddInBundle;

    private String name;
    private PushPublisherConfig config;
    private Set<AssetsItem> assets = new HashSet<>();
    private FilterDescriptor filter;

    static{
        howAddInBundle = new AssignableFromMap<>();

        howAddInBundle.put(ContentType.class, new MetaData(
                (PushPublisherConfig config) -> config.getStructures(),
                (Object asset) -> ((ContentType) asset).id(),
                PusheableAsset.CONTENT_TYPE
            )
        );

        howAddInBundle.put(Host.class, new MetaData(
                        (PushPublisherConfig config) -> config.getHostSet(),
                        (Object asset) -> Host.class.isInstance(asset) ? ((Host) asset).getIdentifier() : ((Contentlet) asset).getIdentifier(),
                        PusheableAsset.SITE
                )
        );

        howAddInBundle.put(WorkflowScheme.class, new MetaData(
                        (PushPublisherConfig config) -> config.getWorkflows(),
                        (Object asset) -> ((WorkflowScheme) asset).getId(),
                        PusheableAsset.WORKFLOW
                )
        );

        howAddInBundle.put(Category.class, new MetaData(
                        (PushPublisherConfig config) -> config.getCategories(),
                        (Object asset) -> ((Category) asset).getInode(),
                        PusheableAsset.CATEGORY
                )
        );

        howAddInBundle.put(Folder.class, new MetaData(
                        (PushPublisherConfig config) -> config.getFolders(),
                        (Object asset) -> ((Folder) asset).getInode(),
                        PusheableAsset.FOLDER
                )
        );

        howAddInBundle.put(Template.class, new MetaData(
                        (PushPublisherConfig config) -> config.getTemplates(),
                        (Object asset) -> ((Template) asset).getIdentifier(),
                        PusheableAsset.TEMPLATE
                )
        );

        howAddInBundle.put(Container.class, new MetaData(
                        (PushPublisherConfig config) -> config.getContainers(),
                        (Object asset) -> ((Container) asset).getIdentifier(),
                        PusheableAsset.CONTAINER
                )
        );

        howAddInBundle.put(Contentlet.class, new MetaData(
                        (PushPublisherConfig config) -> config.getContentlets(),
                        (Object asset) -> ((Contentlet) asset).getIdentifier(),
                        PusheableAsset.CONTENTLET
                )
        );

        howAddInBundle.put(Link.class, new MetaData(
                        (PushPublisherConfig config) -> config.getLinks(),
                        (Object asset) -> ((Link) asset).getIdentifier(),
                        PusheableAsset.LINK
                )
        );

        howAddInBundle.put(Rule.class, new MetaData(
                        (PushPublisherConfig config) -> config.getRules(),
                        (Object asset) -> ((Rule) asset).getId(),
                        PusheableAsset.RULE
                )
        );

        howAddInBundle.put(Language.class, new MetaData(
                        (PushPublisherConfig config) -> config.getLanguages(),
                        (Object asset) -> Long.toString(((Language) asset).getId()),
                        PusheableAsset.LANGUAGE
                )
        );
    }

    public BundleDataGen name(final String name) {
        this.name = name;
        return this;
    }

    public BundleDataGen pushPublisherConfig(final PushPublisherConfig config) {
        this.config = config;
        return this;
    }

    public BundleDataGen addAsset(final String inode, final PusheableAsset pusheableAsset){
        assets.add(new AssetsItem(inode, pusheableAsset));
        return this;
    }

    @Override
    public Bundle next() {
        final String bundleName = name != name ? name : "testBundle" + System.currentTimeMillis();
        final Bundle bundle = new Bundle(bundleName, new Date(), null, user.getUserId());

        if (filter != null) {
            bundle.setFilterKey(filter.getKey());
        }

        return bundle;

    }

    private List<PublishQueueElement> getPublishQueueElements(Bundle bundle) {
        final List<PublishQueueElement> publishQueueElements = new ArrayList<>();

        for (final AssetsItem assetItem : assets) {
            final PublishQueueElement publishQueueElement = new PublishQueueElement();
            publishQueueElement.setId(1);
            publishQueueElement.setOperation(PublisherConfig.Operation.PUBLISH.ordinal());
            publishQueueElement.setAsset(assetItem.inode);
            publishQueueElement.setEnteredDate(new Date());
            publishQueueElement.setPublishDate(new Date());
            publishQueueElement.setBundleId(bundle.getId());
            publishQueueElement.setType(assetItem.pusheableAsset.getType());

            publishQueueElements.add(publishQueueElement);
        }

        return publishQueueElements;
    }

    @Override
    public Bundle persist(Bundle bundle) {
        try {
            APILocator.getBundleAPI().saveBundle(bundle);
            final Bundle bundleFromDataBase = APILocator.getBundleAPI().getBundleByName(bundle.getName());

            if (config != null) {
                config.setAssets(getPublishQueueElements(bundleFromDataBase));
                config.setId(bundleFromDataBase.getId());
                config.setOperation(PublisherConfig.Operation.PUBLISH);
                config.setDownloading(true);
            }

            return bundleFromDataBase;
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    public BundleDataGen filter(final FilterDescriptor filterDescriptor) {
        this.filter = filterDescriptor;
        return this;
    }

    public BundleDataGen addAssets(final Collection<Object> assetsToAddInBundle) {
        for (Object asset : assetsToAddInBundle) {
            final MetaData metaData = howAddInBundle.get(asset.getClass());
            this.addAsset(metaData.dataToAdd.apply(asset), metaData.pusheableAsset);
        }

        return this;
    }

    private class AssetsItem {
        String inode;
        PusheableAsset pusheableAsset;

        public AssetsItem(String inode, PusheableAsset pusheableAsset) {
            this.inode = inode;
            this.pusheableAsset = pusheableAsset;
        }
    }

    public static class MetaData {
        public Function<PushPublisherConfig, Collection> collection;
        public Function<Object, String> dataToAdd;
        public PusheableAsset pusheableAsset;

        public MetaData(
                Function<PushPublisherConfig, Collection> collection,
                Function<Object, String> dataToAdd,
                PusheableAsset pusheableAsset) {

            this.collection = collection;
            this.dataToAdd = dataToAdd;
            this.pusheableAsset = pusheableAsset;
        }
    }
}
