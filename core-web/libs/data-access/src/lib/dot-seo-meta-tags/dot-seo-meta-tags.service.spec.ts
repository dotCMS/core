import { expect, it } from '@jest/globals';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IMG_NOT_FOUND_KEY } from '@dotcms/dotcms-models';
import { seoOGTagsResultOgMock } from '@dotcms/utils-testing';

import { DotSeoMetaTagsService } from './dot-seo-meta-tags.service';

import { DotMessageService } from '../dot-alert-confirm/dot-alert-confirm.service';
import { DotSeoMetaTagsUtilService } from '../dot-seo-meta-tags-utils/dot-seo-meta-tags-util.service';
import { DotUploadService } from '../dot-upload/dot-upload.service';

function createTestDocument(): XMLDocument {
    return document.implementation.createDocument('http://www.w3.org/1999/xhtml', 'html', null);
}

describe('DotSetMetaTagsService', () => {
    let service: DotSeoMetaTagsService;
    let serviceUtil: DotSeoMetaTagsUtilService;
    let testDoc: XMLDocument;
    let head: HTMLElement;
    let getImageFileSizeSpy: jest.SpyInstance;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotSeoMetaTagsService,
                DotSeoMetaTagsUtilService,
                {
                    provide: DotMessageService,
                    useValue: {
                        get: (key: string) => key // Lang properties value can change but not the key.
                    }
                },
                DotUploadService
            ]
        });
        service = TestBed.inject(DotSeoMetaTagsService);
        serviceUtil = TestBed.inject(DotSeoMetaTagsUtilService);
        getImageFileSizeSpy = jest.spyOn(serviceUtil, 'getImageFileSize').mockReturnValue(
            of({
                length: 8000000,
                url: 'https://www.dotcms.com/dA/4e870b9fe0/1200w/jpeg/70/dotcms-defualt-og.jpg'
            })
        );

        testDoc = createTestDocument();

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
            expect(value[5].items[0].message).toEqual('seo.rules.og-description.more.one.found');
            expect(value[5].items[1].message).toEqual('seo.rules.og-description.less');
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
            expect(value[2].items[0].message).toEqual('seo.rules.og-title.more.one.found');
            expect(value[2].items[1].message).toEqual('seo.rules.og-title.less');
            done();
        });
    });

    it('should get more than description error', (done) => {
        const description = document.createElement('meta');
        description.setAttribute('name', 'description');
        description.setAttribute('content', 'Costa Rica Special Offer');

        testDoc.head.appendChild(description);

        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value[0].items[0].message).toEqual('seo.rules.description.more.one.found');
            done();
        });
    });

    it('should get description found', (done) => {
        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value[0].items[0].message).toEqual('seo.rules.description.found');
            done();
        });
    });

    it('should og:description meta tag, and Meta Description not found!', (done) => {
        const testDoc: XMLDocument = createTestDocument();

        service.getMetaTagsResults(testDoc).subscribe((value) => {
            expect(value[5].items[0].message).toEqual(
                'seo.rules.og-description.description.not.found'
            );
            done();
        });
    });

    it('should og:image meta tag not found!', (done) => {
        const imageDocument: XMLDocument = createTestDocument();

        const head = imageDocument.createElement('head');
        imageDocument.documentElement.appendChild(head);

        const ogImage = imageDocument.createElement('og:image');
        imageDocument.documentElement.appendChild(ogImage);
        head.appendChild(ogImage);

        getImageFileSizeSpy.mockReturnValueOnce(
            of({
                length: 0,
                url: IMG_NOT_FOUND_KEY
            })
        );

        service.getMetaTagsResults(imageDocument).subscribe((value) => {
            expect(value[1].items[0].message).toEqual('seo.rules.og-image.not.found');
            done();
        });
    });

    it('should og:image meta tag not found!', (done) => {
        const descriptionDocument: XMLDocument = createTestDocument();

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
            expect(value[5].items[0].message).toEqual('seo.rules.og-description.greater');
            done();
        });
    });

    it('should found title meta tag, with an appropriate amount of content!', (done) => {
        const titleDoc: XMLDocument = createTestDocument();

        const head = titleDoc.createElement('head');
        titleDoc.documentElement.appendChild(head);

        const title = titleDoc.createElement('title');
        title.innerHTML = 'HTML TITLE -------------- TEST';
        head.appendChild(title);

        service.getMetaTagsResults(titleDoc).subscribe((value) => {
            expect(value[4].items[0].message).toEqual('seo.rules.title.found.empty');
            done();
        });
    });

    it('should found title meta tag, with an appropriate amount of content!', (done) => {
        const titleDoc: XMLDocument = createTestDocument();

        const head = titleDoc.createElement('head');
        titleDoc.documentElement.appendChild(head);

        const title = titleDoc.createElement('title');
        title.innerHTML = 'HTML TITLE -------------- TEST';
        head.appendChild(title);

        service.getMetaTagsResults(titleDoc).subscribe((value) => {
            expect(value[4].items[0].message).toEqual('seo.rules.title.found.empty');
            done();
        });
    });

    it('should found title meta tag, with an appropriate amount of content when min limit', (done) => {
        const titleDoc: XMLDocument = createTestDocument();

        const head = titleDoc.createElement('head');
        titleDoc.documentElement.appendChild(head);

        const title = titleDoc.createElement('title');
        title.innerHTML = 'HTML TITLE -------------- TEST';
        head.appendChild(title);

        service.getMetaTagsResults(titleDoc).subscribe((value) => {
            expect(value[4].items[0].message).toEqual('seo.rules.title.found.empty');
            done();
        });
    });

    it('should found title meta tag, with an appropriate amount of content when max limit', (done) => {
        const titleDoc: XMLDocument = createTestDocument();

        const head = titleDoc.createElement('head');
        titleDoc.documentElement.appendChild(head);

        const title = titleDoc.createElement('title');
        title.innerHTML = 'HTML TITLE -------------- TEST******************************';
        head.appendChild(title);

        service.getMetaTagsResults(titleDoc).subscribe((value) => {
            expect(value[4].items[0].message).toEqual('seo.rules.title.found.empty');
            done();
        });
    });

    it('should found description meta tag, with an appropriate amount of content when min limit', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = doc.createElement('meta');
        metaDesc.name = 'description';
        metaDesc.content = 'DESCRIPTION ****TEST.Lorem ipsum dolor sit amet.-------';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[0].items[0].message).toEqual('seo.rules.description.found');
            done();
        });
    });

    it('should found description meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = doc.createElement('meta');
        metaDesc.name = 'description';
        metaDesc.content =
            'DESCRIPTION ****TEST.Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ante metus, posuere quis posuere eu, varius nec ante. Aenean nec';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[0].items[0].message).toEqual('seo.rules.description.found');
            done();
        });
    });

    it('should found og:title meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaTitle = doc.createElement('meta');
        metaTitle.name = 'og:title';
        metaTitle.content = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.****';
        head.appendChild(metaTitle);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[2].items[0].message).toEqual('seo.rules.og-title.found');
            done();
        });
    });

    it('should found og:title meta tag, with an appropriate amount of content when min limit', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaTitle = doc.createElement('meta');
        metaTitle.name = 'og:title';
        metaTitle.content = 'Lorem ipsum dolor sit amet****';
        head.appendChild(metaTitle);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[2].items[0].message).toEqual('seo.rules.og-title.found');
            done();
        });
    });

    it('should found og:description meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = doc.createElement('meta');
        metaDesc.name = 'og:description';
        metaDesc.content = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[5].items[0].message).toEqual('seo.rules.og-description.found');
            done();
        });
    });

    it('should found og:description meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = doc.createElement('meta');
        metaDesc.name = 'og:description';
        metaDesc.content =
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ante metus, posuere quis posuere eu, varius nec ante. Aenean nec dictum purus.**********';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[5].items[0].message).toEqual('seo.rules.og-description.found');
            done();
        });
    });

    it('should found twitter:description meta tag, with an appropriate amount of content when min limit', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const twitterDesc = doc.createElement('meta');
        twitterDesc.name = 'twitter:description';
        twitterDesc.content =
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ante metus, posuere quis posuere eu, varius nec ante. Aenean nec dictum purus. Nullam rhoncus velit mauris, vel fringilla purus mollis ege';
        head.appendChild(twitterDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[8].items[0].message).toEqual('seo.rules.twitter-card-description.found');
            done();
        });
    });

    it('should found twitter:description meta tag, with an appropriate amount of content when max limit', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const twitterDesc = doc.createElement('meta');
        twitterDesc.name = 'twitter:description';
        twitterDesc.content = 'Lorem ipsum dolor sit amettest';
        head.appendChild(twitterDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[8].items[0].message).toEqual('seo.rules.twitter-card-description.found');
            done();
        });
    });

    it('should found twitter:description meta tag not found! Showing Description instead.', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const metaDesc = document.createElement('meta');
        metaDesc.name = 'description';
        metaDesc.content =
            'Get down to Costa Rica this winter for some of the best surfing int he world. Large winter swell is pushing across the Pacific.';
        head.appendChild(metaDesc);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[8].items[0].message).toEqual(
                'seo.rules.twitter-card-description.not.found'
            );
            done();
        });
    });

    it('should found twitter:description meta tag', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[8].items[0].message).toEqual(
                'seo.rules.twitter-card-description.description.not.found'
            );
            done();
        });
    });

    it('should found twitter:title meta tag not found and HTML Title not found!', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[7].items[0].message).toEqual(
                'seo.rules.twitter-card-title.title.not.found'
            );
            done();
        });
    });

    it('should found twitter:title meta tag, Showing HTML Title instead.', (done) => {
        const doc: XMLDocument = createTestDocument();

        const head = doc.createElement('head');
        doc.documentElement.appendChild(head);

        const title = document.createElement('title');
        title.innerText = 'Costa Rica Special Offer';
        head.appendChild(title);

        service.getMetaTagsResults(doc).subscribe((value) => {
            expect(value[7].items[0].message).toEqual('seo.rules.twitter-card-title.not.found');
            done();
        });
    });
});
