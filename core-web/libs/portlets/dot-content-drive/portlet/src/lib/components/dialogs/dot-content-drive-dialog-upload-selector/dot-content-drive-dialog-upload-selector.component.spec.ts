import { beforeEach, describe, expect, it } from '@jest/globals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotFolderTreeNodeData } from '@dotcms/portlets/content-drive/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveDialogUploadSelectorComponent } from './dot-content-drive-dialog-upload-selector.component';

import { DotContentDriveUploadSelection } from '../../../shared/models';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';

const TARGET_FOLDER = {
    id: 'folder-123',
    hostname: 'localhost',
    path: 'folder-123',
    type: 'folder'
} as DotFolderTreeNodeData;

describe('DotContentDriveDialogUploadSelectorComponent', () => {
    let spectator: Spectator<DotContentDriveDialogUploadSelectorComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveDialogUploadSelectorComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                closeDialog: jest.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.common.cancel': 'Cancel',
                    'content-drive.dialog.upload-selector.asset': 'Asset',
                    'content-drive.dialog.upload-selector.asset.description': 'For images',
                    'content-drive.dialog.upload-selector.file': 'File',
                    'content-drive.dialog.upload-selector.file.description': 'For code',
                    'content-drive.dialog.upload-selector.recommended': 'Recommended',
                    'content-drive.dialog.upload-selector.continue': 'Continue'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('targetFolder', TARGET_FOLDER);
        spectator.detectChanges();

        store = spectator.inject(DotContentDriveStore, true);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    const chooseFile = () => {
        const radios = spectator.debugElement.queryAll(By.css('p-radiobutton'));
        spectator.triggerEventHandler(radios[1], 'ngModelChange', 'FILEASSET');
        spectator.detectChanges();
    };

    const clickContinue = () =>
        spectator.click(
            spectator.query(byTestId('upload-selector-continue')).querySelector('button')
        );

    describe('rendering', () => {
        it('should render both upload options', () => {
            expect(spectator.query(byTestId('upload-selector-option-DOTASSET'))).toBeTruthy();
            expect(spectator.query(byTestId('upload-selector-option-FILEASSET'))).toBeTruthy();
        });

        it('should mark only the Asset option as recommended', () => {
            const recommended = spectator.queryAll(byTestId('upload-selector-recommended'));
            const assetOption = spectator.query(byTestId('upload-selector-option-DOTASSET'));

            expect(recommended.length).toBe(1);
            expect(
                assetOption?.querySelector('[data-testid="upload-selector-recommended"]')
            ).toBeTruthy();
        });

        it('should enable Continue by default with Asset preselected', () => {
            const continueButton = spectator
                .query(byTestId('upload-selector-continue'))
                ?.querySelector('button');

            expect(continueButton?.disabled).toBe(false);
        });
    });

    describe('selection', () => {
        it('should emit the DOTASSET selection with the folder and files when Continue is clicked', () => {
            const files = { length: 0 } as FileList;
            spectator.setInput('files', files);
            spectator.detectChanges();

            let emitted: DotContentDriveUploadSelection | undefined;
            spectator.component.selectUploadType.subscribe((selection) => (emitted = selection));

            clickContinue();

            expect(emitted).toEqual({
                targetFolder: TARGET_FOLDER,
                baseType: 'DOTASSET',
                files
            });
        });

        it('should emit the FILEASSET selection when File is chosen', () => {
            let emitted: DotContentDriveUploadSelection | undefined;
            spectator.component.selectUploadType.subscribe((selection) => (emitted = selection));

            chooseFile();
            clickContinue();

            expect(emitted?.baseType).toBe('FILEASSET');
            expect(emitted?.targetFolder).toEqual(TARGET_FOLDER);
        });

        it('should not emit and should close the dialog when Cancel is clicked', () => {
            const emitSpy = jest.fn();
            spectator.component.selectUploadType.subscribe(emitSpy);

            spectator.click(
                spectator.query(byTestId('upload-selector-cancel'))?.querySelector('button')
            );

            expect(store.closeDialog).toHaveBeenCalled();
            expect(emitSpy).not.toHaveBeenCalled();
        });
    });
});
