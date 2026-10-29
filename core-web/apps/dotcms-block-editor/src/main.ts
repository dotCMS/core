import 'zone.js';
import { enableProdMode } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { createApplication } from '@angular/platform-browser';

import { DotBlockEditorComponent } from '@dotcms/block-editor';
import { DotCMSEditorComponent } from '@dotcms/new-block-editor';

import { appConfig } from './app/app.config';
import { EDITOR_DEMO_CONTENT } from './app/editor-demo-content';
import { environment } from './environments/environment';

if (environment.production) {
    enableProdMode();
}

createApplication(appConfig)
    .then((appRef) => {
        if (!customElements.get('dotcms-block-editor')) {
            const element = createCustomElement(DotCMSEditorComponent, {
                injector: appRef.injector
            });
            customElements.define('dotcms-block-editor', element);
        }

        // Rollback target for the new editor. Registered while `FEATURE_FLAG_NEW_BLOCK_EDITOR=false`
        // (default) so customers can opt out of the TipTap-v3 editor. Remove this block — and the
        // legacy `@dotcms/block-editor` import — once the new editor exits QA.
        if (!customElements.get('dotcms-old-block-editor')) {
            const element = createCustomElement(DotBlockEditorComponent, {
                injector: appRef.injector
            });
            customElements.define('dotcms-old-block-editor', element);
        }

        document.addEventListener('contentletEdit', forwardContentletEdit, true);

        if (!environment.production) {
            wireDevDemo();
        }
    })
    .catch((err) => console.error(err));

/** The two tags this bundle registers, the only elements whose `contentletEdit` is ours. */
const EDITOR_TAGS = new Set(['DOTCMS-BLOCK-EDITOR', 'DOTCMS-OLD-BLOCK-EDITOR']);

/**
 * Hands "edit this embedded contentlet" from either editor to the legacy editor's panel (#38303):
 * the panel listens for `ng-event` on this document and opens the contentlet in a stacked panel,
 * so the author never leaves the content they are editing.
 *
 * Registered on `document` because the JSP creates the editor elements, not this bundle, so there
 * is no instance to subscribe to here. Registered for the capture phase because Angular Elements
 * re-dispatches an output as `new CustomEvent(name, { detail })` on the element, which does not
 * bubble: only a capturing listener above the element sees it.
 */
function forwardContentletEdit(event: Event): void {
    const target = event.target as Element | null;
    if (!target || !EDITOR_TAGS.has(target.tagName)) return;

    // The new editor sends its whole edit event; the old one only these two keys.
    const { inode, contentType } = (event as CustomEvent<{ inode: string; contentType: string }>)
        .detail;

    document.dispatchEvent(
        new CustomEvent('ng-event', {
            detail: { name: 'edit-contentlet', data: { inode, contentType } }
        })
    );
}

/**
 * Local dev only: when `index.html` includes `<dotcms-block-editor id="demo">`, prefill it with
 * sample content so `nx serve dotcms-block-editor` shows a working editor. The JSP host page
 * never renders an element with `id="demo"`, so this is a no-op in production.
 */
function wireDevDemo(): void {
    const demo = document.getElementById('demo') as (HTMLElement & { value?: string }) | null;
    if (!demo) return;
    demo.value = EDITOR_DEMO_CONTENT;
    demo.addEventListener('valueChange', (event) => {
        // eslint-disable-next-line no-console
        console.debug('[dev demo] valueChange', (event as CustomEvent).detail);
    });
}
