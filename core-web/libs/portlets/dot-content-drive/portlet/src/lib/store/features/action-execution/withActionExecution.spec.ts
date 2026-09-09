import { describe, expect, it } from '@jest/globals';
import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import {
    AddToBundleService,
    DotBulkRefreshService,
    DotEventsSocket,
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import { DotBulkRefreshCompletedEvent, DotBulkUploadCompletedEvent } from '@dotcms/dotcms-models';

import { withActionExecution } from './withActionExecution';

import {
    DEFAULT_PAGE,
    DEFAULT_PAGINATION,
    DEFAULT_PATH,
    DEFAULT_SORT,
    DEFAULT_TREE_EXPANDED
} from '../../../shared/constants';
import { DotContentDriveState, DotContentDriveStatus } from '../../../shared/models';

/**
 * Characterisation spec — describes what this feature does **today**.
 *
 * Written before the run registry is reworked (#37166 Phases 4–5, tasks T037/T055): the single
 * `actionExecution` slot becomes a keyed collection so an upload cannot block a selection action.
 * That rework touches every method here, and this file had no test of any kind, so these tests
 * exist to define what must survive it rather than to describe a desired design.
 *
 * Two things are deliberately pinned even though they look incidental, because they are the
 * behaviours most likely to be lost in a rewrite:
 *
 * 1. **Counts come from the response, never from the number of items submitted.** Every endpoint
 *    here answers 200 with per-item failures inside, so substituting `inodes.length` would turn a
 *    refusal into a reported success.
 * 2. **Refresh deliberately does not set `actionExecution`.** It reads like an oversight and is not:
 *    the field drives an "Applying …" indicator and locks the Action Center, and neither fits a job
 *    that runs for minutes. A rework that "fixes" this by treating every action alike would
 *    regress it.
 */

/**
 * Mirrors the store's own initial state, using the same shared constants rather than literals.
 *
 * Deliberate: the sibling feature specs hand-rolled this and have since gone stale — they still
 * carry a `totalItems` field the state no longer has and omit `pagination.page`, which it now
 * requires. Jest transpiles without typechecking so they stay green while being wrong. Sourcing
 * the defaults from `shared/constants` keeps this file honest for longer.
 */
const initialState: DotContentDriveState = {
    currentSite: undefined,
    path: DEFAULT_PATH,
    filters: {},
    items: [],
    selectedItems: [],
    status: DotContentDriveStatus.LOADING,
    pagination: DEFAULT_PAGINATION,
    sort: DEFAULT_SORT,
    isTreeExpanded: DEFAULT_TREE_EXPANDED,
    isTreeForceCollapsed: false,
    pages: [DEFAULT_PAGE],
    userSearchableFields: [],
    userSearchableActive: [],
    userSearchableFieldsLoaded: false,
    showInListFields: [],
    languages: [],
    defaultLanguageId: undefined,
    defaultLanguageLoaded: false,
    currentUserIsAdmin: false
};

const actionExecutionStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withActionExecution()
);

describe('withActionExecution', () => {
    let spectator: SpectatorService<InstanceType<typeof actionExecutionStoreMock>>;
    let store: InstanceType<typeof actionExecutionStoreMock>;

    const fireDefaultAction = jest.fn();
    const bulkFire = jest.fn();
    const addToBundle = jest.fn();
    const pushPublishAssets = jest.fn();
    const refresh = jest.fn();
    const handle = jest.fn();

    /** Lets a test push a completion event onto the socket the feature subscribes to on init. */
    let socketEvents: Subject<DotBulkRefreshCompletedEvent>;

    const createService = createServiceFactory({
        service: actionExecutionStoreMock,
        providers: [
            mockProvider(DotWorkflowActionsFireService, { fireDefaultAction, bulkFire }),
            mockProvider(AddToBundleService, { addToBundle }),
            mockProvider(PushPublishService, { pushPublishAssets }),
            mockProvider(DotBulkRefreshService, { refresh }),
            mockProvider(DotHttpErrorManagerService, { handle }),
            mockProvider(DotMessageService, { get: (key: string) => key }),
            mockProvider(DotEventsSocket, { on: () => socketEvents.asObservable() })
        ]
    });

    const build = () => {
        socketEvents = new Subject<DotBulkRefreshCompletedEvent>();
        spectator = createService();
        store = spectator.service;
    };

    beforeEach(() => {
        fireDefaultAction.mockReset();
        bulkFire.mockReset();
        addToBundle.mockReset();
        pushPublishAssets.mockReset();
        refresh.mockReset();
        handle.mockReset();
    });

    describe('initial state', () => {
        it('should start with nothing running and nothing to report', () => {
            build();

            expect(store.actionExecution()).toBeUndefined();
            expect(store.actionExecutionResult()).toBeUndefined();
        });
    });

    describe('executeQuickAction', () => {
        it('should mark the run in flight with the action name and the item count', () => {
            build();
            // Never settles, so the in-flight state stays observable.
            fireDefaultAction.mockReturnValue(new Subject());

            store.executeQuickAction('lock-id', 'Lock', ['inode-1', 'inode-2']);

            // `objectContaining`: a run now also carries its id, operation and targets. The two
            // fields the indicator reads are what this pins.
            expect(store.actionExecution()).toEqual(
                expect.objectContaining({ actionName: 'Lock', total: 2 })
            );
        });

        it('should report the counts the server returned, not the number of inodes submitted', () => {
            build();
            // Two submitted, one refused server-side: the distinction this assertion protects.
            fireDefaultAction.mockReturnValue(of({ summary: { successCount: 1, failCount: 1 } }));

            store.executeQuickAction('lock-id', 'Lock', ['inode-1', 'inode-2']);

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({
                    actionName: 'Lock',
                    successCount: 1,
                    skippedCount: 0,
                    failedCount: 1
                })
            );
        });

        it('should clear the in-flight marker once the run settles', () => {
            build();
            fireDefaultAction.mockReturnValue(of({ summary: { successCount: 2, failCount: 0 } }));

            store.executeQuickAction('lock-id', 'Lock', ['inode-1', 'inode-2']);

            expect(store.actionExecution()).toBeUndefined();
        });

        it('should refuse the same action fired again over the same item', () => {
            build();
            fireDefaultAction.mockReturnValue(new Subject());

            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);
            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);

            expect(fireDefaultAction).toHaveBeenCalledTimes(1);
            expect(store.actionExecution()).toEqual(
                expect.objectContaining({ actionName: 'Lock', total: 1 })
            );
        });

        it('should allow a different action while one is in flight', () => {
            // **Deliberate change.** The guard used to be global, so any run blocked every other.
            // That was tolerable when actions took a second and unacceptable once one of them is an
            // upload running for minutes (FR-015). It is now scoped to this operation over these
            // items (FR-016).
            build();
            fireDefaultAction.mockReturnValue(new Subject());

            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);
            store.executeQuickAction('unlock-id', 'Unlock', ['inode-2']);

            expect(fireDefaultAction).toHaveBeenCalledTimes(2);
            expect(store.activeRunCount()).toBe(2);
        });

        it('should name no single run while several are in flight', () => {
            // Naming one of several arbitrarily would be worse than naming none; the indicator
            // falls back to a count (FR-017).
            build();
            fireDefaultAction.mockReturnValue(new Subject());

            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);
            store.executeQuickAction('unlock-id', 'Unlock', ['inode-2']);

            expect(store.actionExecution()).toBeUndefined();
        });

        it('should keep a run over rows off the toolbar', () => {
            // The rows dim in front of the author, so a toolbar line saying the same thing is the
            // duplication this set out to remove. `actionExecution` still reports it, because the
            // Action Center reads that to gate itself — only the *presentation* signal is narrowed.
            build();
            fireDefaultAction.mockReturnValue(new Subject());

            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);

            expect(store.actionExecution()).toBeDefined();
            expect(store.toolbarRun()).toBeUndefined();
            expect(store.toolbarRunCount()).toBe(0);
        });

        it('should expose the items every in-flight run is touching', () => {
            build();
            fireDefaultAction.mockReturnValue(new Subject());

            store.executeQuickAction('lock-id', 'Lock', ['inode-1', 'inode-2']);
            store.executeQuickAction('unlock-id', 'Unlock', ['inode-3']);

            expect(store.busyRows()).toEqual(['inode-1', 'inode-2', 'inode-3']);
        });

        it('should refuse a run that overlaps items another run is already touching', () => {
            // The key is now `operation:targets`, so Publish on [a,b] and Publish on [a] are
            // different keys. A key match alone would let the second one through and act on `a`
            // twice at once — the exact thing the guard exists to stop. Busy rows are
            // non-interactive in the UI, but a disabled control is an affordance, not a lock.
            build();
            fireDefaultAction.mockReturnValue(new Subject());

            store.executeQuickAction('lock-id', 'Lock', ['inode-1', 'inode-2']);
            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);

            expect(fireDefaultAction).toHaveBeenCalledTimes(1);
        });

        it('should do nothing when there are no inodes', () => {
            build();

            store.executeQuickAction('lock-id', 'Lock', []);

            expect(fireDefaultAction).not.toHaveBeenCalled();
            expect(store.actionExecution()).toBeUndefined();
        });

        it('should route a transport failure to the error handler and clear the marker', () => {
            build();
            fireDefaultAction.mockReturnValue(throwError(() => new Error('boom')));

            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);

            expect(handle).toHaveBeenCalled();
            expect(store.actionExecution()).toBeUndefined();
            expect(store.actionExecutionResult()).toBeUndefined();
        });

        it('should treat a 200 carrying no summary as an error rather than a success', () => {
            build();
            // Reachable: the endpoint streams results then summary, and the writer swallows a
            // mid-stream IOException. There is no honest count to report, so none is invented.
            fireDefaultAction.mockReturnValue(of({}));

            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);

            expect(handle).toHaveBeenCalled();
            expect(store.actionExecutionResult()).toBeUndefined();
        });
    });

    describe('executeWorkflowAction', () => {
        it('should carry skips through to the result, distinct from failures', () => {
            build();
            // A mixed-type selection skips items whose scheme does not own the action.
            bulkFire.mockReturnValue(
                of({ successCount: 3, skippedCount: 2, fails: [{ inode: 'x' }] })
            );

            store.executeWorkflowAction('wf-1', 'Publish', ['a', 'b', 'c', 'd', 'e', 'f']);

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({
                    actionName: 'Publish',
                    successCount: 3,
                    skippedCount: 2,
                    failedCount: 1
                })
            );
        });

        it('should refuse the same workflow action fired again over the same items', () => {
            build();
            bulkFire.mockReturnValue(new Subject());

            store.executeWorkflowAction('wf-1', 'Publish', ['a']);
            store.executeWorkflowAction('wf-1', 'Publish', ['a']);

            expect(bulkFire).toHaveBeenCalledTimes(1);
        });
    });

    describe('executeAddToBundle', () => {
        it('should report the server total minus its error count, not the identifiers submitted', () => {
            build();
            addToBundle.mockReturnValue(of({ total: 5, errors: 2 }));

            store.executeAddToBundle('Add to Bundle', { id: 'b1', name: 'B1' }, ['i1', 'i2', 'i3']);

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({
                    actionName: 'Add to Bundle',
                    successCount: 3,
                    skippedCount: 0,
                    failedCount: 2
                })
            );
        });

        it('should ask for its success to be confirmed', () => {
            // Bundling changes nothing in the listing, so the "silent on success" rule would leave
            // the author with no sign at all - the same complaint that started this. Push Publish
            // shares this path for the same reason.
            build();
            addToBundle.mockReturnValue(of({ total: 2, errors: 0 }));

            store.executeAddToBundle('Add to Bundle', { id: 'b1', name: 'B1' }, ['i1', 'i2']);

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({ confirmSuccess: true })
            );
        });

        it('should treat a non-numeric errors field as a failure, not a success', () => {
            build();
            // The servlet answers 200 for its own failures, writing {"errors": "<message>"} with
            // no total. Reported as a success it would render NaN.
            addToBundle.mockReturnValue(of({ errors: 'DotPublisherException' }));

            store.executeAddToBundle('Add to Bundle', { id: 'b1', name: 'B1' }, ['i1']);

            expect(handle).toHaveBeenCalled();
            expect(store.actionExecutionResult()).toBeUndefined();
            expect(store.actionExecution()).toBeUndefined();
        });
    });

    describe('executeRefresh', () => {
        it('should NOT mark the run in flight — deliberate, see the file comment', () => {
            build();
            refresh.mockReturnValue(of({ jobId: 'job-1' }));

            store.executeRefresh('Refresh', ['inode-1']);

            expect(store.actionExecution()).toBeUndefined();
        });

        it('should not be blocked by another action already running', () => {
            build();
            fireDefaultAction.mockReturnValue(new Subject());
            refresh.mockReturnValue(of({ jobId: 'job-1' }));

            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);
            store.executeRefresh('Refresh', ['inode-2']);

            expect(refresh).toHaveBeenCalledTimes(1);
        });

        it('should report a submission failure, since no completion event is coming', () => {
            build();
            refresh.mockReturnValue(throwError(() => new Error('boom')));

            store.executeRefresh('Refresh', ['inode-1']);

            expect(handle).toHaveBeenCalled();
        });
    });

    describe('reportUploadCompleted', () => {
        const completed = (
            overrides: Partial<DotBulkUploadCompletedEvent> = {}
        ): DotBulkUploadCompletedEvent =>
            ({
                jobId: 'upload-1',
                state: 'SUCCESS',
                total: 3,
                processed: 3,
                successCount: 3,
                failedCount: 0,
                skippedCount: 0,
                ...overrides
            }) as DotBulkUploadCompletedEvent;

        it('should publish a result for a batch this store submitted', () => {
            build();
            store.trackUploadJob('upload-1');

            store.reportUploadCompleted('Upload', completed());

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({
                    actionName: 'Upload',
                    successCount: 3,
                    // Arrives unprompted, minutes after the author moved on, so it must announce
                    // itself and must not disturb whatever they are doing.
                    backgrounded: true
                })
            );
        });

        it('should ignore a batch it never submitted', () => {
            // The event is scoped to the user, not the tab, so another window's upload reaches this
            // store too. Reporting it would toast counts for files this grid never sent.
            build();
            store.trackUploadJob('upload-1');

            store.reportUploadCompleted('Upload', completed({ jobId: 'someone-elses' }));

            expect(store.actionExecutionResult()).toBeUndefined();
        });

        it.each([['FAILED_PERMANENTLY'], ['ABANDONED_PERMANENTLY']])(
            'should report a %s run as an error even when its counters add up',
            (state) => {
                // A run that gave up still records the counters it reached, and those can close
                // over `total` perfectly well. Publishing them would tell the author their batch
                // finished — the counts cannot distinguish "done" from "gave up", only the state
                // can.
                build();
                store.trackUploadJob('upload-1');

                store.reportUploadCompleted('Upload', completed({ state } as never));

                expect(store.actionExecutionResult()).toBeUndefined();
                expect(handle).toHaveBeenCalled();
            }
        );

        it('should publish a cancelled run, which is an outcome rather than a failure', () => {
            // Cancelling is a thing the author did, and the counts then say how far it got before
            // they stopped it — including the files it never reached.
            build();
            store.trackUploadJob('upload-1');

            store.reportUploadCompleted(
                'Upload',
                completed({
                    state: 'CANCELED',
                    total: 3,
                    successCount: 1,
                    failedCount: 0,
                    skippedCount: 2
                })
            );

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({ successCount: 1, skippedCount: 2 })
            );
        });

        it('should report an unusable outcome as an error rather than a clean run', () => {
            // A finished run carrying no counters is indistinguishable from a run over nothing if
            // the zeros are trusted, so the absence is treated as the failure it is.
            build();
            store.trackUploadJob('upload-1');

            store.reportUploadCompleted('Upload', {
                jobId: 'upload-1',
                state: 'SUCCESS'
            } as DotBulkUploadCompletedEvent);

            expect(store.actionExecutionResult()).toBeUndefined();
            expect(handle).toHaveBeenCalled();
        });

        it('should report counts that do not account for every file as an error', () => {
            build();
            store.trackUploadJob('upload-1');

            store.reportUploadCompleted(
                'Upload',
                completed({ total: 5, successCount: 1, failedCount: 0, skippedCount: 0 })
            );

            expect(store.actionExecutionResult()).toBeUndefined();
            expect(handle).toHaveBeenCalled();
        });

        it('should report a recognised resubmission as already uploaded, not as total failure', () => {
            // The retry promise. Under the collision branch a retry that worked collides on every
            // file, so the counts read as "50 of 50 failed — already in this folder". Only the flag
            // tells that apart from a batch whose files genuinely all collided, and without it the
            // author deletes and re-uploads files that were already there.
            build();
            store.trackUploadJob('upload-1');

            store.reportUploadCompleted(
                'Upload',
                completed({
                    total: 3,
                    processed: 3,
                    successCount: 0,
                    failedCount: 3,
                    skippedCount: 0,
                    duplicateSubmission: true
                })
            );

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({ duplicateSubmission: true })
            );
        });

        it('should not flag an ordinary all-collided batch as a resubmission', () => {
            // Same counts, different cause: these files really were already there, and the author
            // does need to deal with them.
            build();
            store.trackUploadJob('upload-1');

            store.reportUploadCompleted(
                'Upload',
                completed({
                    total: 3,
                    processed: 3,
                    successCount: 0,
                    failedCount: 3,
                    skippedCount: 0
                })
            );

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({ duplicateSubmission: undefined })
            );
        });

        it('should carry the per-file failures so the author learns which files and why', () => {
            // Counts alone tell an author three files failed and nothing they can act on. The names
            // and reasons are the whole point of a partial outcome.
            build();
            store.trackUploadJob('upload-1');

            const results = [
                { key: 'a.png', status: 'SUCCESS' as const },
                {
                    key: 'huge.mov',
                    status: 'FAILED' as const,
                    reason: 'OVER_SIZE_LIMIT' as const
                }
            ];

            store.reportUploadCompleted(
                'Upload',
                completed({ total: 2, successCount: 1, failedCount: 1, skippedCount: 0, results })
            );

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({ failures: results })
            );
        });

        it('should carry the destination so the listing only reloads where it can show it', () => {
            build();
            store.trackUploadJob('upload-1', ['//demo.com/images']);

            store.reportUploadCompleted('Upload', completed());

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({ affectedFolders: ['//demo.com/images'] })
            );
        });

        it('should settle a batch only once', () => {
            // A resumed run must not report twice, and the same event can be delivered again.
            build();
            store.trackUploadJob('upload-1');

            store.reportUploadCompleted('Upload', completed());
            store.clearActionExecutionResult();
            store.reportUploadCompleted('Upload', completed());

            expect(store.actionExecutionResult()).toBeUndefined();
        });
    });

    describe('reportRefreshCompleted', () => {
        const completed = (
            overrides: Partial<DotBulkRefreshCompletedEvent> = {}
        ): DotBulkRefreshCompletedEvent =>
            ({
                jobId: 'job-1',
                state: 'SUCCESS',
                total: 2,
                successCount: 2,
                failedCount: 0,
                skippedCount: 0,
                ...overrides
            }) as DotBulkRefreshCompletedEvent;

        const submitRefresh = () => {
            refresh.mockReturnValue(of({ jobId: 'job-1' }));
            store.executeRefresh('Refresh', ['inode-1']);
        };

        it('should publish a result for a run this store submitted', () => {
            build();
            submitRefresh();

            store.reportRefreshCompleted('Refresh', completed());

            expect(store.actionExecutionResult()).toEqual(
                expect.objectContaining({
                    actionName: 'Refresh',
                    successCount: 2,
                    backgrounded: true
                })
            );
        });

        it('should ignore an event for a run it did not submit', () => {
            build();
            // The event is scoped to the user, so another tab's run arrives here too. Silent by
            // design: an error toast would blame this user for somebody else's run.
            store.reportRefreshCompleted('Refresh', completed({ jobId: 'someone-elses' }));

            expect(store.actionExecutionResult()).toBeUndefined();
            expect(handle).not.toHaveBeenCalled();
        });

        it('should treat a non-terminal state as an error, not a green result', () => {
            build();
            submitRefresh();

            store.reportRefreshCompleted('Refresh', completed({ state: 'FAILED' }));

            expect(handle).toHaveBeenCalled();
            expect(store.actionExecutionResult()).toBeUndefined();
        });

        it('should treat counters that do not close over total as an error', () => {
            build();
            submitRefresh();

            // 1 + 0 + 0 !== 2: the shortfall is unexplained, so no number is invented.
            store.reportRefreshCompleted('Refresh', completed({ successCount: 1 }));

            expect(handle).toHaveBeenCalled();
            expect(store.actionExecutionResult()).toBeUndefined();
        });

        it('should not clear an unrelated action that is still running', () => {
            build();
            fireDefaultAction.mockReturnValue(new Subject());
            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);
            submitRefresh();

            store.reportRefreshCompleted('Refresh', completed());

            // Was a real hazard when there was one slot to wipe. Keying runs by id removes it by
            // construction, so this now guards the property rather than the workaround.
            expect(store.actionExecution()).toEqual(
                expect.objectContaining({ actionName: 'Lock', total: 1 })
            );
        });

        it('should settle only once for the same run', () => {
            build();
            submitRefresh();

            store.reportRefreshCompleted('Refresh', completed());
            store.clearActionExecutionResult();
            store.reportRefreshCompleted('Refresh', completed());

            expect(store.actionExecutionResult()).toBeUndefined();
        });
    });

    describe('clearActionExecutionResult', () => {
        it('should drop the result once it has been presented', () => {
            build();
            fireDefaultAction.mockReturnValue(of({ summary: { successCount: 1, failCount: 0 } }));
            store.executeQuickAction('lock-id', 'Lock', ['inode-1']);

            store.clearActionExecutionResult();

            expect(store.actionExecutionResult()).toBeUndefined();
        });
    });
});
