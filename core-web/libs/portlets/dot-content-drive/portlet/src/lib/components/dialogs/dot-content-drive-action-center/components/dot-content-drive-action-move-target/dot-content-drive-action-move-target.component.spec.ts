import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { EMPTY, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotBrowsingService } from '@dotcms/ui';

import { DotContentDriveActionMoveTargetComponent } from './dot-content-drive-action-move-target.component';

describe('DotContentDriveActionMoveTargetComponent', () => {
    let spectator: Spectator<DotContentDriveActionMoveTargetComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveActionMoveTargetComponent,
        providers: [
            // Paired with the testing backend: a real HttpClient in jsdom dials
            // localhost for every relative URL and the request dies with
            // "socket hang up", asynchronously — Jest dropped that, Vitest counts it.
            // Nothing asserts on these requests; they just must not leave the process.
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, {
                get: vi.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotHttpErrorManagerService),
            // Backs the picker's own store. The picker renders for real: this component exists only to
            // host it, so stubbing it out would leave nothing under test.
            mockProvider(DotBrowsingService, {
                getSitesTreePath: vi.fn(() => of([])),
                // `pagination`, not `total`: the host-folder store reads
                // `pagination.totalEntries` off this response, and the wrong shape left
                // it dereferencing undefined inside an effect.
                getSitesPage: vi.fn(() =>
                    of({ sites: [], pagination: { currentPage: 1, perPage: 15, totalEntries: 0 } })
                ),
                // EMPTY, not `of(null)`: these tests need the current-site lookup to
                // produce nothing, and the store dereferences `currentSite.key` without
                // guarding the null its own return type allows — a product defect, so
                // the fixture completes without emitting instead. Same meaning for the
                // picker, no NPE inside the effect.
                getCurrentSiteAsTreeNodeItem: vi.fn(() => EMPTY)
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({ props: { itemCount: 3 } });
        spectator.detectChanges();
    });

    it('should render the folder picker', () => {
        expect(spectator.query(byTestId('move-target-picker'))).toBeTruthy();
    });

    it('should render no dialog of its own', () => {
        // The shell owns the one dialog; a second one here would nest a modal inside it.
        expect(spectator.query('p-dialog')).toBeNull();
    });

    it('should add no controls of its own outside the picker', () => {
        // Continue belongs to the Action Center's pinned footer, which owns the in-flight disabled
        // state; a control here would be a second, unsynchronised commit path. The picker's own
        // buttons (its trigger, copy and Select) are its business, so only strays outside it count.
        const picker = spectator.query(byTestId('move-target-picker'));
        const strays = spectator
            .queryAll('button')
            .filter((button) => !picker?.contains(button as Node));

        expect(strays).toEqual([]);
    });

    describe('choosing a destination', () => {
        it('should emit the path in the actionlet format', () => {
            const emitted: string[] = [];
            spectator.output<string>('pathToMoveChange').subscribe((path) => emitted.push(path));

            spectator.component['onHostFolderValueChange']('demo.dotcms.com:/application');

            expect(emitted).toEqual(['//demo.dotcms.com/application']);
        });

        it('should emit an empty path when the picker is cleared', () => {
            const emitted: string[] = [];
            spectator.output<string>('pathToMoveChange').subscribe((path) => emitted.push(path));

            spectator.component['onHostFolderValueChange'](null);

            expect(emitted).toEqual(['']);
        });

        it('should stay invalid for a value it cannot convert', () => {
            // Emitting a malformed path would fire a move the server rejects with an opaque error.
            spectator.component['onHostFolderValueChange']('demo.dotcms.com');

            expect(spectator.component['$hasError']()).toBe(true);
        });
    });

    describe('invalid styling', () => {
        it('should not mark the field invalid before the user has touched it', () => {
            // The bug this guards: the field rendered with a red border the moment the step opened,
            // including through the sites/folders load, when the user had done nothing wrong yet.
            expect(spectator.component['$hasError']()).toBe(false);
            expect(spectator.query('.ng-invalid')).toBeNull();
        });

        it('should not mark the field invalid while the picker is still loading', () => {
            // Same render, no value yet — nothing to correct, so nothing red.
            expect(spectator.component['$hostFolderValue']()).toBeNull();
            expect(spectator.component['$hasError']()).toBe(false);
        });

        it('should mark the field invalid once the user clears a chosen destination', () => {
            spectator.component['onHostFolderValueChange']('demo.dotcms.com:/application');
            spectator.detectChanges();
            expect(spectator.component['$hasError']()).toBe(false);

            spectator.component['onHostFolderValueChange'](null);

            expect(spectator.component['$hasError']()).toBe(true);
        });
    });

    describe('starting path', () => {
        it('should seed the picker with the folder being browsed', () => {
            // The picker has no "open here" input: `writeValue` drives its initial load, so the
            // starting location has to arrive as a value.
            spectator = createComponent({
                props: { itemCount: 3, startingPath: '//demo.dotcms.com/blogs' }
            });
            spectator.detectChanges();

            expect(spectator.component['$hostFolderValue']()).toBe('demo.dotcms.com:/blogs');
        });

        it('should leave the picker unseeded for a path it cannot parse', () => {
            spectator = createComponent({
                props: { itemCount: 3, startingPath: 'demo.dotcms.com/blogs' }
            });
            spectator.detectChanges();

            expect(spectator.component['$hostFolderValue']()).toBeNull();
        });

        it('should still let the user choose a different destination', () => {
            spectator = createComponent({
                props: { itemCount: 3, startingPath: '//demo.dotcms.com/blogs' }
            });
            spectator.detectChanges();

            const emitted: string[] = [];
            spectator.output<string>('pathToMoveChange').subscribe((path) => emitted.push(path));

            spectator.component['onHostFolderValueChange']('demo.dotcms.com:/application');

            expect(emitted).toEqual(['//demo.dotcms.com/application']);
        });
    });
});
