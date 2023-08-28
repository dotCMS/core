import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';

import { DotSeoMetaTagsService } from './dot-seo-meta-tags.service';

describe('DotSetMetaTagsService', () => {
    let service: DotSeoMetaTagsService;
    let testDoc: Document;
    let head: HTMLElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotSeoMetaTagsService, DotMessageService]
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

        const linkCanonical = document.createElement('link');
        linkCanonical.rel = 'icon';
        linkCanonical.href = 'https://dotcms.com/img/favicon';

        head = testDoc.createElement('head');
        testDoc.documentElement.appendChild(head);
        head.appendChild(linkCanonical);
        head.appendChild(metaDesc);
        head.appendChild(title);
    });

    it('should returns the meta tags elements map in the object', () => {
        const titleList = testDoc.querySelectorAll('title');
        const faviconList = testDoc.querySelectorAll('link[rel="icon"]');

        expect(service.getMetaTags(testDoc)).toEqual({
            title: 'Costa Rica Special Offer',
            description:
                'Get down to Costa Rica this winter for some of the best surfing int he world. Large winter swell is pushing across the Pacific.',
            favicon: 'https://dotcms.com/img/favicon',
            faviconElements: faviconList,
            titleElements: titleList
        });
    });
});
