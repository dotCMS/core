import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import {
    DotMessageService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService
} from '@dotcms/data-access';
import { SEO_MEDIA_TYPES, SEO_LIMITS } from '@dotcms/dotcms-models';
import {
    MockDotMessageService,
    seoOGTagsMock,
    seoOGTagsResultMock,
    seoOGTagsResultOgMockTwitter
} from '@dotcms/utils-testing';

import { DotResultsSeoToolComponent } from './dot-results-seo-tool.component';

describe('DotResultsSeoToolComponent', () => {
    let spectator: Spectator<DotResultsSeoToolComponent>;
    const createComponent = createComponentFactory({
        component: DotResultsSeoToolComponent,
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
                        'og:image metatag found, with an appropriate sized image!',
                    'seo.rules.read-more.facebook.learn':
                        'Learn more about <span><a target="_blank" href="https://ogp.me/">The Open Graph Protocol</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.facebook.sharing':
                        'Explore the <span><a target="_blank" href="https://developers.facebook.com/tools/debug/">Sharing Debugger</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.facebook.title':
                        'Ensure that your <code>og:title</code> content is between 55 and 150 characters.',
                    'seo.rules.read-more.facebook.title.unique':
                        'Keep in mind that your <code>og:title</code> content should be unique across your site.',
                    'seo.rules.read-more.facebook.title.sizes':
                        'Optimize your <code>og:image</code> sizes to be under 1200 x 630 pixels.',
                    'seo.rules.read-more.facebook.og-image':
                        'Make sure your <code>og:image</code> file sizes are under 8 MB.',
                    'seo.rules.read-more.facebook.social':
                        'Read more about social media tile <span><a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.twitter.learn':
                        'Learn more about <span><a target="_blank" href="https://developer.twitter.com/en/docs/twitter-for-websites/cards/overview/abouts-cards">About Twitter Cards</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.twitter.suggest':
                        'Suggest using the <code>twitter:card</code> value of <span><a target="_blank" href="https://developer.twitter.com/en/docs/twitter-for-websites/cards/overview/summary-card-with-large-image">summary_large_image</a> <i class="pi pi-external-link"></i></span> for most content.',
                    'seo.rules.read-more.twitter.twitter-title':
                        '<code>twitter:title</code> content should be between 30 and 70 characters.',
                    'seo.rules.read-more.twitter.twitter-title.content':
                        '<code>twitter:title</code> content should be unique across your site.',
                    'seo.rules.read-more.twitter.length':
                        'The length of the description allowed will depend on the reader\'s device size; on the smallest size only about 110 characters are allowed. Longer descriptions will show up with some sort of "read more" or "expand" option.',
                    'seo.rules.read-more.twitter.twitter-image':
                        '<code>twitter:image</code> content should be in JPG, PNG, WEBP, or GIF format.',
                    'seo.rules.read-more.twitter.twitter-image.aspect':
                        '<code>twitter:image</code> content should use a 16:9 aspect ratio, with a max of 1200 x 675 pixels.',
                    'seo.rules.read-more.twitter.twitter-image.content':
                        '<code>twitter:image</code> content should be smaller than 5MB.',
                    'seo.rules.read-more.twitter.twitter-image.social':
                        'Read more about social media tile <span><a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.linkedin.learn':
                        'Learn more about <span><a target="_blank"  href="https://www.linkedin.com/post-inspector/">LinkedIn\'s Post InspectorTool</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.linkedin.meta':
                        'Learn more about <span><a target="_blank" href="https://www.linkedin.com/pulse/meta-tags-getting-them-right-linkedin-evelyn-pei/">Meta Tags: Getting Them Right for LinkedIn</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.linkedin.summary':
                        'Read more about social media tile <span><a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes</a>. <i class="pi pi-external-link"></i></span>',

                    'seo.rules.read-more.google.favicons':
                        'Favicons should be <span><a target="_blank" href="https://favicon.io/">.ico files</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.google.title':
                        'HTML Title content should be between 30 and 60 characters.',
                    'seo.rules.read-more.google.title.unique':
                        'HTML Title content should be unique per page across your site.',
                    'seo.rules.read-more.google.description':
                        'Meta Description tags should be under 160 characters.',
                    'seo.rules.read-more.google.length':
                        'The length of the Description allowed will depend on the reader\'s device size; on the smallest size only about 110 characters are allowed. Longer descriptions will show up with some sort of "read more" or "expand" option.',
                    'seo.rules.read-more.google.meta-tags':
                        '<span><a target="_blank" href="https://ahrefs.com/blog/seo-meta-tags/">Meta Tags for SEO: A Simple Guide for Beginners</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.google.meta-description':
                        '<span><a target="_blank" href="https://moz.com/learn/seo/meta-description">What Are Meta Descriptions And How to Write Them</a>. <i class="pi pi-external-link"></i></span>',
                    'seo.rules.read-more.google.image-sizes':
                        'Read more about social media tile <span><a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes</a>. <i class="pi pi-external-link"></i></span>'
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

    it('should display title', () => {
        const titleElement = spectator.query(byTestId('results-seo-tool-search-title'));
        expect(titleElement.textContent).toContain(seoOGTagsMock.title);
        expect(titleElement.textContent.length).toBeLessThan(SEO_LIMITS.MAX_OG_TITLE_LENGTH);
    });

    it('should display description', () => {
        const titleElement = spectator.query(byTestId('results-seo-tool-search-description'));
        expect(titleElement.textContent).toContain(seoOGTagsMock.description);
        expect(titleElement.textContent.length).toBeLessThan(SEO_LIMITS.MAX_OG_DESCRIPTION_LENGTH);
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
            expect(items[0].key).toEqual(seoOGTagsResultMock[5].key);
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

    it('should render Readmore for Twitter', () => {
        spectator.setInput({
            seoMedia: SEO_MEDIA_TYPES.TWITTER
        });
        spectator.detectChanges();

        const readmore = spectator.queryAll(byTestId('readmore'));
        expect(readmore).toExist();

        expect(readmore[0].innerHTML).toEqual(
            'Learn more about <span><a target="_blank" href="https://developer.twitter.com/en/docs/twitter-for-websites/cards/overview/abouts-cards">About Twitter Cards</a>. <i class="pi pi-external-link"></i></span>'
        );
        expect(readmore[1].innerHTML).toEqual(
            'Suggest using the <code>twitter:card</code> value of <span><a target="_blank" href="https://developer.twitter.com/en/docs/twitter-for-websites/cards/overview/summary-card-with-large-image">summary_large_image</a> <i class="pi pi-external-link"></i></span> for most content.'
        );
        expect(readmore[2].innerHTML).toEqual(
            '<code>twitter:title</code> content should be between 30 and 70 characters.'
        );
        expect(readmore[3].innerHTML).toEqual(
            '<code>twitter:title</code> content should be unique across your site.'
        );
        expect(readmore[4].innerHTML).toEqual(
            'The length of the description allowed will depend on the reader\'s device size; on the smallest size only about 110 characters are allowed. Longer descriptions will show up with some sort of "read more" or "expand" option.'
        );
        expect(readmore[5].innerHTML).toEqual(
            '<code>twitter:image</code> content should be in JPG, PNG, WEBP, or GIF format.'
        );
        expect(readmore[6].innerHTML).toEqual(
            '<code>twitter:image</code> content should use a 16:9 aspect ratio, with a max of 1200 x 675 pixels.'
        );
        expect(readmore[7].innerHTML).toEqual(
            '<code>twitter:image</code> content should be smaller than 5MB.'
        );
        expect(readmore[8].innerHTML).toEqual(
            'Read more about social media tile <span><a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes</a>. <i class="pi pi-external-link"></i></span>'
        );
    });

    it('should render Readmore for Facebook', () => {
        spectator.setInput({
            seoMedia: SEO_MEDIA_TYPES.FACEBOOK
        });
        spectator.detectChanges();

        const readmore = spectator.queryAll(byTestId('readmore'));
        expect(readmore).toExist();

        expect(readmore[0].innerHTML).toEqual(
            'Learn more about <span><a target="_blank" href="https://ogp.me/">The Open Graph Protocol</a>. <i class="pi pi-external-link"></i></span>'
        );
        expect(readmore[1].innerHTML).toEqual(
            'Explore the <span><a target="_blank" href="https://developers.facebook.com/tools/debug/">Sharing Debugger</a>. <i class="pi pi-external-link"></i></span>'
        );
        expect(readmore[2].innerHTML).toEqual(
            'Ensure that your <code>og:title</code> content is between 55 and 150 characters.'
        );
        expect(readmore[3].innerHTML).toEqual(
            'Keep in mind that your <code>og:title</code> content should be unique across your site.'
        );
        expect(readmore[4].innerHTML).toEqual(
            'Optimize your <code>og:image</code> sizes to be under 1200 x 630 pixels.'
        );
        expect(readmore[5].innerHTML).toEqual(
            'Make sure your <code>og:image</code> file sizes are under 8 MB.'
        );
        expect(readmore[6].innerHTML).toEqual(
            'Read more about social media tile <span><a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes</a>. <i class="pi pi-external-link"></i></span>'
        );
    });

    it('should render Readmore for LinkedIn', () => {
        spectator.setInput({
            seoMedia: SEO_MEDIA_TYPES.LINKEDIN
        });
        spectator.detectChanges();

        const readmore = spectator.queryAll(byTestId('readmore'));
        expect(readmore).toExist();

        expect(readmore[0].innerHTML).toEqual(
            'Learn more about <span><a target="_blank" href="https://www.linkedin.com/post-inspector/">LinkedIn\'s Post InspectorTool</a>. <i class="pi pi-external-link"></i></span>'
        );
        expect(readmore[1].innerHTML).toEqual(
            'Learn more about <span><a target="_blank" href="https://www.linkedin.com/pulse/meta-tags-getting-them-right-linkedin-evelyn-pei/">Meta Tags: Getting Them Right for LinkedIn</a>. <i class="pi pi-external-link"></i></span>'
        );
        expect(readmore[2].innerHTML).toEqual(
            'Read more about social media tile <span><a target="_blank" href="https://blog.hootsuite.com/social-media-image-sizes-guide/">image sizes</a>. <i class="pi pi-external-link"></i></span>'
        );
    });

    it('should render Readmore for Google', () => {
        spectator.setInput({
            seoMedia: SEO_MEDIA_TYPES.GOOGLE
        });
        spectator.detectChanges();

        const readmore = spectator.queryAll(byTestId('readmore'));
        expect(readmore).toExist();

        expect(readmore[0].innerHTML).toEqual(
            'Favicons should be <span><a target="_blank" href="https://favicon.io/">.ico files</a>. <i class="pi pi-external-link"></i></span>'
        );
        expect(readmore[1].innerHTML).toEqual(
            'HTML Title content should be between 30 and 60 characters.'
        );
        expect(readmore[2].innerHTML).toEqual(
            'HTML Title content should be unique per page across your site.'
        );
        expect(readmore[3].innerHTML).toEqual(
            'Meta Description tags should be under 160 characters.'
        );
        expect(readmore[4].innerHTML).toEqual(
            'The length of the Description allowed will depend on the reader\'s device size; on the smallest size only about 110 characters are allowed. Longer descriptions will show up with some sort of "read more" or "expand" option.'
        );
        expect(readmore[5].innerHTML).toEqual(
            '<span><a target="_blank" href="https://ahrefs.com/blog/seo-meta-tags/">Meta Tags for SEO: A Simple Guide for Beginners</a>. <i class="pi pi-external-link"></i></span>'
        );
    });

    it('should display the default icon when noFavicon is true', () => {
        const imageElement = spectator.query(byTestId('favicon-image'));
        spectator.dispatchFakeEvent(imageElement, 'error');
        spectator.detectComponentChanges();

        const defaultIcon = spectator.query(byTestId('favicon-default'));
        expect(defaultIcon).toBeTruthy();
        expect(defaultIcon.querySelector('.pi-globe')).toBeTruthy();
    });

    it('should display the favicon image when noFavicon is false', () => {
        spectator.component.seoOGTags.favicon = 'favicon-image-url.png';

        spectator.detectComponentChanges();

        const faviconImage = spectator.query(byTestId('favicon-image'));
        expect(faviconImage).toBeTruthy();
        expect(faviconImage.getAttribute('src')).toBe('favicon-image-url.png');
    });
});
