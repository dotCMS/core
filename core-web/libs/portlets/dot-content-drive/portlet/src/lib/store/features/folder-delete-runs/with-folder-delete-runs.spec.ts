import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
    DotEventsSocket,
    DotFolderBulkDeleteService,
    DotHttpErrorManagerService,
    DotSystemEventType
} from '@dotcms/data-access';
import { DotFolderDeleteActiveRun, DotFolderDeleteAnnouncementEvent } from '@dotcms/dotcms-models';

import { withFolderDeleteRuns } from './with-folder-delete-runs';

import {
    DEFAULT_PAGE,
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED
} from '../../../shared/constants';
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
    const PATH_A = '//demo.dotcms.com/old-a/';
    const PATH_B = '//demo.dotcms.com/old-b/';

    /** A folder row as the listing holds it — both keys, because neither is reliably present. */
    const folderRow = (path: string, inode: string, identifier: string) =>
        ({
            type: 'folder',
            path,
            inode,
            identifier
        }) as unknown as DotContentDriveState['items'][0];

    const initialState: DotContentDriveState = {
        currentSite: undefined,
        path: DEFAULT_PATH,
        filters: {},
        items: [folderRow(PATH_A, 'inode-a', 'id-a'), folderRow(PATH_B, 'inode-b', 'id-b')],
        status: DotContentDriveStatus.LOADED,
        pagination: DEFAULT_PAGINATION,
        page: DEFAULT_PAGE,
        sort: DEFAULT_SORT,
        isTreeExpanded: DEFAULT_TREE_EXPANDED
    } as DotContentDriveState;

    const store = signalStore(
        { providedIn: 'root' },
        withState(initialState),
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
        parameters: { assetPaths: paths }
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

            expect(instance.inFlightFolderPaths()).toEqual([PATH_A]);
        });

        it('should cover runs started by ANY author, not only this client', () => {
            // Nothing here distinguishes whose run it is, and that is the point: a folder someone
            // else is deleting must not be presented as usable either (FR-020).
            build([run('someone-elses', [PATH_A]), run('mine', [PATH_B])]);

            expect(instance.inFlightFolderPaths().sort()).toEqual([PATH_A, PATH_B].sort());
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
            expect(instance.inFlightFolderPaths()).toEqual([PATH_A]);

            readActiveRuns.mockReturnValue(of([]));
            instance.establishInFlightFolders();

            expect(instance.inFlightFolderPaths()).toEqual([]);
        });
    });

    describe('keeping the set current from announcements', () => {
        it('should mark a folder another author starts deleting, without a reload', () => {
            build([]);

            startedEvents.next({ jobId: 'later', path: PATH_A });

            expect(instance.inFlightFolderPaths()).toEqual([PATH_A]);
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

            expect(instance.inFlightFolderPaths()).toEqual([PATH_B]);
        });

        it('should let one run end without clearing another run’s folders', () => {
            build([run('r1', [PATH_A]), run('r2', [PATH_B])]);

            instance.clearRun('r1');

            expect(instance.inFlightFolderPaths()).toEqual([PATH_B]);
        });

        it('should not double-count a folder announced twice', () => {
            build([run('r1', [PATH_A])]);

            startedEvents.next({ jobId: 'r1', path: PATH_A });

            expect(instance.inFlightFolderPaths()).toEqual([PATH_A]);
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
    });
});
