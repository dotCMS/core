import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, Subject } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import {
    DotAiService,
    DotMessageService,
    DotSiteService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { ASSET_PICKER_LAUNCHER, DotBrowserSelectorComponent } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldComponent } from './dot-file-field.component';

import { FILE_FIELD_MOCK, IMAGE_FIELD_MOCK } from '../../../../utils/mocks';
import {
    LegacyDialogImageEditorLauncher,
    LegacyDojoImageEditorLauncher
} from '../../services/image-editor';
import { DotFileFieldUploadService } from '../../services/upload-file/upload-file.service';
import { FileFieldStore } from '../../store/file-field.store';
import { DotFileFieldPreviewComponent } from '../dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from '../dot-file-field-ui-message/dot-file-field-ui-message.component';

/**
 * "Select Existing File" in the **legacy Dojo host** — i.e. the field mounted as the
 * `dotcms-binary-field` custom element, where `ASSET_PICKER_LAUNCHER` is not provided.
 *
 * The new AssetPicker belongs to the Angular Edit Content, so here the field must keep opening
 * `DotBrowserSelectorComponent` with the same scoping and header it used before #36944.
 *
 * Kept in its own file because Spectator allows a single `createComponentFactory` per file, and
 * `dot-file-field.component.spec.ts` provides the launcher.
 */
describe('DotFileFieldComponent — legacy host picker (no asset-picker launcher)', () => {
    let spectator: Spectator<DotFileFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotFileFieldComponent,
        imports: [ReactiveFormsModule],
        componentMocks: [DotFileFieldPreviewComponent, DotFileFieldUiMessageComponent],
        providers: [
            // Deliberately NO Router and NO GlobalStore: the legacy Dojo host is a custom element
            // bootstrapped without either, so anything the field reaches for has to survive that.
            mockProvider(DotSiteService, { getCurrentSite: jest.fn() }),
            FileFieldStore,
            mockProvider(DotFileFieldUploadService),
            mockProvider(DialogService),
            LegacyDialogImageEditorLauncher,
            LegacyDojoImageEditorLauncher,
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
            mockProvider(DotAiService, {
                checkPluginInstallation: jest.fn().mockReturnValue(of(false))
            }),
            provideHttpClient(),
            provideHttpClientTesting()
            // ASSET_PICKER_LAUNCHER intentionally not provided (legacy host).
        ]
    });

    /** Mounts the field for `field` and stubs `DialogService.open` to close with `selected`. */
    const setup = (field: typeof IMAGE_FIELD_MOCK, selected?: unknown) => {
        spectator = createComponent({
            props: {
                field,
                contentlet: createFakeContentlet({ [field.variable]: null }),
                hasError: false
            } as never
        });
        spectator.detectChanges();

        const dialogService = spectator.inject(DialogService);
        (dialogService.open as jest.Mock).mockReturnValue({
            onClose: selected === undefined ? new Subject() : of(selected),
            close: jest.fn()
        });

        return dialogService;
    };

    it('should not resolve the launcher token in this host', () => {
        setup(IMAGE_FIELD_MOCK);

        // `TestBed.inject` with `optional` is the only way to ask without throwing NG0201 — which
        // is exactly the shape every consumer uses.
        expect(TestBed.inject(ASSET_PICKER_LAUNCHER, null, { optional: true })).toBeNull();
    });

    it('should open the legacy browser selector for an Image field', () => {
        const dialogService = setup(IMAGE_FIELD_MOCK);

        spectator.component.showSelectExistingFileDialog();

        expect(dialogService.open).toHaveBeenCalledWith(
            DotBrowserSelectorComponent,
            expect.objectContaining({
                header: 'dot.file.field.dialog.select.existing.image.header',
                data: expect.objectContaining({ mimeTypes: ['image'] })
            })
        );
    });

    it('should open the legacy browser selector unrestricted for a File field', () => {
        const dialogService = setup(FILE_FIELD_MOCK);

        spectator.component.showSelectExistingFileDialog();

        expect(dialogService.open).toHaveBeenCalledWith(
            DotBrowserSelectorComponent,
            expect.objectContaining({
                header: 'dot.file.field.dialog.select.existing.file.header',
                data: expect.objectContaining({ mimeTypes: [] })
            })
        );
    });

    it('should never look up a site — the legacy selector browses without one', () => {
        setup(IMAGE_FIELD_MOCK);

        spectator.component.showSelectExistingFileDialog();

        expect(spectator.inject(DotSiteService).getCurrentSite).not.toHaveBeenCalled();
    });

    it('should write the selected asset to the store exactly as the new picker does', () => {
        const file = createFakeContentlet({ identifier: 'picked-id' });
        setup(IMAGE_FIELD_MOCK, file);
        const setPreviewFile = jest.spyOn(spectator.component.store, 'setPreviewFile');

        spectator.component.showSelectExistingFileDialog();

        expect(setPreviewFile).toHaveBeenCalledWith({ source: 'contentlet', file });
    });

    it('should not touch the store when the selector is dismissed', () => {
        setup(IMAGE_FIELD_MOCK, null);
        const setPreviewFile = jest.spyOn(spectator.component.store, 'setPreviewFile');

        spectator.component.showSelectExistingFileDialog();

        expect(setPreviewFile).not.toHaveBeenCalled();
    });

    it('should open exactly one dialog when the trigger is double-clicked', () => {
        // The pending guard the new picker needs for its async site lookup must not leave the legacy
        // path able to stack two dialogs either.
        const dialogService = setup(IMAGE_FIELD_MOCK);

        spectator.component.showSelectExistingFileDialog();
        spectator.component.showSelectExistingFileDialog();

        expect(dialogService.open).toHaveBeenCalledTimes(1);
    });

    it('should open again after the selector closed', () => {
        const dialogService = setup(IMAGE_FIELD_MOCK, null);

        spectator.component.showSelectExistingFileDialog();
        spectator.component.showSelectExistingFileDialog();

        expect(dialogService.open).toHaveBeenCalledTimes(2);
    });

    it('should do nothing while the field is disabled', () => {
        const dialogService = setup(IMAGE_FIELD_MOCK);
        spectator.component.setDisabledState(true);

        spectator.component.showSelectExistingFileDialog();

        expect(dialogService.open).not.toHaveBeenCalled();
    });

    it('should close an open selector when the field is destroyed', () => {
        const dialogService = setup(IMAGE_FIELD_MOCK);
        const close = jest.fn();
        (dialogService.open as jest.Mock).mockReturnValue({ onClose: new Subject(), close });

        spectator.component.showSelectExistingFileDialog();
        spectator.component.ngOnDestroy();

        expect(close).toHaveBeenCalled();
    });
});
