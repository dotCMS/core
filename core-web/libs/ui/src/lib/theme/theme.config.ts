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
/**
 * Height of a single `p-listbox` option, in pixels: Lara's `0.625rem 1rem` option padding plus
 * the option's line height, at this app's root font size. The preset does not set it — this is
 * what Lara renders — so a PrimeNG or theme upgrade that changes the padding or font needs it
 * re-measured.
 *
 * Exported because virtual scrolling needs the row height as a number: PrimeNG cannot measure
 * it, so `[virtualScrollItemSize]` must be told, and a listbox whose item size disagrees with
 * its rendered rows scrolls wrong. Import this instead of re-measuring per component.
 */
export const LISTBOX_OPTION_HEIGHT = 40.6;

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
        blockui: {
            // A blocked panel is greyed out, not dimmed: the mask is white, so what is behind it
            // fades toward the page rather than darkening.
            //
            // Through `--px-mask-background` rather than `background`: the base rule reads
            // `var(--px-mask-background, dt('mask.background'))`, so this is the hook meant for
            // exactly this — no specificity or layer-order fight with `.p-overlay-mask`, and the
            // shared token stays where it is, which every dialog backdrop also reads.
            css: `
                .p-blockui-mask {
                    --px-mask-background: rgb(255 255 255 / 0.65);
                }
            `
        },
        panel: {
            // A panel whose footer slot is empty still draws the band, because PrimeNG renders it
            // on the template existing rather than on it producing anything. `dot-panel-no-footer`
            // is for a card that offers a footer only some of the time: the template stays put —
            // Panel resolves it once, through a plain `@ContentChild`, and is OnPush — and this
            // takes the empty band out of the layout.
            css: `
                .dot-panel-no-footer .p-panel-footer {
                    display: none;
                }
            `
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
            //
            // The remove icon is flipped to the left of the label app-wide via flexbox
            // `order` (`.p-chip` is `display:flex`): PrimeNG's Chip template always
            // renders the remove icon after the label with no input to reorder it, so
            // this is the only way to achieve it without forking the component. DOM
            // order (and keyboard focus order) is unaffected — only the visual order
            // changes.
            css: `
                .p-chip {
                    height: calc(var(--spacing) * 7); /* 1.75rem */
                    padding: 0 calc(var(--spacing) * 2); /* 0.5rem */
                    font-size: var(--text-xs); /* 0.75rem */
                }
                .p-chip .p-chip-remove-icon {
                    order: -1;
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
        },
        popover: {
            // Popovers are panels app-wide, and what the content area does depends on what is put
            // in it. Plain block markup — a list, a form, a help blurb — gets the panel's inset
            // from here, so no consumer has to restate it. A component dropped straight into a
            // popover is instead the panel itself: a listbox's options have to reach the edges for
            // the hover and selected bands to cover the full row, and a component that lays out a
            // search box above such a list already spaces its own parts. So the inset applies when
            // the content's direct children include a block element, and not when the content is
            // the component.
            //
            // Keying on `> div` rather than naming the components keeps this out of the business of
            // knowing which ones own their spacing: a wrapper component reads the same as the
            // listbox it wraps, which a `> p-listbox` selector would have missed. Checked against
            // all 27 popover consumers; several also pin `p-0` through `pt`, which wins over both
            // rules anyway.
            //
            // `dot-popover-flush` is the opt-out for a panel built from plain markup that still
            // wants its rows to run full bleed — the Content Drive filter panels that carry their
            // own header rows, and the template-builder layout properties panel.
            css: `
                .p-popover {
                    border-radius: var(--radius-lg);
                    overflow: hidden;
                }
                .p-popover .p-popover-content {
                    padding: 0;
                }
                .p-popover .p-popover-content:has(> div) {
                    padding: calc(var(--spacing) * 4); /* 1rem */
                }
                .p-popover.dot-popover-flush .p-popover-content {
                    padding: 0;
                }
            `
        },
        listbox: {
            // Listboxes drop their own chrome (border, radius, shadow) app-wide: they are
            // effectively always rendered inside a container that already provides it — a
            // popover panel, a sidebar, a bubble menu. Option padding, selection colors and
            // checkbox size follow the filter-panel design spec.
            css: `
                .p-listbox {
                    /* The only structural rule: a listbox is always inside something that
                       already draws an edge (popover panel, sidebar, bubble menu), so drawing
                       its own produces a border inside a border. Row height and padding are
                       left to Lara — its 0.625rem/1rem option padding is exactly the 40.6px
                       row these lists have always rendered (see LISTBOX_OPTION_HEIGHT). */
                    border: 0;
                    border-radius: 0;
                    box-shadow: none;

                    /* Design spec: soft grey hover, primary text on the selected row, and a
                       16px checkbox rather than Lara's larger default. */
                    --p-listbox-option-focus-background: var(--p-slate-50);
                    --p-listbox-option-selected-color: var(--p-primary-700);
                    --p-listbox-option-selected-focus-color: var(--p-primary-700);
                    --p-checkbox-width: 16px;
                    --p-checkbox-height: 16px;

                    /* Keep a selected option's background when it also has focus, instead of
                       swapping to the focus background. Points at the SEMANTIC token: the
                       component-level --p-listbox-option-selected-background is only emitted
                       when overridden, so referencing it resolved to empty and flattened the
                       selected row to white. */
                    --p-listbox-option-selected-focus-background: var(
                        --p-list-option-selected-background
                    );
                }
            `
        }
    }
});
