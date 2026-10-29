import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/vitest';
import { Observable } from 'rxjs';

import { Component, input, reflectComponentType, signal } from '@angular/core';

import { SeoMetaTags, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotResultsSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { seoOGTagsMock, seoOGTagsResultMock, seoOGTagsResultOgMock } from '@dotcms/utils-testing';

import { DotUveSeoResultsComponent } from './dot-uve-seo-results.component';

import { UVEStore } from '../../../store/dot-uve.store';

/** The SEO results tool has its own spec; here it only shows what it was given. */
@Component({ selector: 'dot-results-seo-tool', template: '' })
class DotResultsSeoToolStubComponent {
    readonly seoMedia = input<string>();
    readonly seoOGTags = input<SeoMetaTags>();
    readonly seoOGTagsResults = input<Observable<SeoMetaTagsResult[]>>();
}

/** The part of `UVEStore` the SEO results read. */
function createUVEStoreMock() {
    return {
        viewSocialMedia: signal<string | null>('facebook'),
        editorOgTags: signal<SeoMetaTags | null>(seoOGTagsMock),
        viewOgTagsResults: signal<SeoMetaTagsResult[] | null>(seoOGTagsResultMock)
    };
}

describe('DotUveSeoResultsComponent', () => {
    let spectator: Spectator<DotUveSeoResultsComponent>;
    let store: ReturnType<typeof createUVEStoreMock>;

    const createComponent = createComponentFactory({
        component: DotUveSeoResultsComponent,
        overrideComponents: [
            [
                DotUveSeoResultsComponent,
                {
                    remove: { imports: [DotResultsSeoToolComponent] },
                    add: { imports: [DotResultsSeoToolStubComponent] }
                }
            ]
        ],
        providers: [{ provide: UVEStore, useFactory: () => store }],
        detectChanges: false
    });

    const tool = () => spectator.query(DotResultsSeoToolStubComponent);

    beforeEach(() => {
        store = createUVEStoreMock();
        spectator = createComponent();
    });

    it('should have no inputs or outputs', () => {
        const mirror = reflectComponentType(DotUveSeoResultsComponent);

        expect(mirror?.inputs).toEqual([]);
        expect(mirror?.outputs).toEqual([]);
    });

    it("should show the chosen social media's results for the page's og tags", () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('results-seo-tool'))).toBeTruthy();
        expect(tool()?.seoMedia()).toBe('facebook');
        expect(tool()?.seoOGTags()).toEqual(seoOGTagsMock);
    });

    it('should hand the tool the og tag results the store holds, as they change', () => {
        const received: SeoMetaTagsResult[][] = [];

        spectator.detectChanges();
        tool()
            ?.seoOGTagsResults()
            ?.subscribe((results) => received.push(results));

        store.viewOgTagsResults.set(seoOGTagsResultOgMock);
        spectator.detectChanges();

        expect(received).toEqual([seoOGTagsResultMock, seoOGTagsResultOgMock]);
    });

    it('should show nothing while no social-media preview is chosen', () => {
        store.viewSocialMedia.set(null);
        spectator.detectChanges();

        expect(spectator.query(byTestId('results-seo-tool'))).toBeNull();
    });

    it("should show nothing until the page's og tags are read", () => {
        store.editorOgTags.set(null);
        spectator.detectChanges();

        expect(spectator.query(byTestId('results-seo-tool'))).toBeNull();
    });
});
