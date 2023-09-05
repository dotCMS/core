import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { DotResultsSeoToolComponent } from './dot-results-seo-tool.component';
import { seoOGTagsMock, seoOGTagsResultMock } from './mocks';

describe('DotResultsSeoToolComponent', () => {
    let spectator: Spectator<DotResultsSeoToolComponent>;
    const createComponent = createComponentFactory(DotResultsSeoToolComponent);

    beforeEach(() => {
        spectator = createComponent({
            props: {
                hostName: 'A title',
                seoOGTags: seoOGTagsMock,
                seoOGTagsResults: seoOGTagsResultMock
            }
        });
    });

    it('should display title', () => {
        const title = spectator.query(byTestId('page-title'));
        expect(title).toHaveText(spectator.component.hostName);
    });

    it('should display SEO Tags', () => {
        const tags = spectator.queryAll(byTestId('seo-tag'));
        expect(tags[0]).toContainText('FavIcon found!');
        expect(tags[1]).toContainText('Meta Description not found! Showing Description instead');
        expect(tags[2]).toContainText(
            'HTML Title found, but has fewer than 30 characters of content.'
        );
    });

    it('should display Key tags', () => {
        const keys = spectator.queryAll(byTestId('result-key'));
        expect(keys[0]).toContainText(seoOGTagsResultMock[0].key);
        expect(keys[1]).toContainText(seoOGTagsResultMock[1].key);
        expect(keys[2]).toContainText(seoOGTagsResultMock[2].key);
    });

    it('should display Key tags', () => {
        const keys = spectator.queryAll(byTestId('result-key'));
        expect(keys[0]).toContainText(seoOGTagsResultMock[0].key);
        expect(keys[1]).toContainText(seoOGTagsResultMock[1].key);
        expect(keys[2]).toContainText(seoOGTagsResultMock[2].key);
    });

    it('should display mobile size for the preview', () => {
        const previews = spectator.queryAll(byTestId('seo-preview'));
        expect(previews[1]).toHaveClass('results-seo-tool__version--small');
    });
});
