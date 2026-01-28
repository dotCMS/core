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
 */
export const CustomLaraPreset = definePreset(Lara, {
    semantic: {
        primary: DotUiColorsService.getDefaultPrimeNGPalette()
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
