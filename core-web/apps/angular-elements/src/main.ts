import { createCustomElement } from '@angular/elements';
import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';
import { DotCounterComponent } from './app/components/dot-counter/dot-counter.component';

bootstrapApplication(AppComponent)
    .then((app) => {
        const myCounterButton = createCustomElement(DotCounterComponent, {
            injector: app.injector
        });

        customElements.define('wc-counter', myCounterButton);
    })
    .catch((err) => console.error(err));
