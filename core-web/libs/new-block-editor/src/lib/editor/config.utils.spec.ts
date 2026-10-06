import {
    FULLSCREEN_AWARE_OVERLAY_OPTIONS,
    OVERLAY_ABOVE_FULLSCREEN_Z_INDEX,
    buildBrowserSelectorConfig
} from './config.utils';

/**
 * `buildBrowserSelectorConfig` is what the legacy Dojo host's image / video / audio pickers are
 * configured with, and restoring it exactly is an acceptance criterion of #37132 — the old editor
 * has to keep browsing what it always browsed.
 *
 * These assertions are deliberately on the literal values rather than on the builder's own output:
 * `EditorModalService`'s spec asserts it *delegates* here, which by construction cannot notice a
 * field being dropped from this function. This is the file that would.
 */
describe('buildBrowserSelectorConfig', () => {
    const config = buildBrowserSelectorConfig({ header: 'Pick a thing', mimeTypes: ['image'] });

    it('should pass the caller header and mime scoping through', () => {
        expect(config.header).toBe('Pick a thing');
        expect(config.data.mimeTypes).toEqual(['image']);
    });

    it('should clear the fullscreen editor shell backdrop', () => {
        // The shell renders its backdrop at `z-[9998]`; a modal on PrimeNG's default base (~1000)
        // paints underneath it and cannot be clicked.
        expect(config.baseZIndex).toBe(OVERLAY_ABOVE_FULLSCREEN_Z_INDEX);
    });

    it('should keep the dialog dismissable every way a user expects', () => {
        expect(config).toEqual(
            expect.objectContaining({
                closable: true,
                closeOnEscape: true,
                dismissableMask: true,
                modal: true
            })
        );
    });

    it('should keep the restored sizing, including the fullscreen-safe min-height', () => {
        expect(config).toEqual(
            expect.objectContaining({
                appendTo: 'body',
                draggable: false,
                keepInViewport: false,
                maskStyleClass: 'p-dialog-mask-dynamic',
                resizable: false,
                width: '90%',
                style: { 'max-width': '1040px', overflow: 'hidden' },
                contentStyle: { overflow: 'auto', 'min-height': 'min(45rem, 80vh)' }
            })
        );
    });

    it('should browse exactly what it browsed before the AssetPicker landed', () => {
        // Every one of these flags is part of the restore. Asserting the whole object means dropping
        // one fails here rather than silently changing what the legacy editor can pick.
        expect(config.data).toEqual({
            mimeTypes: ['image'],
            showLinks: false,
            showDotAssets: true,
            showPages: false,
            showFiles: true,
            showFolders: false,
            showWorking: true,
            showArchived: false,
            sortByDesc: true
        });
    });

    it('should leave the mime allowlist unrestricted when given none', () => {
        expect(buildBrowserSelectorConfig({ header: 'h', mimeTypes: [] }).data.mimeTypes).toEqual(
            []
        );
    });

    it('should lift editor dropdown panels above the fullscreen shell too', () => {
        expect(FULLSCREEN_AWARE_OVERLAY_OPTIONS).toEqual({
            autoZIndex: true,
            baseZIndex: OVERLAY_ABOVE_FULLSCREEN_Z_INDEX
        });
    });
});
