import { createServiceFactory, SpectatorService } from '@openng/spectator';
import { of, Subject } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotMessageService,
    DotPropertiesService,
    DotSiteService,
    DotUploadFileService
} from '@dotcms/data-access';
import { ASSET_PICKER_LAUNCHER, DotAssetSearchDialogComponent } from '@dotcms/ui';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { DotWysiwygPluginService } from './dot-wysiwyg-plugin.service';
import { formatDotImageNode } from './utils/editor.utils';

const MOCK_IMAGE_URL_PATTERN = '/dA/{shortyId}/{name}?language_id={languageId}';

/** Minimal TinyMCE surface: register a button, click it, and observe insert/focus. */
class MockEditor {
    private customButtons: Record<string, { onAction: () => void }> = {};

    ui = {
        registry: {
            getAll: () => ({ buttons: this.customButtons }),
            addButton: (name: string, config: { onAction: () => void }) => {
                this.customButtons[name] = config;
            }
        }
    };

    on = jest.fn();
    insertContent = jest.fn();
    focus = jest.fn();
}

/**
 * The WYSIWYG image button in a **legacy host** — i.e. anywhere `ASSET_PICKER_LAUNCHER` is not
 * provided, which is every host but the three Angular Edit Content ones.
 *
 * `DotWysiwygPluginService` documents itself as constructible outside the Edit Content shell, and
 * the new AssetPicker belongs to that shell only, so here *Add image* must open
 * `DotAssetSearchDialogComponent` — the dialog it opened before #36944.
 *
 * Kept in its own file because Spectator allows a single `createServiceFactory` per file, and the
 * sibling `dot-wysiwyg-plugin.service.spec.ts` provides the launcher.
 */
describe('DotWysiwygPluginService — legacy host (no asset-picker launcher)', () => {
    let spectator: SpectatorService<DotWysiwygPluginService>;
    let dialogService: DialogService;
    let editor: MockEditor;
    let closeSpy: jest.Mock;

    const createService = createServiceFactory({
        service: DotWysiwygPluginService,
        providers: [
            DialogService,
            {
                provide: DotPropertiesService,
                useValue: { getKey: jest.fn().mockReturnValue(of(MOCK_IMAGE_URL_PATTERN)) }
            },
            { provide: DotUploadFileService, useValue: { publishContent: jest.fn() } },
            { provide: DotSiteService, useValue: { getCurrentSite: jest.fn() } },
            { provide: DotMessageService, useValue: { get: jest.fn((key: string) => key) } }
            // ASSET_PICKER_LAUNCHER intentionally not provided (legacy host).
            // DotEditContentStore is absent too, as it is in the legacy Dojo pages.
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createService();
        dialogService = spectator.inject(DialogService);
        editor = new MockEditor();
        closeSpy = jest.fn();
    });

    /** Mocks `DialogService.open` to close with `asset`, then clicks the toolbar button. */
    const clickAddImage = (asset?: unknown) => {
        jest.spyOn(dialogService, 'open').mockReturnValue({
            onClose: of(asset),
            close: closeSpy
        } as DynamicDialogRef);

        spectator.service.initializePlugins(editor as never);
        editor.ui.registry.getAll().buttons['dotAddImage'].onAction();
    };

    it('should not resolve the launcher token in this host', () => {
        // `TestBed.inject` with `optional` is the only way to ask without throwing NG0201 — which
        // is exactly the shape every consumer uses.
        expect(TestBed.inject(ASSET_PICKER_LAUNCHER, null, { optional: true })).toBeNull();
    });

    it('should open the legacy asset search dialog', () => {
        clickAddImage(EMPTY_CONTENTLET);

        expect(dialogService.open).toHaveBeenCalledWith(
            DotAssetSearchDialogComponent,
            expect.objectContaining({
                header: 'Insert Image',
                data: { assetType: 'image' }
            })
        );
    });

    it('should never look up a site — the legacy dialog browses without one', () => {
        clickAddImage(EMPTY_CONTENTLET);

        expect(spectator.inject(DotSiteService).getCurrentSite).not.toHaveBeenCalled();
    });

    it('should insert the selected image honoring the URL pattern', () => {
        clickAddImage(EMPTY_CONTENTLET);

        expect(editor.insertContent).toHaveBeenCalledWith(
            formatDotImageNode(MOCK_IMAGE_URL_PATTERN, EMPTY_CONTENTLET)
        );
        expect(editor.focus).toHaveBeenCalled();
    });

    it('should return focus without inserting when dismissed', () => {
        clickAddImage(undefined);

        expect(editor.insertContent).not.toHaveBeenCalled();
        // Dismissing via ✕, Esc or the mask must still leave the caret in the editor.
        expect(editor.focus).toHaveBeenCalled();
    });

    it('should not stack a second dialog while one is open', () => {
        const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue({
            // Never emits: the dialog stays open for the duration of the test.
            onClose: new Subject(),
            close: closeSpy
        } as DynamicDialogRef);

        spectator.service.initializePlugins(editor as never);
        const button = editor.ui.registry.getAll().buttons['dotAddImage'];
        button.onAction();
        button.onAction();

        expect(openSpy).toHaveBeenCalledTimes(1);
    });

    it('should open again after the dialog closed', () => {
        clickAddImage(undefined);
        editor.ui.registry.getAll().buttons['dotAddImage'].onAction();

        expect(dialogService.open).toHaveBeenCalledTimes(2);
    });

    it('should close an open dialog when the field is destroyed', () => {
        // PrimeNG never closes dialogs on service teardown (`DialogService` has no `ngOnDestroy`),
        // so the service has to do it or the dialog outlives the field that opened it.
        jest.spyOn(dialogService, 'open').mockReturnValue({
            onClose: new Subject(),
            close: closeSpy
        } as DynamicDialogRef);

        spectator.service.initializePlugins(editor as never);
        editor.ui.registry.getAll().buttons['dotAddImage'].onAction();
        spectator.service.ngOnDestroy();

        expect(closeSpy).toHaveBeenCalledTimes(1);
    });
});
