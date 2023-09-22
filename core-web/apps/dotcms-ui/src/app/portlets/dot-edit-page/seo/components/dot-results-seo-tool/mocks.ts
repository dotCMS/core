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

const seoOGTagsResultOgMockTwitter = [
    {
        key: 'twitter:card',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">twitter:card</span> meta tag not found!',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 1,
        info: ''
    },
    {
        key: 'twitter:title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">twitter:title</span> meta tag not found! Showing HTML Title instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2,
        info: ''
    },
    {
        key: 'twitter:description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">twitter:description</span> meta tag not found! Showing Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 3,
        info: ''
    },
    {
        key: 'twitter:image',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">og:image</span> metatag not found!',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 4,
        info: ''
    }
];

const seoOGTagsResultMock = [
    {
        key: 'Favicon',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message: 'Favicon found!',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            }
        ],
        sort: 1
    },
    {
        key: 'Description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'Meta Description not found! Showing Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2,
        info: "The length of the description allowed will depend on the reader's device size; on the smallest size only about 110 characters are allowed."
    },
    {
        key: 'Title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-yellow',
        items: [
            {
                message: 'HTML Title found, but has fewer than 30 characters of content.',
                color: 'results-seo-tool__result-icon--alert-yellow',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 3,
        info: 'HTML Title content should be between 30 and 60 characters.'
    },
    {
        key: 'Og:title',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">og:title</span> metatag found, with an appropriate amount of content!',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 4,
        info: 'HTML Title content should be between 30 and 60 characters.'
    },
    {
        key: 'Og:image',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">og:image</span> metatag found, with an appropriate sized image!',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 5,
        info: 'HTML Title content should be between 30 and 60 characters.'
    },
    {
        key: 'Og:description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'Meta Description not found! Showing Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2,
        info: "The length of the description allowed will depend on the reader's device size; on the smallest size only about 110 characters are allowed."
    },
    ...seoOGTagsResultOgMockTwitter
];

const seoOGTagsResultOgMock = [
    {
        key: 'description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">og:description</span> meta tag not found! Showing Meta Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2,
        info: "The length of the description allowed will depend on the reader's device size; on the smallest size only about 110 characters are allowed."
    },
    {
        key: 'og:image',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">og:image</span> metatag found, with an appropriate sized image!',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            }
        ],
        sort: 6,
        info: ''
    },
    {
        key: 'og:title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-yellow',
        items: [
            {
                message: 'title metatag found, but has fewer than 30 characters of content.',
                color: 'results-seo-tool__result-icon--alert-yellow',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 5,
        info: 'HTML Title content should be between 30 and 60 characters.'
    },
    {
        key: 'favicon',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message: 'Favicon found!',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            }
        ],
        sort: 1,
        info: ''
    },
    {
        key: 'title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-yellow',
        items: [
            {
                message: 'HTML Title found, but has fewer than 30 characters of content.',
                color: 'results-seo-tool__result-icon--alert-yellow',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 4,
        info: 'HTML Title content should be between 30 and 60 characters.'
    },
    {
        key: 'og:description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">og:description</span> meta tag not found! Showing Meta Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 3,
        info: "The length of the description allowed will depend on the reader's device size; on the smallest size only about 110 characters are allowed."
    },
    {
        key: 'twitter:card',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">twitter:card</span> meta tag not found!',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 1,
        info: ''
    },
    {
        key: 'twitter:title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">twitter:title</span> meta tag not found! Showing HTML Title instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2,
        info: ''
    },
    {
        key: 'twitter:description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">twitter:description</span> meta tag not found! Showing Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 3,
        info: ''
    },
    {
        key: 'twitter:image',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<span class="results-seo-tool__result-tag">twitter:image</span> meta tag not found!',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            },
            {
                message:
                    '<span class="results-seo-tool__result-tag">twitter:image</span> meta tag found, but image is over 5 MB.',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            }
        ],
        sort: 4,
        info: ''
    }
];

export { seoOGTagsMock, seoOGTagsResultMock, seoOGTagsResultOgMock, seoOGTagsResultOgMockTwitter };
