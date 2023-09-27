import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotResultsSeoToolComponent } from './dot-results-seo-tool.component';
import { seoOGTagsMock, seoOGTagsResultMock, seoOGTagsResultOgMockTwitter } from './mocks';

import { SEO_MEDIA_TYPES } from '../../../content/services/dot-edit-content-html/models/meta-tags-model';
import { DotSeoMetaTagsService } from '../../../content/services/html/dot-seo-meta-tags.service';

describe('DotResultsSeoToolComponent', () => {
    let spectator: Spectator<DotResultsSeoToolComponent>;
    const createComponent = createComponentFactory({
        component: DotResultsSeoToolComponent,
        providers: [
            DotSeoMetaTagsService,
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'seo.rules.favicon.not.found': 'FavIcon not found!',
                    'seo.rules.favicon.more.one.found': 'More than 1 Favicon found!',
                    'seo.rules.favicon.found': 'Favicon found!',
                    'seo.rules.description.found.empty': 'Meta Description found, but is empty!',
                    'seo.rules.description.found': 'Meta Description found!',
                    'seo.rules.title.not.found': 'HTML Title not found!',
                    'seo.rules.title.more.one.found': 'More than 1 HTML Title found!',
                    'seo.rules.title.greater': 'HTML Title found, but has more than 60 characters.',
                    'seo.rules.title.less':
                        'HTML Title found, but has fewer than 30 characters of content.',
                    'seo.rules.title.found':
                        'HTML Title found, with an appropriate amount of content!',
                    'seo.resuls.tool.read.more': 'Read More',
                    'seo.resuls.tool.version': 'Version',
                    'seo.rules.description.info':
                        'The length of the description allowed will depend on the readers device size; on the smallest size only about 110 characters are allowed.',
                    'seo.rules.title.info':
                        'HTML Title content should be between 30 and 60 characters.',
                    'seo.rules.og-title.not.found':
                        'og:title metatag not found! Showing HTML Title instead.',
                    'seo.rules.og-title.more.one.found': 'more than 1 og:title metatag found!',
                    'seo.rules.og-title.more.one.found.empty':
                        'og:title metatag found, but is empty!',
                    'seo.rules.og-title.greater':
                        'og:title metatag found, but has more than 160 characters.',
                    'seo.rules.og-title.less:og':
                        'title metatag found, but has fewer than 30 characters of content.',
                    'seo.rules.og-title.found':
                        'og:title metatag found, with an appropriate amount of content!',
                    'seo.rules.og-image.not.found': 'og:image metatag not found!',
                    'seo.rules.og-image.more.one.found': 'more than 1 og:image metatag found!',
                    'seo.rules.og-image.more.one.found.empty':
                        'og:image metatag found, but is empty!',
                    'seo.rules.og-image.over': 'og:image metatag found, but image is over 8 MB.',
                    'seo.rules.og-image.found':
                        'og:image metatag found, with an appropriate sized image!'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                hostName: 'dotcms.com',
                seoOGTags: seoOGTagsMock,
                seoOGTagsResults: of(seoOGTagsResultMock),
                seoMedia: SEO_MEDIA_TYPES.GOOGLE
            }
        });
    });

    it('should display host Name', () => {
        const hostName = spectator.query(byTestId('page-hostName'));
        expect(hostName).toHaveText(spectator.component.hostName);
    });

    it('should display SEO Tags', () => {
        const tags = spectator.queryAll(byTestId('seo-tag'));
        expect(tags[0]).toContainText('Favicon found!');
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
        spectator.setInput({
            seoMedia: SEO_MEDIA_TYPES.GOOGLE
        });
        spectator.detectChanges();
        const previews = spectator.queryAll(byTestId('seo-preview-mobile'));
        expect(previews[1]).toHaveClass('results-seo-tool__version--small');
    });

    it('should filter seo results by Facebook, seoMedia on changes', (done) => {
        spectator.setInput({
            seoMedia: SEO_MEDIA_TYPES.FACEBOOK
        });
        spectator.detectChanges();
        spectator.component.currentResults$.subscribe((items) => {
            expect(items.length).toEqual(3);
            expect(items[0].key).toEqual(seoOGTagsResultMock[5].key);
            expect(items[1].key).toEqual(seoOGTagsResultMock[3].key);
            expect(items[2].key).toEqual(seoOGTagsResultMock[4].key);
            done();
        });
    });

    it('should filter seo results by Twitter, seoMedia on changes', (done) => {
        spectator.setInput({
            seoMedia: SEO_MEDIA_TYPES.TWITTER
        });
        spectator.detectChanges();
        spectator.component.currentResults$.subscribe((items) => {
            expect(items.length).toEqual(4);
            expect(items[0].key).toEqual(seoOGTagsResultOgMockTwitter[0].key);
            expect(items[1].key).toEqual(seoOGTagsResultOgMockTwitter[1].key);
            expect(items[2].key).toEqual(seoOGTagsResultOgMockTwitter[2].key);
            expect(items[3].key).toEqual(seoOGTagsResultOgMockTwitter[3].key);
            done();
        });
    });

    it('should filter seo results by Linkedin seoMedia on changes', (done) => {
        spectator.setInput({
            seoMedia: SEO_MEDIA_TYPES.LINKEDIN
        });
        spectator.detectChanges();
        spectator.component.currentResults$.subscribe((items) => {
            expect(items.length).toEqual(3);
            expect(items[0].key).toEqual(seoOGTagsResultMock[1].key);
            expect(items[1].key).toEqual(seoOGTagsResultMock[3].key);
            expect(items[2].key).toEqual(seoOGTagsResultMock[4].key);
            done();
        });
    });

    it('should render the result card title with title case', () => {
        const expectedTitle = 'Title';
        const expectedDescription = 'Description';

        const resultKeyTitle = spectator.queryAll(byTestId('result-key'))[2];
        const resultKeyDescription = spectator.queryAll(byTestId('result-key'))[1];

        expect(resultKeyTitle).toExist();
        expect(resultKeyDescription).toExist();
        expect(resultKeyTitle).toContainText(expectedTitle);
        expect(resultKeyDescription).toContainText(expectedDescription);
    });
});
