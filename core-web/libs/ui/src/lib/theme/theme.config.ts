import { definePreset } from '@primeuix/themes';
import Lara from '@primeuix/themes/lara';

import { DotUiColorsService } from '@dotcms/data-access';

/**
 * Custom Lara preset for dotCMS
 *
 * The primary palette is generated from DEFAULT_COLORS.primary (#426BF0) via the same
 * PrimeNG palette() generator DotUiColorsService uses, keeping this initial preset and
 * runtime updates (updatePrimaryPalette) in sync.
 *
 * Brand secondary is intentionally absent here. PrimeNG models a single accent (primary)
 * plus neutral surfaces and has no second-accent slot, so the dotCMS secondary brand color
 * lives only in the legacy --color-palette-secondary-* CSS vars (set at runtime by
 * DotUiColorsService, consumed by Angular components and the JSP/Dojo iframe).
 * Note: severity="secondary" is unrelated — it is PrimeNG's neutral/gray variant, not a brand color.
 *
 * Future direction: register secondary as a custom token group via the preset's `extend`
 * option to get engine-managed --p-secondary-* tokens. See issue #35869.
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
            root: {
                shadow: 'none'
            },
            body: {
                padding: '1rem'
            },
            css: `
                .p-card {
                    border: 1px solid dt('gray.300');
                }
            `
        },
        chip: {
            // dotCMS chips are compact by default: 1.75rem (24.5px at the 14px root)
            // tall, vertically centered, with a small label. Applied to the base
            // `.p-chip` so every chip (Content Status, locale, relationship, etc.)
            // gets the size without per-template classes. PrimeNG has no chip size
            // token, so this is expressed as CSS — same mechanism as card/confirmpopup.
            css: `
                .p-chip {
                    height: calc(var(--spacing) * 7); /* 1.75rem */
                    padding: 0 calc(var(--spacing) * 2); /* 0.5rem */
                    font-size: var(--text-xs); /* 0.75rem */
                }
            `
        },
        tag: {
            // Status tags follow the dotCMS design spec globally (not per-instance, so a
            // forgotten class can never make one look different): a fully-rounded pill with
            // a tinted background + dark text instead of Lara's default small-radius solid
            // fill + white text. Shape/typography live in `root`; the soft per-severity
            // colors live in `colorScheme`. {green.100}/{green.700} map 1:1 to the design.
            root: {
                borderRadius: '9999px',
                padding: '4px 12px',
                fontWeight: '600'
            },
            colorScheme: {
                light: {
                    success: {
                        background: '{green.100}',
                        color: '{green.700}'
                    }
                }
            }
        },
        toolbar: {
            root: {
                borderRadius: '0',
                padding: '0.5rem 1rem'
            }
        },
        confirmpopup: {
            // Hide the arrow (pseudo-elements) on p-confirmpopup; no token for visibility in the preset.
            css: `
                .p-confirmpopup:before,
                .p-confirmpopup:after {
                    display: none !important;
                }
            `
        }
    }
});
