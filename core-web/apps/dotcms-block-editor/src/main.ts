import 'zone.js';
import { enableProdMode } from '@angular/core';
import { createApplication } from '@angular/platform-browser';
import { createCustomElement } from '@angular/elements';

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

        if (!environment.production) {
            wireDevDemo();
        }
    })
    .catch((err) => console.error(err));

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
