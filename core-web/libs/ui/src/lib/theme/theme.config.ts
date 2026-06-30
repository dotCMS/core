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
        accordion: {
            // Flat accordion app-wide: Lara renders each panel as a rounded,
            // bordered, surface-filled card. We drop the card chrome — the header
            // fill, the L/R/B borders, the first/last corner radii — so sections
            // read as flush bands separated only by their own dividers (and the
            // focus ring becomes square along with the header). Per-feature spacing
            // and dividers stay in the consuming component.
            panel: {
                borderWidth: '0'
            },
            header: {
                borderWidth: '0',
                borderRadius: '0',
                first: {
                    topBorderRadius: '0',
                    borderWidth: '0'
                },
                last: {
                    bottomBorderRadius: '0',
                    activeBottomBorderRadius: '0'
                }
            },
            content: {
                borderWidth: '0'
            },
            colorScheme: {
                light: {
                    header: {
                        // Opaque surface (not transparent) so a header pinned via
                        // position: sticky never shows scrolling content through it.
                        // On a white panel this still reads as a flat, fill-less band.
                        background: '{surface.0}',
                        hoverBackground: '{surface.50}',
                        activeBackground: '{surface.0}',
                        activeHoverBackground: '{surface.50}'
                    }
                }
            }
        },
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
            // `.p-chip` so every chip (locale, relationship, etc.) gets the size
            // without per-template classes. PrimeNG has no chip size token, so this
            // is expressed as CSS — same mechanism as card/confirmpopup. Content
            // status badges use `p-tag` (see the `tag` block below), not chips.
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
            // fill + white text. Soft per-severity colors use PrimeNG palette tokens
            // ({green.100}/{green.700} map 1:1 to the design); shape/typography are expressed
            // with Tailwind theme variables — same mechanism as `chip` — so there are no magic
            // numbers. `calc(infinity * 1px)` is exactly what Tailwind's `rounded-full` emits;
            // there is no --radius-full token.
            css: `
                .p-tag {
                    height: calc(var(--spacing) * 7); /* 1.75rem — same fixed height as chip */
                    border-radius: calc(infinity * 1px);
                    padding: 0 calc(var(--spacing) * 3); /* 0 0.75rem — vertical centering via inline-flex */
                    font-weight: var(--font-weight-medium); /* 500 */
                }
            `,
            // All severities use the soft "tinted background + dark text" pill (palette {x.100}/
            // {x.700}) instead of Lara's solid fills, so status tags read consistently across the
            // app (status badges, version-history states, locale labels) per the design reference.
            // `secondary` is omitted — Lara already maps it to surface.100/surface.600 (soft gray).
            colorScheme: {
                light: {
                    success: {
                        background: '{green.100}',
                        color: '{green.700}'
                    },
                    info: {
                        background: '{blue.100}',
                        color: '{blue.700}'
                    },
                    warn: {
                        background: '{yellow.100}',
                        color: '{yellow.700}'
                    },
                    danger: {
                        background: '{red.100}',
                        color: '{red.700}'
                    }
                }
            }
        },
        tabs: {
            // Underline-style tabs per the design: the active indicator sits on the BOTTOM
            // border (Lara defaults to a 2px TOP border) and tabs have no static background
            // (Lara fills inactive tabs with surface-50). The active state still reads via the
            // primary bottom border + primary text (tab.activeBorderColor / activeColor).
            tab: {
                borderWidth: '0 0 2px 0'
            },
            colorScheme: {
                light: {
                    tab: {
                        background: 'transparent',
                        hoverBackground: 'transparent',
                        activeBackground: 'transparent'
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
