import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSeoMetaTagsService } from './dot-seo-meta-tags.service';

import { seoOGTagsResultOgMock } from '../../../seo/components/dot-results-seo-tool/mocks';

describe('DotSetMetaTagsService', () => {
    let service: DotSeoMetaTagsService;
    let testDoc: Document;
    let head: HTMLElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotSeoMetaTagsService,
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'seo.rules.favicon.not.found': 'FavIcon not found!',
                        'seo.rules.favicon.more.one.found': 'More than 1 FavIcon found!',
                        'seo.rules.favicon.found': 'FavIcon found!',
                        'seo.rules.description.not.found':
                            'Meta Description not found! Showing Description instead.',
                        'seo.rules.description.found.empty':
                            'Meta Description found, but is empty!',
                        'seo.rules.description.found': 'Meta Description found!',
                        'seo.rules.title.not.found': 'HTML Title not found!',
                        'seo.rules.title.more.one.found': 'More than 1 HTML Title found!',
                        'seo.rules.title.greater':
                            'HTML Title found, but has more than 60 characters.',
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
                        'seo.rules.og-image.over':
                            'og:image metatag found, but image is over 8 MB.',
                        'seo.rules.og-image.found':
                            'og:image metatag found, with an appropriate sized image!',
                        'seo.rules.twitter-card.not.found': 'twitter:card meta tag not found!',
                        'seo.rules.twitter-card.more.one.found':
                            'more than 1 twitter:card meta tag found!',
                        'seo.rules.twitter-card.more.one.found.empty':
                            'twitter:card meta tag found, but is empty!',
                        'seo.rules.twitter-card.found': 'twitter:card meta tag found!',
                        'seo.rules.twitter-card-title.not.found':
                            'twitter:title meta tag not found! Showing HTML Title instead.',
                        'seo.rules.twitter-card-title.more.one.found':
                            'more than 1 twitter:title meta tag found!',
                        'seo.rules.twitter-card-title.more.one.found.empty':
                            'twitter:title meta tag found, but is empty!',
                        'seo.rules.twitter-card.title.greater':
                            'twitter:title meta tag found, but has more than 70 characters.',
                        'seo.rules.twitter-card.title.less=twitter':
                            'title meta tag found, but has fewer than 30 characters of content.',
                        'seo.rules.twitter-card-title.found': 'twitter:title meta tag found!',
                        'seo.rules.twitter-card-description.not.found':
                            'twitter:description meta tag not found! Showing Description instead.',
                        'seo.rules.twitter-card-description.more.one.found':
                            'more than 1 twitter:description meta tag found!',
                        'seo.rules.twitter-card-description.more.one.found.empty':
                            'twitter:description meta tag found, but is empty!',
                        'seo.rules.twitter-card-description.found':
                            'twitter:description meta tag with valid content found!',
                        'seo.rules.twitter-card-description.greater':
                            'twitter:description meta tag found, but has more than 200 characters.',
                        'seo.rules.twitter-image.not.found': 'twitter:image meta tag not found!',
                        'seo.rules.twitter-image.more.one.found':
                            'more than 1 twitter:image meta tag found!',
                        'seo.rules.twitter-image.more.one.found.empty':
                            'twitter:image meta tag found, but is empty!',
                        'seo.rules.twitter-image.found':
                            'twitter:image meta tag found, with an appropriate sized image!',
                        'seo.rules.twitter-image.over':
                            'twitter:image meta tag found, but image is over 5 MB.'
                    })
                },
                DotUploadService
            ]
        });
        service = TestBed.inject(DotSeoMetaTagsService);

        testDoc = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const title = document.createElement('title');
        title.innerText = 'Costa Rica Special Offer';

        const metaDesc = document.createElement('meta');
        metaDesc.name = 'description';
        metaDesc.content =
            'Get down to Costa Rica this winter for some of the best surfing int he world. Large winter swell is pushing across the Pacific.';

        const ogMetaTitle = document.createElement('meta');
        ogMetaTitle.setAttribute('property', 'og:title');
        ogMetaTitle.setAttribute('content', 'Costa Rica Special Offer');

        const ogMetaImage = document.createElement('meta');
        ogMetaImage.setAttribute('property', 'og:image');
        ogMetaImage.setAttribute(
            'content',
            'https://www.dotcms.com/dA/4e870b9fe0/1200w/jpeg/70/dotcms-defualt-og.jpg'
        );

        const linkCanonical = document.createElement('link');
        linkCanonical.rel = 'icon';
        linkCanonical.href = 'https://dotcms.com/img/favicon';

        head = testDoc.createElement('head');
        testDoc.documentElement.appendChild(head);
        head.appendChild(linkCanonical);
        head.appendChild(metaDesc);
        head.appendChild(title);
        head.appendChild(ogMetaImage);
        head.appendChild(ogMetaTitle);
    });

    it('should returns the meta tags elements map in the object', () => {
        const titleList = testDoc.querySelectorAll('title');
        const faviconList = testDoc.querySelectorAll('link[rel="icon"]');
        const titleOgElements = testDoc.querySelectorAll('meta[property="og:title"]');
        const imagesOgElements = testDoc.querySelectorAll('meta[property="og:image"]');

        expect(service.getMetaTags(testDoc)).toEqual({
            title: 'Costa Rica Special Offer',
            description:
                'Get down to Costa Rica this winter for some of the best surfing int he world. Large winter swell is pushing across the Pacific.',
            favicon: 'https://dotcms.com/img/favicon',
            faviconElements: faviconList,
            titleElements: titleList,
            titleOgElements: titleOgElements,
            imageOgElements: imagesOgElements,
            'og:title': 'Costa Rica Special Offer',
            'og:image': 'https://www.dotcms.com/dA/4e870b9fe0/1200w/jpeg/70/dotcms-defualt-og.jpg'
        });
    });

    it('should get the result found for ogTags with the async call', () => {
        spyOn(service, 'getImageFileSize').and.returnValue(
            of({
                length: 8000000,
                url: 'https://www.dotcms.com/dA/4e870b9fe0/1200w/jpeg/70/dotcms-defualt-og.jpg'
            })
        );

        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value.length).toEqual(6);
            expect(value).toEqual(seoOGTagsResultOgMock);
        });
    });
});
