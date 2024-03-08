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
        title: 'card',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: '<code>twitter:card</code> meta tag not found!',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 1
    },
    {
        key: 'twitter:title',
        title: 'title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<code>twitter:title</code> meta tag not found! Showing HTML Title instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2
    },
    {
        key: 'twitter:description',
        title: 'description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message:
                    '<code>twitter:description</code> meta tag not found! Showing Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 3
    },
    {
        key: 'twitter:image',
        title: 'image',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: '<code>og:image</code> metatag not found!',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 4
    }
];

const seoOGTagsResultMock = [
    {
        key: 'Favicon',
        title: 'favicon',
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
        title: 'Description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'Meta Description not found! Showing Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2
    },
    {
        key: 'Title',
        title: 'Title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-yellow',
        items: [
            {
                message: 'HTML Title found, but has fewer than 30 characters of content.',
                color: 'results-seo-tool__result-icon--alert-yellow',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 3
    },
    {
        key: 'Og:title',
        title: 'title',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message:
                    '<code>og:title</code> metatag found, with an appropriate amount of content!',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 4
    },
    {
        key: 'Og:image',
        title: 'image',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message: '<code>og:image</code> metatag found, with an appropriate sized image!',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 5
    },
    {
        key: 'Og:description',
        title: 'Description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'Meta Description not found! Showing Description instead.',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2
    },
    ...seoOGTagsResultOgMockTwitter
];

const seoOGTagsResultOgMock = [
    {
        key: 'description',
        title: 'description',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message: 'seo.rules.description.found',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            }
        ],
        sort: 3
    },
    {
        key: 'og:image',
        title: 'image',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message: 'seo.rules.og-image.found',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            }
        ],
        sort: 6
    },
    {
        key: 'og:title',
        title: 'title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-yellow',
        items: [
            {
                message: 'seo.rules.og-title.less',
                color: 'results-seo-tool__result-icon--alert-yellow',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 2
    },
    {
        key: 'favicon',
        title: 'favicon',
        keyIcon: 'pi-check-circle',
        keyColor: 'results-seo-tool__result-icon--alert-green',
        items: [
            {
                message: 'seo.rules.favicon.found',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            }
        ],
        sort: 1
    },
    {
        key: 'title',
        title: 'title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-yellow',
        items: [
            {
                message: 'seo.rules.title.less',
                color: 'results-seo-tool__result-icon--alert-yellow',
                itemIcon: 'pi-exclamation-circle'
            }
        ],
        sort: 2
    },
    {
        key: 'og:description',
        title: 'description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'seo.rules.og-description.not.found',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 4
    },
    {
        key: 'twitter:card',
        title: 'card',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'seo.rules.twitter-card.not.found',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 2
    },
    {
        key: 'twitter:title',
        title: 'title',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'seo.rules.twitter-card-title.not.found',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 1
    },
    {
        key: 'twitter:description',
        title: 'description',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'seo.rules.twitter-card-description.not.found',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            }
        ],
        sort: 3
    },
    {
        key: 'twitter:image',
        title: 'image',
        keyIcon: 'pi-exclamation-triangle',
        keyColor: 'results-seo-tool__result-icon--alert-red',
        items: [
            {
                message: 'seo.rules.twitter-image.found',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            },
            {
                message: 'seo.rules.twitter-image.not.found',
                color: 'results-seo-tool__result-icon--alert-red',
                itemIcon: 'pi-times'
            },
            {
                message: 'seo.rules.twitter-image.over',
                color: 'results-seo-tool__result-icon--alert-green',
                itemIcon: 'pi-check'
            }
        ],
        sort: 4
    }
];

export { seoOGTagsMock, seoOGTagsResultMock, seoOGTagsResultOgMock, seoOGTagsResultOgMockTwitter };
