package com.dotcms.variant;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.validation.Preconditions;
import com.dotcms.util.DotPreconditions;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;


/**
 * API of {@link Variant}
 */
public class VariantAPIImpl implements VariantAPI {
    private final VariantFactory variantFactory;
    private final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();


    public VariantAPIImpl(){
        variantFactory = FactoryLocator.getVariantFactory();
    }

    /**
     * Implementation for {@link VariantAPI#save(Variant)}
     * @param variant
     *
     * @return
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public Variant save(final Variant variant) throws DotDataException {

        DotPreconditions.checkNotNull(variant.name(), IllegalArgumentException.class,
                "Variant name should not be null");
        DotPreconditions.checkArgument(!variant.archived(), "Variant can not be created as archive");

        Logger.debug(this, ()-> "Saving Variant: " + variant);

        return variantFactory.save(variant);
    }

    /**
     * Implementation for {@link VariantAPI#update(Variant)}
     * @param variant
     *
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void update(final Variant variant) throws DotDataException {
        Preconditions.checkNotNull(variant.name(), IllegalArgumentException.class,
                "Variant name should not be null");
        Preconditions.checkNotNull(variant.name(), IllegalArgumentException.class ,
                "Variant ID should not be null");
        DotPreconditions.isTrue(!variant.name().equals(DEFAULT_VARIANT.name()),
                "DEFAULT variant can not be updated");

        get(variant.name())
                .orElseThrow(() -> new DoesNotExistException("The variant does not exists"));

        Logger.debug(this, () -> "Updating Variant: " + variant);
        variantFactory.update(variant);
    }

    /**
     * Implementation for {@link VariantAPI#delete(String)}
     * @param id Variant's id to be deleted
     */
    @Override
    @WrapInTransaction
    public void delete(String id) throws DotDataException {
        final Variant variant = get(id).orElseThrow(() -> new DoesNotExistException("The variant must exists"));

        DotPreconditions.isTrue(!variant.name().equals(DEFAULT_VARIANT.name()),
                "DEFAULT variant can not be deleted");

        DotPreconditions.checkArgument(variant.archived(),
                DotStateException.class,
                "The Variant must be archived to be able to delete it");

        Logger.debug(this, ()-> "Deleting Variant: " + variant);

        variantFactory.delete(id);

        deleteAllContentletVersions(variant);
        deleteMultiTrees(variant);

    }

    private void deleteMultiTrees(final Variant variant) throws DotDataException {
        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final List<MultiTree> multiTrees = multiTreeAPI.getMultiTrees(variant);
        multiTreeAPI.deleteMultiTree(multiTrees);

    }

    private static void deleteAllContentletVersions(Variant variant) throws DotDataException {
        try {
            APILocator.getContentletAPI().getAllContentByVariants(APILocator.systemUser(), false , variant.name())
                    .stream()
                    .map(contentlet -> getIdentifier(contentlet))
                    .flatMap(identifier -> getVersions(variant, identifier).stream())
                    .forEach(contentlet -> deleteContentlet(contentlet));
        } catch (DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    private static Collection<Contentlet> getVersions(Variant variant, Identifier identifier) {
        try {
            return APILocator.getContentletAPI()
                    .findAllVersions(identifier, variant, APILocator.systemUser(), false);
        } catch (DotSecurityException | DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static Identifier getIdentifier(Contentlet contentlet)  {
        try {
            return APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteContentlet(final Contentlet contentlet) {
        try {
            APILocator.getVersionableAPI().deleteContentletVersionInfoByVariant(contentlet);
            APILocator.getContentletAPI().deleteVersion(contentlet, APILocator.systemUser(), false);
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Implementation for {@link VariantAPI#archive(String)}
     * @param name Variant's id to be archive
     */
    @Override
    @WrapInTransaction
    public void archive(final String name) throws DotDataException {
        final Variant variant = get(name)
                .orElseThrow(() -> new DoesNotExistException("The Variant does not exists"));

        final Variant variantArchived = variant.builder()
                .name(variant.name())
                .description(variant.description())
                .archived(true).build();

        Logger.debug(this, () -> "Archiving Variant: " + variant);
        update(variantArchived);
    }

    /**
     * Implementation for {@link VariantAPI#get(String)} (String)}
     *
     * @param name {@link Variant}'s identifier
     * @return
     */
    @Override
    @CloseDBIfOpened
    public Optional<Variant> get(final String name) throws DotDataException {
        Preconditions.checkNotNull(name, "Variant Name should not be null");
        Logger.debug(this, ()-> "Getting Variant by Name: " + name);

        return variantFactory.get(name);
    }

    @Override
    @CloseDBIfOpened
    public List<Variant> getVariants() throws DotDataException {
        return variantFactory.getVariants();
    }

    @WrapInTransaction
    @Override
    public void promote(final Variant variant, final User user) throws DotDataException {

        DotPreconditions.checkArgument(!variant.name().equals(DEFAULT_VARIANT.name()),
                "DEFAULT variant can not be promoted");

        final Variant variantFromDatabase = APILocator.getVariantAPI().get(variant.name())
                .orElseThrow(() -> new DoesNotExistException(
                        String.format("Variant `%s` does not exists", variant.name())));


        Logger.debug(this, () -> "Promoting Variant: " + variantFromDatabase);

        DotPreconditions.checkArgument(!variantFromDatabase.archived(), "An archived Variant can not be promoted");

        APILocator.getVersionableAPI().findAllByVariant(variantFromDatabase).stream()
                .forEach(contentletVersionInfo -> promoteToDefault(user, contentletVersionInfo));

        final List<MultiTree> variantMultiTrees = multiTreeAPI.getMultiTrees(variant);
        variantMultiTrees.stream().map(MultiTree::getHtmlPage)
                .forEach(pageId -> deleteMultiTree(VariantAPI.DEFAULT_VARIANT, pageId));

        final Map<String, List<MultiTree>> multiTreeByPageId = sortByPageId(variantMultiTrees);

        for (Entry<String, List<MultiTree>> entry : multiTreeByPageId.entrySet()) {
            multiTreeAPI.copyMultiTree(entry.getKey(), entry.getValue(), VariantAPI.DEFAULT_VARIANT.name());


        }
    }

    @NotNull
    private static Map<String, List<MultiTree>> sortByPageId(final List<MultiTree> variantMultiTrees) {
        final Map<String, List<MultiTree>> multiTreeByPageId = new HashMap<>();

        for (final MultiTree multiTree : variantMultiTrees) {
            final List<MultiTree> multiTrees = multiTreeByPageId.get(multiTree.getHtmlPage());

            if (multiTrees == null) {
                multiTreeByPageId.put(multiTree.getHtmlPage(), Lists.newArrayList(multiTree));
            } else {
                multiTrees.add(multiTree);
            }
        }
        return multiTreeByPageId;
    }

    private void deleteMultiTree(Variant variant, String pageId) {
        try {
            multiTreeAPI.deleteMultiTree(pageId, variant.name());
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static void promoteToDefault(final User user, final ContentletVersionInfo contentletVersionInfo) {
        try {
            if (UtilMethods.isSet(contentletVersionInfo.getLiveInode())) {
                final Contentlet liveContentlet = APILocator.getContentletAPI()
                        .find(contentletVersionInfo.getLiveInode(), user, false);

                final Contentlet contentletOnVariant = APILocator.getContentletAPI()
                        .copyContentToVariant(liveContentlet, VariantAPI.DEFAULT_VARIANT.name(), user);

                APILocator.getContentletAPI().publish(contentletOnVariant, user, false);
            }

            if (!contentletVersionInfo.getWorkingInode().equals(contentletVersionInfo.getLiveInode())) {

                final Contentlet workingContentlet = APILocator.getContentletAPI()
                        .find(contentletVersionInfo.getWorkingInode(), user, false);

                APILocator.getContentletAPI()
                        .copyContentToVariant(workingContentlet, VariantAPI.DEFAULT_VARIANT.name(), user);
            }

        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}
