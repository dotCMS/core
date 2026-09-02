/**
 * Footer resources constants
 */
export const FOOTER_RESOURCES = [
    {
        icon: 'pi pi-file-import',
        title: 'starter.footer.link.documentation.title',
        description: 'starter.footer.link.documentation.description',
        link: 'https://dotcms.com/docs/latest/',
        dataTestId: 'starter.footer.link.documentation',
        sortOrder: 1
    },
    {
        icon: 'pi pi-code',
        title: 'starter.footer.link.examples.title',
        description: 'starter.footer.link.examples.description',
        link: 'https://dotcms.com/codeshare/',
        dataTestId: 'starter.footer.link.examples',
        sortOrder: 2
    },
    {
        icon: 'pi pi-users',
        title: 'starter.footer.link.community.title',
        description: 'starter.footer.link.community.description',
        link: 'https://dotcms.com/forum/',
        dataTestId: 'starter.footer.link.community',
        sortOrder: 3
    },
    {
        icon: 'pi pi-video',
        title: 'starter.footer.link.training.title',
        description: 'starter.footer.link.training.description',
        link: 'https://dotcms.com/courses/',
        dataTestId: 'starter.footer.link.training',
        sortOrder: 4
    },
    {
        icon: 'pi pi-star',
        title: 'starter.footer.link.review.title',
        description: 'starter.footer.link.review.description',
        link: 'https://dotcms.com/review/',
        dataTestId: 'starter.footer.link.review',
        sortOrder: 5
    },
    {
        icon: 'pi pi-comment',
        title: 'starter.footer.link.feedback.title',
        description: 'starter.footer.link.feedback.description',
        link: 'https://dotcms.com/contact-us/',
        dataTestId: 'starter.footer.link.feedback',
        sortOrder: 6
    }
].sort((a, b) => a.sortOrder - b.sortOrder);

/**
 * API and services constants
 */
export const API_AND_SERVICES = [
    {
        link: 'https://dotcms.com/docs/latest/graphql',
        dataTestId: 'starter.side.link.graphQl',
        title: 'starter.side.link.graphQl.title',
        description: 'starter.side.link.graphQl.description',
        sortOrder: 1
    },
    {
        title: 'starter.side.link.content.title',
        description: 'starter.side.link.content.description',
        link: 'https://dotcms.com/docs/latest/content-api',
        dataTestId: 'starter.side.link.content',
        sortOrder: 2
    },
    {
        title: 'starter.side.link.image.processing.title',
        description: 'starter.side.link.image.processing.description',
        link: 'https://dotcms.com/docs/latest/image-resizing-and-processing',
        dataTestId: 'starter.side.link.image.processing',
        sortOrder: 3
    },
    {
        link: 'https://dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas',
        dataTestId: 'starter.side.link.page.layout',
        title: 'starter.side.link.page.layout.title',
        description: 'starter.side.link.page.layout.description',
        sortOrder: 4
    },
    {
        link: 'https://dotcms.com/docs/latest/rest-api-authentication#APIToken',
        dataTestId: 'starter.side.link.generate.key',
        title: 'starter.side.link.generate.key.title',
        description: 'starter.side.link.generate.key.description',
        sortOrder: 5
    }
].sort((a, b) => a.sortOrder - b.sortOrder);
