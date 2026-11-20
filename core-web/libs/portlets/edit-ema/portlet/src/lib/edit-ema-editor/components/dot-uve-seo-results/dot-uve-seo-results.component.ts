import { Component, inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';

import { DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';

import { UVEStore } from '../../../store/dot-uve.store';

/**
 * Shows how the page looks when shared on the social media the user picks in the device selector,
 * built from the og tags the canvas reads off the page.
 */
@Component({
    selector: 'dot-uve-seo-results',
    imports: [DotResultsSeoToolComponent],
    templateUrl: './dot-uve-seo-results.component.html',
    host: { class: 'contents' }
})
export class DotUveSeoResultsComponent {
    protected readonly uveStore = inject(UVEStore);

    // The results tool takes its og tag results as a stream.
    protected readonly ogTagsResults$ = toObservable(this.uveStore.viewOgTagsResults);
}
