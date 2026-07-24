import { Injectable } from '@angular/core';

import { A11yGroup } from '../models/a11y-groups';

/** Attribute tagging marker elements so we can find + clear them. */
const MARKER_ATTR = 'data-a11y-marker';
/** Id of the single overlay layer the markers live in. */
const LAYER_ID = 'dot-a11y-marker-layer';

/** Outline colors matching the preview-pane legend. */
const COLOR = {
    error: { outline: '#f59e0b', fill: 'rgba(245,158,11,.12)' }, // detected
    warning: { outline: '#dc2626', fill: 'rgba(220,38,38,.10)' } // needs attention
};

/**
 * Draws accessibility violation markers *inside* a same-origin preview iframe.
 *
 * For each axe finding we resolve its CSS `selector` against the iframe document
 * and append an absolutely-positioned highlight box into the iframe's own body —
 * so markers scroll with the page content for free (no parent-side reposition).
 *
 * The iframe must be same-origin (it is: the preview loads through the
 * `/dot-page` dev proxy / the BE origin in prod). If `contentDocument` is null
 * (cross-origin / not yet loaded) the methods no-op safely.
 */
@Injectable()
export class A11yMarkerService {
    /** Remove any markers previously injected, then draw one per flagged element. */
    render(iframe: HTMLIFrameElement | null | undefined, groups: A11yGroup[]): void {
        const doc = this.getDocument(iframe);
        if (!doc?.body) {
            return;
        }

        const layer = this.ensureLayer(doc);
        layer.replaceChildren();

        for (const group of groups) {
            const palette = COLOR[group.type];
            for (const item of group.items) {
                const el = this.safeQuery(doc, item.selector);
                if (!el || this.isRootLevel(el)) {
                    // Skip <html>/<body> and page-spanning roots — outlining the
                    // whole page is noise, not a useful marker.
                    continue;
                }
                const marker = this.buildMarker(doc, el, group, palette);
                if (marker) {
                    layer.appendChild(marker);
                }
            }
        }
    }

    /** Remove all injected markers from the iframe (e.g. before a re-scan). */
    clear(iframe: HTMLIFrameElement | null | undefined): void {
        const doc = this.getDocument(iframe);
        if (doc) {
            this.clearIn(doc);
        }
    }

    private buildMarker(
        doc: Document,
        el: Element,
        group: A11yGroup,
        palette: { outline: string; fill: string }
    ): HTMLElement | null {
        const rect = el.getBoundingClientRect();
        // Skip elements that aren't laid out yet (0×0) — re-render on next load.
        if (rect.width === 0 && rect.height === 0) {
            return null;
        }

        const win = doc.defaultView;
        // getBoundingClientRect() is viewport-relative; add the page scroll to get
        // document-space coordinates. The layer's containing block is the initial
        // containing block (it's on <html>), so these align regardless of body margin.
        const scrollX = win?.scrollX ?? doc.documentElement.scrollLeft;
        const scrollY = win?.scrollY ?? doc.documentElement.scrollTop;

        const marker = doc.createElement('div');
        marker.setAttribute(MARKER_ATTR, group.code);
        marker.title = `${group.code} (${group.impact ?? group.type})`;
        Object.assign(marker.style, {
            position: 'absolute',
            top: `${rect.top + scrollY}px`,
            left: `${rect.left + scrollX}px`,
            width: `${rect.width}px`,
            height: `${rect.height}px`,
            boxSizing: 'border-box',
            border: `2px solid ${palette.outline}`,
            borderRadius: '3px',
            background: palette.fill,
            pointerEvents: 'none'
        });
        return marker;
    }

    /**
     * The single full-document overlay layer the markers live in. Positioned on
     * <html> so its containing block is the initial containing block — document
     * coordinates map 1:1, unaffected by body margins/padding.
     */
    private ensureLayer(doc: Document): HTMLElement {
        let layer = doc.getElementById(LAYER_ID);
        if (!layer) {
            layer = doc.createElement('div');
            layer.id = LAYER_ID;
            Object.assign(layer.style, {
                position: 'absolute',
                top: '0',
                left: '0',
                width: '0',
                height: '0',
                pointerEvents: 'none',
                zIndex: '2147483646'
            });
            doc.documentElement.appendChild(layer);
        }
        return layer;
    }

    /** True for <html>/<body> — page-spanning roots we don't want to outline. */
    private isRootLevel(el: Element): boolean {
        const tag = el.tagName?.toLowerCase();
        return tag === 'html' || tag === 'body';
    }

    private clearIn(doc: Document): void {
        doc.getElementById(LAYER_ID)?.remove();
    }

    /** Access the iframe document; null when cross-origin or not yet loaded. */
    private getDocument(iframe: HTMLIFrameElement | null | undefined): Document | null {
        try {
            return iframe?.contentDocument ?? null;
        } catch {
            // Cross-origin access throws — markers are unavailable in that case.
            return null;
        }
    }

    /** axe selectors can be exotic; guard against invalid querySelector input. */
    private safeQuery(doc: Document, selector: string): Element | null {
        if (!selector) {
            return null;
        }
        try {
            return doc.querySelector(selector);
        } catch {
            return null;
        }
    }
}
