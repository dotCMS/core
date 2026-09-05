import { beforeEach, describe, expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { TreeNodeData } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUploadTypeSelectorComponent } from './dot-upload-type-selector.component';
import { DotUploadSelection } from './models';

const TARGET_FOLDER = {
    id: 'folder-123',
    hostname: 'localhost',
    path: 'folder-123',
    type: 'folder'
} as TreeNodeData;

describe('DotUploadTypeSelectorComponent', () => {
    let spectator: Spectator<DotUploadTypeSelectorComponent>;

    const createComponent = createComponentFactory({
        component: DotUploadTypeSelectorComponent,
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
                        'Set your default upload type in the Folder Settings.',
                    'content-drive.dialog.upload-selector.asset.description.scoped':
                        'For {0} used in your content',
                    'content-drive.dialog.upload-selector.file.description.scoped':
                        'For {0} that need predictable URLs'
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

    describe('copy scoped to a restriction', () => {
        const descriptionOf = (baseType: string) =>
            spectator
                .query(byTestId(`upload-selector-option-${baseType}`))
                ?.textContent?.replace(/\s+/g, ' ')
                .trim();

        describe('when the host restricts what may be uploaded', () => {
            beforeEach(() => {
                spectator.setInput('restrictionLabel', 'video files');
                spectator.detectChanges();
            });

            it('should still offer both storage options', () => {
                // The list is never filtered. An Asset and a File can each hold a video, and a
                // folder's pinned default can be either, so removing one takes away a real choice.
                expect(spectator.query(byTestId('upload-selector-option-DOTASSET'))).toBeTruthy();
                expect(spectator.query(byTestId('upload-selector-option-FILEASSET'))).toBeTruthy();
            });

            it('should describe the Asset option in terms of the restriction', () => {
                expect(descriptionOf('DOTASSET')).toContain('For video files used in your content');
            });

            it('should describe the File option in terms of the restriction', () => {
                expect(descriptionOf('FILEASSET')).toContain(
                    'For video files that need predictable URLs'
                );
            });

            it('should not promise types the restriction excludes', () => {
                // The defect's most visible symptom: "For images, documents, and media" offered
                // inside a video-only picker.
                expect(descriptionOf('DOTASSET')).not.toContain('For images');
                expect(descriptionOf('FILEASSET')).not.toContain('For code');
            });
        });

        describe('when the host restricts nothing', () => {
            // Content Drive passes no label, so its rendered copy must be exactly today's. This is
            // the AC-008 guarantee — assert it rather than assume it.
            it('should keep the default Asset description', () => {
                expect(descriptionOf('DOTASSET')).toContain('For images');
            });

            it('should keep the default File description', () => {
                expect(descriptionOf('FILEASSET')).toContain('For code');
            });

            it('should keep the default descriptions for an empty label', () => {
                spectator.setInput('restrictionLabel', '');
                spectator.detectChanges();

                expect(descriptionOf('DOTASSET')).toContain('For images');
                expect(descriptionOf('FILEASSET')).toContain('For code');
            });
        });
    });

    describe('selection', () => {
        it('should emit the DOTASSET selection with the folder and files when Asset is clicked', () => {
            const files = { length: 0 } as FileList;
            spectator.setInput('files', files);
            spectator.detectChanges();

            let emitted: DotUploadSelection | undefined;
            spectator.component.selectUploadType.subscribe((selection) => (emitted = selection));

            clickOption('DOTASSET');

            expect(emitted).toEqual({
                targetFolder: TARGET_FOLDER,
                baseType: 'DOTASSET',
                files
            });
        });

        it('should emit the FILEASSET selection when File is clicked', () => {
            let emitted: DotUploadSelection | undefined;
            spectator.component.selectUploadType.subscribe((selection) => (emitted = selection));

            clickOption('FILEASSET');

            expect(emitted?.baseType).toBe('FILEASSET');
            expect(emitted?.targetFolder).toEqual(TARGET_FOLDER);
        });
    });
});
