import { DotSite } from '@dotcms/dotcms-models';

import { buildAssetPickerDialogConfig } from './asset-picker-dialog';
import { DotAssetPickerConfig } from './store/models';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

const DATA: DotAssetPickerConfig = { site: SITE };

describe('buildAssetPickerDialogConfig', () => {
    it('should carry the picker configuration through as the dialog data', () => {
        expect(buildAssetPickerDialogConfig(DATA).data).toBe(DATA);
    });

    describe("flags the picker's own markup depends on", () => {
        // These are the picker's contract, not a caller preference — the reason this builder exists
        // instead of every caller assembling its own config.
        it('should hide PrimeNG chrome header', () => {
            // The picker renders its own header, so PrimeNG's would be a second one.
            expect(buildAssetPickerDialogConfig(DATA).showHeader).toBe(false);
        });

        it('should be maximizable', () => {
            // The picker's full-screen toggle drives PrimeNG's maximized state.
            expect(buildAssetPickerDialogConfig(DATA).maximizable).toBe(true);
        });

        it('should not autofocus on show', () => {
            // Autofocus lands on the search input and paints a focus halo that reads as an error.
            expect(buildAssetPickerDialogConfig(DATA).focusOnShow).toBe(false);
        });

        it('should let the picker fill the dialog', () => {
            // Without a full-height content box the picker cannot grow with the full-screen toggle.
            expect(buildAssetPickerDialogConfig(DATA).contentStyle).toEqual({
                height: '100%',
                overflow: 'hidden',
                padding: '0'
            });
        });

        it('should size itself without an inline max-width', () => {
            // A `max-width` would still clamp the dialog once it goes full screen.
            const config = buildAssetPickerDialogConfig(DATA);

            expect(config.width).toBe('min(90vw, 114rem)');
            expect(config.height).toBe('min(90vh, 68rem)');
            expect(config.style).toBeUndefined();
        });
    });

    describe('baseZIndex', () => {
        it('should be left to PrimeNG by default', () => {
            expect(buildAssetPickerDialogConfig(DATA).baseZIndex).toBeUndefined();
        });

        it('should be overridable for a caller stacked under its own backdrop', () => {
            // The Story Block's full-screen shell paints at `z-[9998]`.
            expect(buildAssetPickerDialogConfig(DATA, { baseZIndex: 10050 }).baseZIndex).toBe(
                10050
            );
        });
    });
});
