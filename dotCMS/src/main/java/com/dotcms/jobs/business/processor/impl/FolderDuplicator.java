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
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * The complement reads the {@code identifier} table directly, for the source and for the
 * duplicate: one row per item whatever its versions, languages or archived state, which is the
 * database-first source for folder contents. Each item of the source whose name is absent from the
 * duplicate is copied with {@link ContentletAPI#copyContentlet}, which copies every
 * version and keeps each one's live and archived state. Relationships therefore point at the
 * originals, as the walk's own copies do. Menu links are not contentlets, so the walk's link copies
 * are kept and only an archived link's copy is archived to match.
 * <p>
 * {@code FolderAPIImpl} and {@code FolderFactoryImpl} are not modified.
 *
 * @author dotCMS
 */
final class FolderDuplicator {

    /** The suffix the walk appends, once or more, until the duplicate's name is free. */
    private static final String COPY_SUFFIX = "_copy";

    /** Keeps this lock apart from the other advisory locks keyed on an identifier. */
    private static final String DUPLICATE_LOCK_PREFIX = "folder-bulk-duplicate:";

    /** What {@link LinkFactory#copyLink} appends to a link's title when the title is taken. */
    private static final String LINK_COPY_SUFFIX = " (COPY) ";

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
            waitForOtherDuplicatesLandingIn(parentFolder != null
                    ? parentFolder.getIdentifier() : site.getIdentifier());

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
     * Holds, until this transaction ends, a lock on the place the duplicate lands, waiting first
     * for any other duplicate landing there to commit or roll back.
     * <p>
     * Without it two overlapping duplicates of one folder each miss the other's uncommitted copy,
     * choose the same free name, and the second fails on the identifier the name derives. Waiting
     * lets the second see the first's copy and choose the next name, so submitting the same folder
     * twice gives two duplicates however the runs overlap. The lock is Postgres's
     * transaction-scoped advisory lock, the kind bulk delete takes per site; a single-folder copy
     * made outside this class does not take it.
     *
     * @param parentIdentifier the identifier of the folder or site the duplicate lands in
     * @throws DotDataException the lock could not be taken
     */
    private static void waitForOtherDuplicatesLandingIn(final String parentIdentifier)
            throws DotDataException {
        // nosemgrep: gitlab.find_sec_bugs.CUSTOM_INJECTION-2 -- static SQL, the only runtime
        // value is bound via addParam
        new DotConnect().setSQL("SELECT pg_advisory_xact_lock(hashtext(?))")
                .addParam(DUPLICATE_LOCK_PREFIX + parentIdentifier)
                .loadObjectResults();
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

        archiveTheCopiesOfArchivedLinks(source, duplicate, systemUser);

        final Set<String> landed = contentletsUnder(duplicate, site).keySet();
        for (final Map.Entry<String, String> item : contentletsUnder(source, site).entrySet()) {
            if (!landed.contains(item.getKey())) {
                final Contentlet contentlet = contentletAPI
                        .findContentletByIdentifierAnyLanguage(item.getValue(), true);
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
     * Archives the copy of every archived menu link directly under the source.
     * <p>
     * The walk copies every working link, archived or not, and {@link LinkFactory#copyLink} keeps
     * a link's live state but not its archived one, so an archived link would reappear in the
     * duplicate as active. A link copy keeps no reference to its original, so each archived link is
     * paired with a copy by the fields the copy carries over. Links that match on all of them are
     * interchangeable, so which of them is archived makes no difference.
     *
     * @throws DotDataException an archived link has no copy to archive
     */
    private void archiveTheCopiesOfArchivedLinks(final Folder source, final Folder duplicate,
            final User systemUser) throws Exception {
        final List<Link> archived = folderAPI.getLinks(source, true, true, systemUser, false);
        if (archived.isEmpty()) {
            return;
        }
        final List<Link> unpaired = new ArrayList<>(
                folderAPI.getLinks(duplicate, true, false, systemUser, false));
        for (final Link link : archived) {
            final Link copy = unpaired.stream()
                    .filter(candidate -> sameLink(link, candidate))
                    .findFirst()
                    .orElseThrow(() -> new DotDataException(
                            "The copy of the archived link " + link.getTitle() + " in "
                                    + source.getPath() + " is missing"));
            unpaired.remove(copy);
            APILocator.getVersionableAPI().setDeleted(copy, true);
        }
    }

    /** Whether a copy carries the original's fields, allowing for the title's copy suffix. */
    private static boolean sameLink(final Link original, final Link copy) {
        final String title = copy.getTitle();
        final String copyTitle = title != null && title.endsWith(LINK_COPY_SUFFIX)
                ? title.substring(0, title.length() - LINK_COPY_SUFFIX.length())
                : title;
        return Objects.equals(original.getTitle(), copyTitle)
                && Objects.equals(original.getUrl(), copy.getUrl())
                && Objects.equals(original.getProtocal(), copy.getProtocal())
                && Objects.equals(original.getLinkType(), copy.getLinkType())
                && Objects.equals(original.getTarget(), copy.getTarget());
    }

    /**
     * Every contentlet directly under a folder, as its name in the folder mapped to its
     * identifier: one entry per item whatever its versions, languages or archived state.
     * <p>
     * <b>Compared by name against what actually landed.</b> Whatever the walk copied sits in the
     * duplicate under its original's name, so an item of the source whose name is absent from the
     * duplicate is exactly what the walk left behind. Reading what landed, rather than asking the
     * walk's own questions again, keeps the second pass off the search index: the walk lists pages
     * through it, so a page saved but not yet indexed is skipped by the walk, and it is copied here
     * because its name is missing from the duplicate. Items named after their identifier, such as
     * generic content, never share a name with a copy, and the walk never carries them anyway.
     * <p>
     * <b>Why the identifier table, and why not paged.</b> One identifier is exactly the unit
     * {@link ContentletAPI#copyContentlet} works in, since it copies every version in every
     * language of the item it is given, and the table holds one row per item. Paging is not
     * needed: this reads only names and identifiers, a few dozen bytes each, and each contentlet
     * is loaded one at a time as it is copied, so what is held at once stays bounded by one
     * folder's items, never the whole subtree. Two such queries per folder are the whole cost of
     * deciding what to copy.
     *
     * @param folder the folder whose direct contents are read
     * @param site   the site the folder belongs to
     * @return each contentlet's name in the folder, mapped to its identifier
     * @throws DotDataException the query failed
     */
    private Map<String, String> contentletsUnder(final Folder folder, final Host site)
            throws DotDataException {
        return new DotConnect()
                .setSQL("SELECT id, asset_name FROM identifier WHERE host_inode = ? "
                        + "AND parent_path = ? AND asset_type = 'contentlet'")
                .addParam(site.getIdentifier())
                .addParam(folder.getPath())
                .loadObjectResults().stream()
                .collect(Collectors.toMap(row -> String.valueOf(row.get("asset_name")),
                        row -> String.valueOf(row.get("id"))));
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
