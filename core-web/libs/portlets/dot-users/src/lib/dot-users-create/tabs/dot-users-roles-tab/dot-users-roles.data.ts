/**
 * Mock catalog used purely for visual iteration on the shuttle. The
 * shape mirrors the fields the design references (`id`, `name`,
 * `group`, `desc`, optional `parent`), so the swap to the real
 * `/api/v1/roles` payload becomes a data-source change without
 * touching the component.
 */
export interface DotUsersRoleOption {
    id: string;
    name: string;
    group: string;
    description: string;
    parent?: string;
}

export const DOT_USERS_ROLE_GROUPS = [
    'System',
    'Publisher / Legal',
    'Intranet',
    'Categories'
] as const;

export type DotUsersRoleGroup = (typeof DOT_USERS_ROLE_GROUPS)[number];

export const DOT_USERS_MOCK_ROLES: DotUsersRoleOption[] = [
    { id: 'login_as', name: 'Login As', group: 'System', description: 'Impersonate other users' },
    {
        id: 'cms_admin',
        name: 'CMS Administrator',
        group: 'System',
        description: 'Full administrative access'
    },
    { id: 'backend', name: 'Back-end User', group: 'System', description: 'Access the admin UI' },
    {
        id: 'scripting',
        name: 'Scripting User',
        group: 'System',
        description: 'Run server-side Velocity'
    },
    {
        id: 'frontend',
        name: 'Front-end User',
        group: 'System',
        description: 'Authenticate on the front-end'
    },
    {
        id: 'publisher',
        name: 'Publisher',
        group: 'Publisher / Legal',
        description: 'Publish & push content'
    },
    {
        id: 'pub_staging',
        name: 'Staging Publisher',
        group: 'Publisher / Legal',
        parent: 'publisher',
        description: 'Publish to staging'
    },
    {
        id: 'pub_prod',
        name: 'Production Publisher',
        group: 'Publisher / Legal',
        parent: 'publisher',
        description: 'Push to production'
    },
    {
        id: 'legal',
        name: 'Legal Reviewer',
        group: 'Publisher / Legal',
        description: 'Approve legal workflow'
    },
    {
        id: 'editor',
        name: 'Content Editor',
        group: 'Intranet',
        description: 'Create & edit content'
    },
    {
        id: 'editor_news',
        name: 'News Editor',
        group: 'Intranet',
        parent: 'editor',
        description: 'Edit news content'
    },
    {
        id: 'editor_blog',
        name: 'Blog Editor',
        group: 'Intranet',
        parent: 'editor',
        description: 'Edit blog content'
    },
    {
        id: 'reviewer',
        name: 'Reviewer',
        group: 'Intranet',
        description: 'Review submissions'
    },
    {
        id: 'contributor',
        name: 'Contributor',
        group: 'Intranet',
        description: 'Submit drafts for review'
    },
    {
        id: 'cat_manager',
        name: 'Category Manager',
        group: 'Categories',
        description: 'Manage taxonomy'
    },
    {
        id: 'cat_products',
        name: 'Products Taxonomy',
        group: 'Categories',
        parent: 'cat_manager',
        description: 'Manage product categories'
    },
    {
        id: 'cat_regions',
        name: 'Regions Taxonomy',
        group: 'Categories',
        parent: 'cat_manager',
        description: 'Manage region categories'
    },
    {
        id: 'cat_viewer',
        name: 'Category Viewer',
        group: 'Categories',
        description: 'Read-only taxonomy'
    }
];
