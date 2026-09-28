package com.dotcms.jobs.business.processor.impl;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Duplicates one folder in place, holding everything its source holds (#37062).
 * <p>
 * <b>Two passes, one transaction.</b> The shipped {@link FolderAPI#copy} walk carries file assets
 * that are working and not archived, working pages, links and subfolders, and nothing else, so a
 * generic contentlet or an archived item is left behind. This runs that walk unchanged, then a
 * complement pass that copies, folder by folder through the new subtree, every contentlet the walk
 * did not carry. Both run inside one transaction, so a failure leaves no partial duplicate, and
 * the {@code COPY_FOLDER} event the walk queues for commit fires only once the content is in place.
 * <p>
 * The complement reads the {@code identifier} table directly: one row per item whatever its
 * versions, languages or archived state, which is the database-first source for folder contents.
 * Each missing identifier is copied with {@link ContentletAPI#copyContentlet}, which copies every
 * version and keeps each one's live and archived state. Relationships therefore point at the
 * originals, as the walk's own copies do.
 * <p>
 * {@code FolderAPIImpl} and {@code FolderFactoryImpl} are not modified.
 *
 * @author dotCMS
 */
final class FolderDuplicator {

    /** The suffix the walk appends, once or more, until the duplicate's name is free. */
    private static final String COPY_SUFFIX = "_copy";

    private final FolderAPI folderAPI;
    private final ContentletAPI contentletAPI;

    FolderDuplicator() {
        this(APILocator.getContentletAPI());
    }

    /**
     * @param contentletAPI copies the contentlets the walk leaves behind; a test passes one that
     *                      fails, to prove a failure rolls the whole duplicate back
     */
    FolderDuplicator(final ContentletAPI contentletAPI) {
        this.folderAPI = APILocator.getFolderAPI();
        this.contentletAPI = contentletAPI;
    }

    /**
     * Duplicates a folder beside itself, in its own parent.
     *
     * @param source the folder to duplicate
     * @param user   the submitting author; the walk checks their rights to read the source and to
     *               add to its parent
     * @return the duplicate
     * @throws Exception the duplicate could not be made; nothing was left behind
     */
    Folder duplicate(final Folder source, final User user) throws Exception {
        return LocalTransaction.wrapReturn(() -> {
            final User systemUser = APILocator.systemUser();
            final Folder parentFolder = parentFolderOf(source, systemUser);
            final Host site = APILocator.getHostAPI().find(source.getHostId(), systemUser, false);

            final Set<String> before = childNames(childrenOf(parentFolder, site, systemUser));
            if (parentFolder == null) {
                folderAPI.copy(source, site, user, false);
            } else {
                folderAPI.copy(source, parentFolder, user, false);
            }
            final Folder duplicate = identifyDuplicate(before,
                    childrenOf(parentFolder, site, systemUser), source.getName())
                    .orElseThrow(() -> new DotDataException(
                            "Could not tell which new folder duplicates " + source.getPath()));

            copyWhatTheWalkLeftBehind(source, duplicate, site, systemUser);
            return duplicate;
        });
    }

    /**
     * The one folder that appeared beside the source and is named after it.
     * <p>
     * Needed because {@link FolderAPI#copy} returns nothing. A folder present afterwards, absent
     * before, and named the source name followed by one or more {@code _copy} can only be this
     * run's duplicate. Anything else, none or more than one, is refused rather than guessed.
     *
     * @param before     names of the parent's child folders before the copy
     * @param after      the parent's child folders after the copy
     * @param sourceName the source folder's name
     * @return the duplicate, or empty when there is not exactly one candidate
     */
    static Optional<Folder> identifyDuplicate(final Set<String> before, final List<Folder> after,
            final String sourceName) {
        final Pattern duplicateName = Pattern.compile(
                Pattern.quote(sourceName) + "(" + Pattern.quote(COPY_SUFFIX) + ")+");
        final List<Folder> candidates = after.stream()
                .filter(folder -> !before.contains(folder.getName()))
                .filter(folder -> duplicateName.matcher(folder.getName()).matches())
                .toList();
        return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    /**
     * Copies, for the source and every folder under it, the contentlets the walk did not carry
     * into the matching folder of the duplicate. Folders are matched by name, since the walk keeps
     * every child folder's name.
     */
    private void copyWhatTheWalkLeftBehind(final Folder source, final Folder duplicate,
            final Host site, final User systemUser) throws Exception {

        final Set<String> carried = carriedByTheWalk(source, systemUser);
        for (final String identifier : contentletIdentifiersUnder(source, site)) {
            if (!carried.contains(identifier)) {
                final Contentlet contentlet = contentletAPI
                        .findContentletByIdentifierAnyLanguage(identifier, true);
                if (contentlet != null && UtilMethods.isSet(contentlet.getInode())) {
                    contentletAPI.copyContentlet(contentlet, duplicate, systemUser, false);
                }
            }
        }

        final Map<String, Folder> duplicateChildren = folderAPI
                .findSubFolders(duplicate, systemUser, false).stream()
                .collect(Collectors.toMap(Folder::getName, folder -> folder));
        for (final Folder child : folderAPI.findSubFolders(source, systemUser, false)) {
            final Folder duplicateChild = duplicateChildren.get(child.getName());
            if (duplicateChild == null) {
                throw new DotDataException("The copy of " + child.getPath() + " is missing");
            }
            copyWhatTheWalkLeftBehind(child, duplicateChild, site, systemUser);
        }
    }

    /**
     * The identifiers the walk copies from one folder, computed with the walk's own calls: file
     * assets that are working and not archived, and working pages.
     * <p>
     * <b>Deliberately the same calls, not an equivalent query.</b> The walk lists pages through the
     * search index, not the database, so a page saved but not yet indexed is skipped by it. A
     * database query would count that page as carried and it would be lost from the duplicate.
     * Asking the walk's own question is the only way every item lands exactly once.
     */
    private Set<String> carriedByTheWalk(final Folder folder, final User systemUser)
            throws Exception {
        final Set<String> carried = new HashSet<>();
        for (final FileAsset fileAsset : APILocator.getFileAssetAPI()
                .findFileAssetsByFolder(folder, systemUser, false)) {
            if (fileAsset.isWorking() && !fileAsset.isArchived()) {
                carried.add(fileAsset.getIdentifier());
            }
        }
        for (final IHTMLPage page : APILocator.getHTMLPageAssetAPI()
                .getWorkingHTMLPages(folder, systemUser, false)) {
            carried.add(page.getIdentifier());
        }
        return carried;
    }

    /**
     * Every contentlet directly under a folder, one identifier per item whatever its versions,
     * languages or archived state.
     * <p>
     * <b>Why the identifier table, and why not paged.</b> The plan named
     * {@code BrowserAPI.getContentUnderParentFromDB}, read a page at a time. That lookup answers
     * one row per language version and filters by the query's user, so its rows would have to be
     * collapsed back to identifiers before copying. One identifier is exactly the unit
     * {@link ContentletAPI#copyContentlet} works in, since it copies every version in every
     * language of the item it is given. Both reads come from the database, not the search index.
     * Paging is not needed: this reads only identifiers, a few dozen bytes each, and each
     * contentlet is loaded one at a time as it is copied, so what is held at once stays bounded
     * by one folder's identifiers, never the whole subtree.
     *
     * @param folder the source folder whose direct contents are read
     * @param site   the site the folder belongs to
     * @return the identifiers of the contentlets directly under the folder
     * @throws DotDataException the query failed
     */
    private List<String> contentletIdentifiersUnder(final Folder folder, final Host site)
            throws DotDataException {
        return new DotConnect()
                .setSQL("SELECT id FROM identifier WHERE host_inode = ? AND parent_path = ? "
                        + "AND asset_type = 'contentlet'")
                .addParam(site.getIdentifier())
                .addParam(folder.getPath())
                .loadObjectResults().stream()
                .map(row -> String.valueOf(row.get("id")))
                .toList();
    }

    /**
     * Where a folder's duplicate lands: the folder it sits in, or its site when it sits at the
     * root. What the author needs add-children rights on.
     *
     * @param source the folder to be duplicated
     * @return the parent folder, or the site
     * @throws Exception the parent could not be read
     */
    Permissionable parentOf(final Folder source) throws Exception {
        final User systemUser = APILocator.systemUser();
        final Folder parentFolder = parentFolderOf(source, systemUser);
        return parentFolder != null ? parentFolder
                : APILocator.getHostAPI().find(source.getHostId(), systemUser, false);
    }

    /** The folder the source sits in, or {@code null} when it sits at its site's root. */
    private Folder parentFolderOf(final Folder source, final User systemUser) throws Exception {
        final Folder parent = folderAPI.findParentFolder(source, systemUser, false);
        return parent == null || !UtilMethods.isSet(parent.getInode())
                || FolderAPI.SYSTEM_FOLDER.equals(parent.getInode()) ? null : parent;
    }

    private List<Folder> childrenOf(final Folder parentFolder, final Host site,
            final User systemUser) {
        try {
            return parentFolder == null
                    ? folderAPI.findSubFolders(site, systemUser, false)
                    : folderAPI.findSubFolders(parentFolder, systemUser, false);
        } catch (final Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    private static Set<String> childNames(final List<Folder> folders) {
        return folders.stream().map(Folder::getName).collect(Collectors.toSet());
    }
}
