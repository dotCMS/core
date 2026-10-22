package com.dotcms.variant;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.validation.Preconditions;
import com.dotcms.util.DotPreconditions;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
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
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Implementation for {@link VariantAPI#promote(Variant, User)}. Permissions on every affected
     * contentlet and page are verified up front, before any version or layout is touched, so a
     * refusal never leaves the DEFAULT variant half promoted.
     *
     * @param variant the {@link Variant} to promote
     * @param user    the {@link User} promoting it
     * @throws DotDataException     when the variant does not exist or a copy fails
     * @throws DotSecurityException when the user lacks permission on any affected content or page
     */
    @WrapInTransaction
    @Override
    public void promote(final Variant variant, final User user)
            throws DotDataException, DotSecurityException {

        DotPreconditions.checkArgument(!variant.name().equals(DEFAULT_VARIANT.name()),
                "DEFAULT variant can not be promoted");

        final Variant variantFromDatabase = APILocator.getVariantAPI().get(variant.name())
                .orElseThrow(() -> new DoesNotExistException(
                        String.format("Variant `%s` does not exists", variant.name())));


        Logger.debug(this, () -> "Promoting Variant: " + variantFromDatabase);

        DotPreconditions.checkArgument(!variantFromDatabase.archived(), "An archived Variant can not be promoted");

        final List<ContentletVersionInfo> versionInfos =
                APILocator.getVersionableAPI().findAllByVariant(variantFromDatabase);
        final List<MultiTree> variantMultiTrees = multiTreeAPI.getMultiTrees(variant);

        validatePromotePermissions(variantFromDatabase, versionInfos, variantMultiTrees, user);

        versionInfos.forEach(contentletVersionInfo -> promoteToDefault(user, contentletVersionInfo));

        variantMultiTrees.stream().map(MultiTree::getHtmlPage)
                .forEach(pageId -> deleteMultiTree(VariantAPI.DEFAULT_VARIANT, pageId));

        final Map<String, List<MultiTree>> multiTreeByPageId = sortByPageId(variantMultiTrees);

        for (Entry<String, List<MultiTree>> entry : multiTreeByPageId.entrySet()) {
            multiTreeAPI.copyMultiTree(entry.getKey(), entry.getValue(), VariantAPI.DEFAULT_VARIANT.name());


        }
    }

    /**
     * Checks that the user may replace everything the promotion is about to replace: PUBLISH on
     * each contentlet that has a live version in the variant (its DEFAULT live version gets
     * replaced), EDIT on each contentlet that only has a working version in it, and PUBLISH on
     * each page whose DEFAULT layout the variant's multi-tree rows are going to overwrite. Pages
     * are checked through a {@link PermissionableProxy}, the way the Experiments API does it.
     * The system user and CMS Administrators pass every check.
     *
     * @param variant           the {@link Variant} being promoted, for logging
     * @param versionInfos      the {@link ContentletVersionInfo} rows that exist in the variant
     * @param variantMultiTrees the {@link MultiTree} rows that exist in the variant
     * @param user              the {@link User} promoting the variant
     * @throws DotDataException     when a contentlet or permission lookup fails
     * @throws DotSecurityException when the user lacks any of the required permissions
     */
    private void validatePromotePermissions(final Variant variant,
            final List<ContentletVersionInfo> versionInfos,
            final List<MultiTree> variantMultiTrees,
            final User user) throws DotDataException, DotSecurityException {

        // split the variant's contentlets by the permission the promotion needs on them
        final Set<String> workingInodesWithLive = versionInfos.stream()
                .filter(info -> UtilMethods.isSet(info.getLiveInode()))
                .map(ContentletVersionInfo::getWorkingInode)
                .collect(Collectors.toSet());
        final List<String> workingInodes = versionInfos.stream()
                .map(ContentletVersionInfo::getWorkingInode)
                .filter(UtilMethods::isSet)
                .collect(Collectors.toList());
        final List<Contentlet> contentlets = workingInodes.isEmpty() ? List.of()
                : APILocator.getContentletAPI().findContentlets(workingInodes);
        final Map<Boolean, List<Contentlet>> byHasLive = contentlets.stream()
                .collect(Collectors.partitioningBy(
                        contentlet -> workingInodesWithLive.contains(contentlet.getInode())));

        final Optional<Contentlet> unpublishable = firstWithoutPermission(byHasLive.get(true),
                PermissionAPI.PERMISSION_PUBLISH, user);
        if (unpublishable.isPresent()) {
            throw deny(variant, user, "publish contentlet " + unpublishable.get().getIdentifier());
        }
        final Optional<Contentlet> uneditable = firstWithoutPermission(byHasLive.get(false),
                PermissionAPI.PERMISSION_EDIT, user);
        if (uneditable.isPresent()) {
            throw deny(variant, user, "edit contentlet " + uneditable.get().getIdentifier());
        }

        final List<PermissionableProxy> pages = variantMultiTrees.stream()
                .map(MultiTree::getHtmlPage)
                .distinct()
                .map(pageId -> {
                    final PermissionableProxy page = new PermissionableProxy();
                    page.setIdentifier(pageId);
                    page.setType("htmlpage");
                    return page;
                })
                .collect(Collectors.toList());

        final Optional<PermissionableProxy> unpublishablePage = firstWithoutPermission(pages,
                PermissionAPI.PERMISSION_PUBLISH, user);
        if (unpublishablePage.isPresent()) {
            throw deny(variant, user, "publish page " + unpublishablePage.get().getIdentifier());
        }
    }

    /**
     * Finds the first item the user lacks the given permission on. The whole list is resolved
     * in one batch query first; only the items that query leaves out are re-checked one by one,
     * because the batch lookup reads the {@code permission_reference} table, which is filled in
     * lazily, while the scalar check also walks the inheritance chain. On the normal path, where
     * everything is permitted, that means a single round-trip.
     *
     * @param items      the {@link Permissionable} items to check
     * @param permission the permission bit required on every item
     * @param user       the {@link User} to check for
     * @param <P>        the item type
     * @return the first item without the permission, or empty when the user holds it on all
     * @throws DotDataException     when a permission lookup fails
     * @throws DotSecurityException when the batch lookup refuses the user
     */
    private <P extends Permissionable> Optional<P> firstWithoutPermission(final List<P> items,
            final int permission, final User user) throws DotDataException, DotSecurityException {

        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Set<String> permitted = permissionAPI.filterCollection(items, permission, user, false)
                .stream().map(Permissionable::getPermissionId).collect(Collectors.toSet());
        for (final P item : items) {
            if (!permitted.contains(item.getPermissionId())
                    && !permissionAPI.doesUserHavePermission(item, permission, user, false)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * Builds, and logs, the exception for a refused promotion.
     *
     * @param variant the {@link Variant} that was being promoted
     * @param user    the {@link User} who was refused
     * @param what    which permission was missing, in plain words
     * @return the {@link DotSecurityException} to throw
     */
    private DotSecurityException deny(final Variant variant, final User user, final String what) {
        final String userId = user != null ? user.getUserId() : "Unknown";
        final String message = String.format(
                "User %s doesn't have permission to %s, required to promote Variant `%s`",
                userId, what, variant.name());
        Logger.warn(this, message);
        return new DotSecurityException(message);
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
