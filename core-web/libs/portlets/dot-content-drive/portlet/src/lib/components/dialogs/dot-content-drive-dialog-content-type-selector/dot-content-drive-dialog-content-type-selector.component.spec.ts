import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import {
    DOT_PALETTE_PERSIST_PREFERENCES,
    DotUvePaletteListComponent,
    DotUVEPaletteListTypes
} from '@dotcms/portlets/dot-ema/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveDialogContentTypeSelectorComponent } from './dot-content-drive-dialog-content-type-selector.component';

import { DotContentDriveNavigationService } from '../../../shared/services/dot-content-drive-navigation.service';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

const SELECTED_VARIABLE = 'Blog';

// Real signal: the component reads it in a computed, and a vi.fn cannot invalidate one.
const systemHostSelected = signal(false);

describe('DotContentDriveDialogContentTypeSelectorComponent', () => {
    let spectator: Spectator<DotContentDriveDialogContentTypeSelectorComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let navigationService: SpyObject<DotContentDriveNavigationService>;

    const createComponent = createComponentFactory({
        component: DotContentDriveDialogContentTypeSelectorComponent,
        overrideComponents: [
            [
                DotContentDriveDialogContentTypeSelectorComponent,
                {
                    remove: { imports: [DotUvePaletteListComponent] },
                    add: { imports: [MockComponent(DotUvePaletteListComponent)] }
                }
            ]
        ],
        providers: [
            mockProvider(DotContentDriveStore, {
                closeDialog: vi.fn(),
                currentSite: vi
                    .fn()
                    .mockReturnValue({ hostname: 'demo.dotcms.com', identifier: 'site-1' }),
                path: vi.fn().mockReturnValue('/about-us/'),
                selectedNode: vi
                    .fn()
                    .mockReturnValue({ data: { type: 'folder', inode: 'inode-1' } }),
                $systemHostSelected: systemHostSelected
            }),
            mockProvider(DotContentDriveNavigationService, {
                createContent: vi.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.common.cancel': 'Cancel',
                    'content-drive.dialog.content-type-selector.create': 'Create'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('listType', DotUVEPaletteListTypes.ALL_CONTENT_TYPES);
        spectator.detectChanges();

        store = spectator.inject(DotContentDriveStore, true);
        navigationService = spectator.inject(DotContentDriveNavigationService, true);
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('palette preferences', () => {
        it('should disable palette preference persistence so it never touches the UVE keys', () => {
            expect(spectator.inject(DOT_PALETTE_PERSIST_PREFERENCES, true)).toBe(false);
        });
    });

    describe('palette list rendering', () => {
        it('should render the palette list in selection mode with the given listType', () => {
            // ng-mocks exposes signal inputs on the mocked instance as raw bound values.
            const paletteList = spectator.query(DotUvePaletteListComponent) as unknown as {
                $type: DotUVEPaletteListTypes;
                $selectionMode: boolean;
            };

            expect(paletteList).toBeTruthy();
            expect(paletteList.$type).toBe(DotUVEPaletteListTypes.ALL_CONTENT_TYPES);
            expect(paletteList.$selectionMode).toBe(true);
        });
    });

    describe('create button state', () => {
        it('should disable the create button when nothing is selected', () => {
            const createButton = spectator
                .query(byTestId('content-type-selector-create'))
                ?.querySelector('button');

            expect(createButton?.disabled).toBe(true);
        });

        it('should enable the create button after a content type is selected', () => {
            spectator.triggerEventHandler(
                DotUvePaletteListComponent,
                'selectContentType',
                SELECTED_VARIABLE
            );
            spectator.detectChanges();

            const createButton = spectator
                .query(byTestId('content-type-selector-create'))
                ?.querySelector('button');

            expect(createButton?.disabled).toBe(false);
        });
    });

    describe('create action', () => {
        beforeEach(() => {
            spectator.triggerEventHandler(
                DotUvePaletteListComponent,
                'selectContentType',
                SELECTED_VARIABLE
            );
            spectator.detectChanges();
        });

        // Where the new content goes is the navigation service's call, shared with `createContent`
        // links (#37759); the selector only names the type.
        it('should ask the navigation service to create the chosen type and close the dialog', () => {
            const createButton = spectator
                .query(byTestId('content-type-selector-create'))
                ?.querySelector('button');

            spectator.click(createButton);

            expect(navigationService.createContent).toHaveBeenCalledWith(SELECTED_VARIABLE);
            expect(store.closeDialog).toHaveBeenCalled();
        });
    });

    describe('cancel action', () => {
        it('should close the dialog and not create content when Cancel is clicked', () => {
            const cancelButton = spectator
                .query(byTestId('content-type-selector-cancel'))
                ?.querySelector('button');

            spectator.click(cancelButton);

            expect(store.closeDialog).toHaveBeenCalled();
            expect(navigationService.createContent).not.toHaveBeenCalled();
        });
    });
});
