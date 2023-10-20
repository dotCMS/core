import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSeoMetaTagsUtilService } from './dot-seo-meta-tags-util.service';
import { DotSeoMetaTagsService } from './dot-seo-meta-tags.service';

import { seoOGTagsResultOgMock } from '../../../seo/components/dot-results-seo-tool/mocks';
import { IMG_NOT_FOUND_KEY } from '../dot-edit-content-html/models/meta-tags-model';

describe('DotSetMetaTagsService', () => {
    let service: DotSeoMetaTagsService;
    let serviceUtil: DotSeoMetaTagsUtilService;
    let testDoc: Document;
    let head: HTMLElement;
    let getImageFileSizeSpy;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotSeoMetaTagsService,
                DotSeoMetaTagsUtilService,
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'seo.rules.favicon.not.found':
                            'Favicon not found, or image link in Favicon not valid!',
                        'seo.rules.favicon.more.one.found': 'More than 1 Favicon found!',
                        'seo.rules.favicon.found': 'Favicon found!',
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
                        'seo.rules.og-title.not.found':
                            'og:title meta tag not found! Showing HTML Title instead.',
                        'seo.rules.og-title.more.one.found': 'more than 1 og:title meta tag found!',
                        'seo.rules.og-title.more.one.found.empty':
                            'og:title meta tag found, but is empty!',
                        'seo.rules.og-title.greater':
                            'og:title meta tag found, but has more than 160 characters.',
                        'seo.rules.og-title.less':
                            'title meta tag found, but has fewer than 30 characters of content.',
                        'seo.rules.og-title.found':
                            'og:title meta tag found, with an appropriate amount of content!',
                        'seo.rules.og-image.not.found': 'og:image meta tag not found!',
                        'seo.rules.og-image.more.one.found': 'more than 1 og:image meta tag found!',
                        'seo.rules.og-image.more.one.found.empty':
                            'og:image meta tag found, but is empty!',
                        'seo.rules.og-image.over':
                            'og:image meta tag found, but image is over 8 MB.',
                        'seo.rules.og-image.found':
                            'og:image meta tag found, with an appropriate sized image!',
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
                            'twitter:image meta tag found, but image is over 5 MB.',
                        'seo.rules.og-description.more.one.found':
                            'more than 1 og:description meta tag found!',
                        'seo.rules.og-description.found.empty':
                            'og:description meta tag found, but is empty!',
                        'seo.rules.og-description.not.found':
                            'og:description meta tag not found! Showing Meta Description instead.',
                        'seo.rules.og-description.greater':
                            'og:description meta tag found, but has more than 150 characters.',
                        'seo.rules.og-description.less':
                            'og:description meta tag found, but has fewer than 55 characters of content.',
                        'seo.rules.og-description.found':
                            'og:description meta tag with valid content found!',
                        'seo.rules.description.more.one.found':
                            'More than 1 Meta Description found!',
                        'seo.rules.og-description.description.not.found':
                            'og:description meta tag, and Meta Description not found!'
                    })
                },
                DotUploadService
            ]
        });
        service = TestBed.inject(DotSeoMetaTagsService);
        serviceUtil = TestBed.inject(DotSeoMetaTagsUtilService);
        getImageFileSizeSpy = spyOn(serviceUtil, 'getImageFileSize').and.returnValue(
            of({
                length: 8000000,
                url: 'https://www.dotcms.com/dA/4e870b9fe0/1200w/jpeg/70/dotcms-defualt-og.jpg'
            })
        );

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
        const imageOgElements = testDoc.querySelectorAll('meta[property="og:image"]');
        const twitterCardElements = testDoc.querySelectorAll('meta[name="twitter:card"]');
        const twitterTitleElements = testDoc.querySelectorAll('meta[name="twitter:title"]');
        const twitterImageElements = testDoc.querySelectorAll('meta[name="twitter:image"]');
        const twitterDescriptionElements = testDoc.querySelectorAll(
            'meta[name="twitter:description"]'
        );
        const descriptionElements = testDoc.querySelectorAll('meta[name="description"]');
        const descriptionOgElements = testDoc.querySelectorAll('meta[property="og:description"]');

        expect(serviceUtil.getMetaTags(testDoc)).toEqual({
            title: 'Costa Rica Special Offer',
            description:
                'Get down to Costa Rica this winter for some of the best surfing int he world. Large winter swell is pushing across the Pacific.',
            favicon: 'https://dotcms.com/img/favicon',
            'og:title': 'Costa Rica Special Offer',
            'og:image': 'https://www.dotcms.com/dA/4e870b9fe0/1200w/jpeg/70/dotcms-defualt-og.jpg',
            faviconElements: faviconList,
            titleElements: titleList,
            titleOgElements,
            imageOgElements,
            twitterCardElements,
            twitterTitleElements,
            twitterImageElements,
            twitterDescriptionElements,
            descriptionOgElements,
            descriptionElements
        });
    });

    it('should get the result found for ogTags with the async call', (done) => {
        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value.length).toEqual(10);
            expect(value).toEqual(seoOGTagsResultOgMock);
            done();
        });
    });

    it('should get more than one og-description error', (done) => {
        const ogMetaDescription = document.createElement('meta');
        ogMetaDescription.setAttribute('property', 'og:description');
        ogMetaDescription.setAttribute('content', 'BE');

        const ogMetaDescriptionSecond = document.createElement('meta');
        ogMetaDescriptionSecond.setAttribute('property', 'og:description');
        ogMetaDescriptionSecond.setAttribute('content', 'Costa Rica Special Offer');

        testDoc.head.appendChild(ogMetaDescription);
        testDoc.head.appendChild(ogMetaDescriptionSecond);

        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value[5].items[0].message).toEqual(
                'more than 1 <code>og:description</code> meta tag found!'
            );
            expect(value[5].items[1].message).toEqual(
                '<code>og:description</code> meta tag found, but has fewer than 55 characters of content.'
            );
            done();
        });
    });

    it('should get more than one og:title error', (done) => {
        const ogMetaTitle = document.createElement('meta');
        ogMetaTitle.setAttribute('property', 'og:title');
        ogMetaTitle.setAttribute('content', 'Costa Rica Special Offer');

        const ogMetaTitleSecond = document.createElement('meta');
        ogMetaTitleSecond.setAttribute('property', 'og:title');
        ogMetaTitleSecond.setAttribute('content', 'Costa Rica Special Offer');

        testDoc.head.appendChild(ogMetaTitle);
        testDoc.head.appendChild(ogMetaTitleSecond);

        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value[2].items[0].message).toEqual(
                'more than 1 <code>og:title</code> meta tag found!'
            );
            expect(value[2].items[1].message).toEqual(
                'title meta tag found, but has fewer than 30 characters of content.'
            );
            done();
        });
    });

    it('should get more than description error', (done) => {
        const description = document.createElement('meta');
        description.setAttribute('name', 'description');
        description.setAttribute('content', 'Costa Rica Special Offer');

        testDoc.head.appendChild(description);

        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value[0].items[0].message).toEqual('More than 1 Meta Description found!');
            done();
        });
    });

    it('should get description found', (done) => {
        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value[0].items[0].message).toEqual('Meta Description found!');
            done();
        });
    });

    it('should og:description meta tag, and Meta Description not found!', (done) => {
        const testDoc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value[5].items[0].message).toEqual(
                '<code>og:description</code> meta tag, and Meta Description not found!'
            );
            done();
        });
    });

    it('should og:image meta tag not found!', (done) => {
        const imageDocument: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = imageDocument.createElement('head');
        imageDocument.documentElement.appendChild(head);

        const ogImage = imageDocument.createElement('og:image');
        imageDocument.documentElement.appendChild(ogImage);
        head.appendChild(ogImage);

        getImageFileSizeSpy.and.callFake(() => {
            return of({
                length: 0,
                url: IMG_NOT_FOUND_KEY
            });
        });

        service.getMetaTagsResults(imageDocument).subscribe((value) => {
            expect(value[1].items[0].message).toEqual('<code>og:image</code> meta tag not found!');
            done();
        });
    });

    it('should og:image meta tag not found!', (done) => {
        const descriptionDocument: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = descriptionDocument.createElement('head');
        descriptionDocument.documentElement.appendChild(head);

        const ogDescription = descriptionDocument.createElement('meta');
        ogDescription.setAttribute('property', 'og:description');
        ogDescription.setAttribute(
            'content',
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla pharetra maximus enim ac tincidunt. Vivamus vestibulum sed enim sed consectetur. Nulla malesuada libero a tristique bibendum. Suspendisse blandit ligula velit, eu volutpat arcu ornare sed.'
        );
        head.appendChild(ogDescription);

        service.getMetaTagsResults(descriptionDocument).subscribe((value) => {
            expect(value[5].items[0].message).toEqual(
                '<code>og:description</code> meta tag found, but has more than 150 characters.'
            );
            done();
        });
    });

    it('should found title meta tag, with an appropriate amount of content!', (done) => {
        const titleDoc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = titleDoc.createElement('head');
        titleDoc.documentElement.appendChild(head);

        const title = titleDoc.createElement('title');
        title.innerHTML = 'HTML TITLE -------------- TEST';
        head.appendChild(title);

        service.getMetaTagsResults(titleDoc).subscribe((value) => {
            expect(value[4].items[0].message).toEqual(
                'HTML Title found, with an appropriate amount of content!'
            );
            done();
        });
    });

    it('should found title meta tag, with an appropriate amount of content!', (done) => {
        const titleDoc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = titleDoc.createElement('head');
        titleDoc.documentElement.appendChild(head);

        const title = titleDoc.createElement('title');
        title.innerHTML = 'HTML TITLE -------------- TEST';
        head.appendChild(title);

        service.getMetaTagsResults(titleDoc).subscribe((value) => {
            expect(value[4].items[0].message).toEqual(
                'HTML Title found, with an appropriate amount of content!'
            );
            done();
        });
    });

    it('should found title meta tag, with an appropriate amount of content when min limit', (done) => {
        const titleDoc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = titleDoc.createElement('head');
        titleDoc.documentElement.appendChild(head);

        const title = titleDoc.createElement('title');
        title.innerHTML = 'HTML TITLE -------------- TEST';
        head.appendChild(title);

        service.getMetaTagsResults(titleDoc).subscribe((value) => {
            expect(value[4].items[0].message).toEqual(
                'HTML Title found, with an appropriate amount of content!'
            );
            done();
        });
    });

    it('should found title meta tag, with an appropriate amount of content when max limit', (done) => {
        const titleDoc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = titleDoc.createElement('head');
        titleDoc.documentElement.appendChild(head);

        const title = titleDoc.createElement('title');
        title.innerHTML = 'HTML TITLE -------------- TEST******************************';
        head.appendChild(title);

        service.getMetaTagsResults(titleDoc).subscribe((value) => {
            expect(value[4].items[0].message).toEqual(
                'HTML Title found, with an appropriate amount of content!'
            );
            done();
        });
    });

    it('should found description meta tag, with an appropriate amount of content when min limit', (done) => {
        const doc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = doc.createElement('meta');
        metaDesc.name = 'description';
        metaDesc.content = 'DESCRIPTION ****TEST.Lorem ipsum dolor sit amet.-------';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[0].items[0].message).toEqual('Meta Description found!');
            done();
        });
    });

    it('should found description meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = doc.createElement('meta');
        metaDesc.name = 'description';
        metaDesc.content =
            'DESCRIPTION ****TEST.Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ante metus, posuere quis posuere eu, varius nec ante. Aenean nec dictum purus';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[0].items[0].message).toEqual('Meta Description found!');
            done();
        });
    });

    it('should found og:title meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaTitle = doc.createElement('meta');
        metaTitle.name = 'og:title';
        metaTitle.content = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.****';
        head.appendChild(metaTitle);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[2].items[0].message).toEqual(
                '<code>og:title</code> meta tag found, with an appropriate amount of content!'
            );
            done();
        });
    });

    it('should found og:title meta tag, with an appropriate amount of content when min limit', (done) => {
        const doc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaTitle = doc.createElement('meta');
        metaTitle.name = 'og:title';
        metaTitle.content = 'Lorem ipsum dolor sit amet****';
        head.appendChild(metaTitle);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[2].items[0].message).toEqual(
                '<code>og:title</code> meta tag found, with an appropriate amount of content!'
            );
            done();
        });
    });

    it('should found og:description meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = doc.createElement('meta');
        metaDesc.name = 'og:description';
        metaDesc.content = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[5].items[0].message).toEqual(
                '<code>og:description</code> meta tag with valid content found!'
            );
            done();
        });
    });

    it('should found og:description meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = doc.createElement('meta');
        metaDesc.name = 'og:description';
        metaDesc.content =
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ante metus, posuere quis posuere eu, varius nec ante. Aenean nec dictum purus.**********';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[5].items[0].message).toEqual(
                '<code>og:description</code> meta tag with valid content found!'
            );
            done();
        });
    });

    it('should found twitter:description meta tag, with an appropriate amount of content when min limit', (done) => {
        const doc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const twitterDesc = doc.createElement('meta');
        twitterDesc.name = 'twitter:description';
        twitterDesc.content =
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ante metus, posuere quis posuere eu, varius nec ante. Aenean nec dictum purus. Nullam rhoncus velit mauris, vel fringilla purus mollis ege';
        head.appendChild(twitterDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[8].items[0].message).toEqual(
                '<code>twitter:description</code> meta tag with valid content found!'
            );
            done();
        });
    });

    it('should found twitter:description meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: Document = document.implementation.createDocument(
            'http://www.w3.org/1999/xhtml',
            'html',
            null
        );

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const twitterDesc = doc.createElement('meta');
        twitterDesc.name = 'twitter:description';
        twitterDesc.content = 'Lorem ipsum dolor sit amettest';
        head.appendChild(twitterDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[8].items[0].message).toEqual(
                '<code>twitter:description</code> meta tag with valid content found!'
            );
            done();
        });
    });
});
