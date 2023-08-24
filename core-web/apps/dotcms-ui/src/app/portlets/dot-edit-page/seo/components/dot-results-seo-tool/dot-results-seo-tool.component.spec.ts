import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { DotResultsSeoToolComponent } from './dot-results-seo-tool.component';

const seoOGTagsMock = {
    description:
        'Get down to Costa Rica this winter for some of the best surfing int he world. Large winter swell is pushing across the Pacific.',
    language: 'english',
    favicon: 'http://localhost:8080/application/themes/landing-page/img/favicon.ico',
    title: 'A title',
    author: 'dotCMS',
    copyright: 'dotCMS LLC, Miami Florida, US',
    'og:title': 'A title',
    'og:url': 'https://dotcms.com$!{dotPageContent.canonicalUrl}',
    'og:image': 'https://dotcms.com/images/default.png'
};

const seoOGTagsResultMock = [
    {
        key: 'Favicon',
        keyIcon: 'pi-check-circle',
        keyColor: 'var(--color-alert-green)',
        items: [
            { message: 'FavIcon found!', color: 'var(--color-alert-green)', itemIcon: 'pi-check' }
        ],
        sort: 1
    },
    {
        key: 'Description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'var(--color-alert-red)',
        items: [
            {
                message: 'Meta Description not found! Showing Description instead.',
                color: 'var(--color-alert-red)',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2,
        info: "The length of the description allowed will depend on the reader's device size; on the smallest size only about 110 characters are allowed."
    },
    {
        key: 'Title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'var(--color-alert-yellow)',
        items: [
            {
                message: 'HTML Title found, but has fewer than 30 characters of content.',
                color: 'var(--color-alert-yellow)',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 3,
        info: 'HTML Title content should be between 30 and 60 characters.'
    }
];

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
