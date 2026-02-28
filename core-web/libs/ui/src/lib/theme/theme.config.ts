import { definePreset } from '@primeuix/themes';
import Lara from '@primeuix/themes/lara';

import { DotUiColorsService } from '@dotcms/data-access';

/**
 * Custom Lara preset for dotCMS
 *
 * The primary color palette is generated from DEFAULT_COLORS.primary (#426BF0)
 * using the same logic as DotUiColorsService. This ensures consistency between:
 * - Initial theme configuration (this file)
 * - Runtime color updates (DotUiColorsService)
 *
 * When colors are loaded from the server, DotUiColorsService.updatePrimeNGColors()
 * will dynamically update the palette using updatePrimaryPalette().
 *
 * Note: Secondary color is NOT defined here in semantic tokens because PrimeNG doesn't
 * support dynamic updates for secondary semantic tokens (only primary can be updated at runtime).
 * Components using severity="secondary" will use the default Lara preset value.
 *
 * However, secondary color IS updated dynamically via CSS variables (--color-palette-secondary-*)
 * which are used by Angular components and custom PrimeNG styles that reference CSS variables.
 */
export const CustomLaraPreset = definePreset(Lara, {
    semantic: {
        primary: DotUiColorsService.getDefaultPrimeNGPalette()
        // Secondary could be added here, but it wouldn't be updatable at runtime
        // secondary: DotUiColorsService.getDefaultSecondaryPalette() // Not implemented
    },
    components: {
        treeselect: {
            tree: {
                padding: '0.5rem'
            }
        },
        card: {
            body: {
                padding: '1rem'
            }
        },
        toolbar: {
            root: {
                borderRadius: '0',
                padding: '0.5rem 1rem'
            }
        }
    }
});
