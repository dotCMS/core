import { signalStore, signalStoreFeature, type, withComputed, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { computed } from '@angular/core';

import {
    DotEventsSocket,
    DotFolderBulkDeleteService,
    DotHttpErrorManagerService,
    DotSystemEventType
} from '@dotcms/data-access';
import {
    DotContentDriveItem,
    DotFolderDeleteActiveRun,
    DotFolderDeleteAnnouncementEvent
} from '@dotcms/dotcms-models';
import { SYSTEM_HOST_ID } from '@dotcms/ui';

import { withFolderDeleteRuns } from './with-folder-delete-runs';

import { SYSTEM_HOST_PATH } from '../../../shared/constants';
import { buildContentDriveState } from '../../../shared/content-drive-state.fixture';
import { DotContentDriveState, DotContentDriveStatus } from '../../../shared/models';

/**
 * Server-derived in-flight folders (#37063 US3).
 *
 * This is the correctness core of the feature: without it, a reload presents a folder that is being
 * destroyed as an ordinary one — openable, uploadable, a valid drop target. A measured delete of a
 * 20,000-file folder took about eight minutes, so the window is long enough that an author will
 * reload, open a second tab, or hand the screen to a colleague inside it.
 */
describe('withFolderDeleteRuns', () => {
    const HOSTNAME = 'demo.dotcms.com';

    /**
     * **The same two folders, in the three spellings this feature has to reconcile.**
     *
     * This is the trap the feature shipped into: a run names its folders the way the *server* does,
     * site-qualified and with a trailing slash, while a *listing row* carries a bare path from the
     * site root. An earlier version of this spec gave the rows the server's spelling, so both sides
     * of the comparison matched here and neither did in the browser — the suite was green while no
     * folder was ever marked for anyone.
     *
     * Keep these distinct. A row fixture that starts with `//` means this spec has stopped testing
     * the thing that broke.
     */
    const PATH_A = '//demo.dotcms.com/old-a/';
    const PATH_B = '//demo.dotcms.com/old-b/';

    /** As a listing row carries them. */
    const ROW_A = '/old-a/';
    const ROW_B = '/old-b/';

    /** As the feature stores and compares them: lower-cased, no trailing slash. */
    const REF_A = '//demo.dotcms.com/old-a';
    const REF_B = '//demo.dotcms.com/old-b';

    /** A folder row as the listing holds it — both keys, because neither is reliably present. */
    const folderRow = (path: string, inode: string, identifier: string) =>
        ({
            type: 'folder',
            path,
            inode,
            identifier
        }) as unknown as DotContentDriveItem;

    const initialState = buildContentDriveState({
        // The browsed site, because that is what turns a row's bare path into the ref a run names.
        currentSite: { aliases: '', archived: false, hostname: HOSTNAME, identifier: 'site-id' },
        items: [folderRow(ROW_A, 'inode-a', 'id-a'), folderRow(ROW_B, 'inode-b', 'id-b')],
        status: DotContentDriveStatus.LOADED
    });

    /**
     * `withFolderDeleteRuns` declares `busyRows` as a required input — it belongs to
     * `withActionExecution`, which the real store installs first. Standing in for it here keeps this
     * spec about the server-derived half without dragging in the whole execution feature.
     *
     * Declared through `signalStoreFeature` with an explicit input rather than inline in the
     * `signalStore` call: a bare `withComputed` leaves its own input generic to be inferred from
     * position, and in a chain that long TypeScript gives up and widens the store to `object`,
     * which surfaces as an unreadable "no overload matches" on the `signalStore` call itself.
     */
    const withBusyRowsStub = () =>
        signalStoreFeature(
            { state: type<DotContentDriveState>() },
            withComputed(() => ({ busyRows: computed<string[]>(() => []) }))
        );

    const store = signalStore(
        { providedIn: 'root' },
        withState(initialState),
        withBusyRowsStub(),
        withFolderDeleteRuns()
    );

    let spectator: SpectatorService<InstanceType<typeof store>>;
    let instance: InstanceType<typeof store>;

    const readActiveRuns = vi.fn();
    const handle = vi.fn();

    /**
     * The two announcement streams, kept apart.
     *
     * A single stream for both would make a *start* announcement reach the *finish* handler too,
     * and the feature would add a folder and immediately drop it again — a green test proving the
     * opposite of what it reads as.
     */
    let startedEvents: Subject<DotFolderDeleteAnnouncementEvent>;
    let finishedEvents: Subject<DotFolderDeleteAnnouncementEvent>;

    const createService = createServiceFactory({
        service: store,
        providers: [
            mockProvider(DotFolderBulkDeleteService, { readActiveRuns }),
            mockProvider(DotHttpErrorManagerService, { handle }),
            mockProvider(DotEventsSocket, {
                on: (type: DotSystemEventType) =>
                    type === DotSystemEventType.FOLDER_DELETE_STARTED
                        ? startedEvents.asObservable()
                        : finishedEvents.asObservable()
            })
        ]
    });

    const run = (id: string, paths: string[]): DotFolderDeleteActiveRun => ({
        id,
        state: 'RUNNING',
        paths
    });

    const build = (active: DotFolderDeleteActiveRun[] = []) => {
        startedEvents = new Subject<DotFolderDeleteAnnouncementEvent>();
        finishedEvents = new Subject<DotFolderDeleteAnnouncementEvent>();
        readActiveRuns.mockReturnValue(of(active));
        spectator = createService();
        instance = spectator.service;
        instance.establishInFlightFolders();
    };

    beforeEach(() => {
        readActiveRuns.mockReset();
        handle.mockReset();
    });

    describe('establishing the set on load', () => {
        it('should mark the folders every in-flight run is working on', () => {
            build([run('r1', [PATH_A])]);

            expect(instance.inFlightFolderPaths()).toEqual([REF_A]);
        });

        it('should cover runs started by ANY author, not only this client', () => {
            // Nothing here distinguishes whose run it is, and that is the point: a folder someone
            // else is deleting must not be presented as usable either (FR-020).
            build([run('someone-elses', [PATH_A]), run('mine', [PATH_B])]);

            expect(instance.inFlightFolderPaths().sort()).toEqual([REF_A, REF_B].sort());
        });

        it('should mark nothing when no run is in flight', () => {
            build([]);

            expect(instance.inFlightFolderPaths()).toEqual([]);
        });

        it('should record that the read has answered', () => {
            build([]);

            expect(instance.folderDeleteRunsEstablished()).toBe(true);
        });

        it('should degrade to nothing marked when the read fails', () => {
            // The listing still renders and stays usable, and the author sees no error about an
            // action they never took (FR-022, SC-010).
            startedEvents = new Subject<DotFolderDeleteAnnouncementEvent>();
            finishedEvents = new Subject<DotFolderDeleteAnnouncementEvent>();
            readActiveRuns.mockReturnValue(throwError(() => new Error('boom')));
            spectator = createService();
            instance = spectator.service;

            instance.establishInFlightFolders();

            expect(instance.inFlightFolderPaths()).toEqual([]);
            expect(handle).not.toHaveBeenCalled();
        });

        it('should re-establish from the server rather than inherit what it was told', () => {
            // A run whose process dies never announces that it ended. A client listening only to
            // announcements would mark that folder indefinitely with nothing to correct it, so the
            // load-time read is what recovers it (FR-020b, CR-12).
            build([run('r1', [PATH_A])]);
            expect(instance.inFlightFolderPaths()).toEqual([REF_A]);

            readActiveRuns.mockReturnValue(of([]));
            instance.establishInFlightFolders();

            expect(instance.inFlightFolderPaths()).toEqual([]);
        });
    });

    describe('keeping the set current from announcements', () => {
        it('should mark a folder another author starts deleting, without a reload', () => {
            build([]);

            startedEvents.next({ jobId: 'later', path: PATH_A });

            expect(instance.inFlightFolderPaths()).toEqual([REF_A]);
        });

        it('should clear a folder when its run announces that it finished', () => {
            build([run('r1', [PATH_A])]);

            finishedEvents.next({ jobId: 'r1', path: PATH_A });

            expect(instance.inFlightFolderPaths()).toEqual([]);
        });

        it('should clear a folder whether the delete SUCCEEDED or FAILED', () => {
            // A failed delete leaves the folder intact and usable. Marking it until the framework
            // moves the run on would be indistinguishable, to the author, from a folder nobody can
            // touch (FR-021).
            build([run('r1', [PATH_A, PATH_B])]);

            instance.clearInFlightFolder({ jobId: 'r1', path: PATH_A });

            expect(instance.inFlightFolderPaths()).toEqual([REF_B]);
        });

        it('should let one run end without clearing another run’s folders', () => {
            build([run('r1', [PATH_A]), run('r2', [PATH_B])]);

            instance.clearRun('r1');

            expect(instance.inFlightFolderPaths()).toEqual([REF_B]);
        });

        it('should not double-count a folder announced twice', () => {
            build([run('r1', [PATH_A])]);

            startedEvents.next({ jobId: 'r1', path: PATH_A });

            expect(instance.inFlightFolderPaths()).toEqual([REF_A]);
        });
    });

    describe('resolving paths to row keys', () => {
        it('should mark BOTH the inode and the identifier of a listed folder', () => {
            // The listing marks by a key that is not reliably one field — the search service only
            // backfills `inode` from `identifier` when the API returned none.
            build([run('r1', [PATH_A])]);

            expect(instance.inFlightFolderKeys().sort()).toEqual(['id-a', 'inode-a']);
        });

        it('should ignore a folder the listing is not currently showing', () => {
            // Nothing is rendering it, so there is nothing to mark. Resolution is bounded by the
            // page rather than by the size of the in-flight set.
            build([run('r1', ['//demo.dotcms.com/elsewhere/'])]);

            expect(instance.inFlightFolderKeys()).toEqual([]);
        });

        it('should resolve every listed folder a run covers', () => {
            build([run('r1', [PATH_A, PATH_B])]);

            expect(instance.inFlightFolderKeys().sort()).toEqual([
                'id-a',
                'id-b',
                'inode-a',
                'inode-b'
            ]);
        });

        it('should match a run whose spelling differs only in case or a trailing slash', () => {
            // dotCMS resolves asset paths through a unique index over the lower-cased full path per
            // host, so these two strings name ONE folder. Comparing them literally would mark it in
            // one session and not in another depending on how the path was typed.
            build([run('r1', ['//DEMO.dotcms.com/Old-A'])]);

            expect(instance.inFlightFolderKeys().sort()).toEqual(['id-a', 'inode-a']);
        });

        it('should not match a same-named folder on a different site', () => {
            // The bare row path `/old-a/` exists on every site. Resolving it without the browsed
            // site's hostname would mark this site's folder because another site's is being
            // deleted — a folder presented as doomed that nothing is touching.
            build([run('r1', ['//other.dotcms.com/old-a/'])]);

            expect(instance.inFlightFolderKeys()).toEqual([]);
        });
    });

    describe('resolving rows while browsing System Host', () => {
        /**
         * System Host belongs to no site, so its rows cannot be qualified with whatever hostname
         * the site switcher happens to show — that produced `//demo.dotcms.comSYSTEM_HOST` in an
         * earlier version of the sibling helper, a reference to nothing.
         */
        const systemHostState = buildContentDriveState({
            currentSite: {
                aliases: '',
                archived: false,
                hostname: HOSTNAME,
                identifier: 'site-id'
            },
            path: SYSTEM_HOST_PATH,
            items: [folderRow(ROW_A, 'inode-a', 'id-a')],
            status: DotContentDriveStatus.LOADED
        });

        const systemHostStore = signalStore(
            { providedIn: 'root' },
            withState(systemHostState),
            withBusyRowsStub(),
            withFolderDeleteRuns()
        );

        const createSystemHostService = createServiceFactory({
            service: systemHostStore,
            providers: [
                mockProvider(DotFolderBulkDeleteService, { readActiveRuns }),
                mockProvider(DotHttpErrorManagerService, { handle }),
                mockProvider(DotEventsSocket, {
                    on: () => new Subject<DotFolderDeleteAnnouncementEvent>().asObservable()
                })
            ]
        });

        it("should resolve rows against System Host, not the switcher's site", () => {
            readActiveRuns.mockReturnValue(of([run('r1', [`//${SYSTEM_HOST_ID}${ROW_A}`])]));
            const systemHostInstance = createSystemHostService().service;
            systemHostInstance.establishInFlightFolders();

            expect(systemHostInstance.inFlightFolderKeys().sort()).toEqual(['id-a', 'inode-a']);
        });
    });
});
