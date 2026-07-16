import { beforeEach, describe, expect, it } from '@jest/globals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { MockComponent } from 'ng-mocks';

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
                closeDialog: jest.fn(),
                currentSite: jest
                    .fn()
                    .mockReturnValue({ hostname: 'demo.dotcms.com', identifier: 'site-1' }),
                path: jest.fn().mockReturnValue('/about-us/'),
                selectedNode: jest.fn().mockReturnValue({ data: { inode: 'inode-1' } })
            }),
            mockProvider(DotContentDriveNavigationService, {
                createContent: jest.fn()
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
        jest.clearAllMocks();
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

        it('should create the content in the current folder and close the dialog when Create is clicked', () => {
            const createButton = spectator
                .query(byTestId('content-type-selector-create'))
                ?.querySelector('button');

            spectator.click(createButton);

            // folderPath = hostname + current path (new editor); folderInode = current folder (legacy editor)
            expect(navigationService.createContent).toHaveBeenCalledWith(SELECTED_VARIABLE, {
                folderPath: 'demo.dotcms.com/about-us/',
                folderInode: 'inode-1'
            });
            expect(store.closeDialog).toHaveBeenCalled();
        });

        it('should fall back to the current site (no folder) when browsing the root', () => {
            // Root: no path selected and the root node carries an empty inode.
            store.path.mockReturnValue(undefined);
            store.selectedNode.mockReturnValue({ data: { inode: '' } });

            const createButton = spectator
                .query(byTestId('content-type-selector-create'))
                ?.querySelector('button');

            spectator.click(createButton);

            expect(navigationService.createContent).toHaveBeenCalledWith(SELECTED_VARIABLE, {
                folderPath: 'demo.dotcms.com',
                folderInode: undefined
            });
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
