import { beforeEach, describe, expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotFolderTreeNodeData } from '@dotcms/portlets/content-drive/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveDialogUploadSelectorComponent } from './dot-content-drive-dialog-upload-selector.component';

import { DotContentDriveUploadSelection } from '../../../shared/models';

const TARGET_FOLDER = {
    id: 'folder-123',
    hostname: 'localhost',
    path: 'folder-123',
    type: 'folder'
} as DotFolderTreeNodeData;

describe('DotContentDriveDialogUploadSelectorComponent', () => {
    let spectator: Spectator<DotContentDriveDialogUploadSelectorComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveDialogUploadSelectorComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.dialog.upload-selector.asset': 'Asset',
                    'content-drive.dialog.upload-selector.asset.description': 'For images',
                    'content-drive.dialog.upload-selector.file': 'File',
                    'content-drive.dialog.upload-selector.file.description': 'For code',
                    'content-drive.dialog.upload-selector.recommended': 'Recommended',
                    'content-drive.dialog.upload-selector.settings-hint':
                        'Set your default upload type in the Folder Settings.'
                })
            }
        ],
        detectChanges: false
    });

    const clickOption = (baseType: string) =>
        spectator.click(byTestId(`upload-selector-option-${baseType}`));

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('targetFolder', TARGET_FOLDER);
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

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

        it('should render the folder-settings hint', () => {
            expect(spectator.query(byTestId('upload-selector-settings-hint'))).toBeTruthy();
        });
    });

    describe('selection', () => {
        it('should emit the DOTASSET selection with the folder and files when Asset is clicked', () => {
            const files = { length: 0 } as FileList;
            spectator.setInput('files', files);
            spectator.detectChanges();

            let emitted: DotContentDriveUploadSelection | undefined;
            spectator.component.selectUploadType.subscribe((selection) => (emitted = selection));

            clickOption('DOTASSET');

            expect(emitted).toEqual({
                targetFolder: TARGET_FOLDER,
                baseType: 'DOTASSET',
                files
            });
        });

        it('should emit the FILEASSET selection when File is clicked', () => {
            let emitted: DotContentDriveUploadSelection | undefined;
            spectator.component.selectUploadType.subscribe((selection) => (emitted = selection));

            clickOption('FILEASSET');

            expect(emitted?.baseType).toBe('FILEASSET');
            expect(emitted?.targetFolder).toEqual(TARGET_FOLDER);
        });
    });
});
