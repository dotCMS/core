import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import {
    DotToolsCatalogEntry,
    DotToolsSection,
    DotToolsSectionForm,
    DotToolsToolForm
} from '../models/dot-tools.models';

/**
 * WAITING FOR BACKEND — the endpoints this service wraps are defined in #37353
 * but not yet implemented. Method signatures match the pinned contract; only
 * the mocked bodies swap for real HTTP calls once the backend lands.
 *
 * Endpoints, per #37353:
 *   getSections    → GET /v1/layouts
 *   getCatalog     → GET /v1/portlet
 *   createSection  → POST /v1/layouts
 *   updateSection  → PUT /v1/layouts/{id}
 *   deleteSection  → DELETE /v1/layouts/{id}
 *   reorderSections → PUT /v1/layouts/_reorder
 *   setSectionTools → PUT /v1/layouts/{id}/portlets
 *   createCustomTool / updateCustomTool → POST | PUT /v1/portlet/custom
 *   deleteCustomTool → DELETE /v1/portlet/portletId/{id} (path still under
 *     discussion in the spike; see the comment on #37352)
 *
 * The mock bodies here just echo the inputs back so the UI can render an
 * optimistic update while we wait for the real endpoints.
 */
@Injectable({ providedIn: 'root' })
export class DotToolsService {
    getSections(): Observable<DotToolsSection[]> {
        return of(MOCK_SECTIONS);
    }

    getCatalog(): Observable<DotToolsCatalogEntry[]> {
        return of(MOCK_CATALOG);
    }

    createSection(form: DotToolsSectionForm): Observable<DotToolsSection> {
        return of({
            id: slugify(form.name),
            name: form.name,
            icon: form.icon,
            tabOrder: Number.MAX_SAFE_INTEGER,
            portletIds: []
        });
    }

    updateSection(id: string, form: DotToolsSectionForm): Observable<DotToolsSection> {
        return of({
            id,
            name: form.name,
            icon: form.icon,
            tabOrder: 0,
            portletIds: []
        });
    }

    deleteSection(_id: string): Observable<void> {
        return of(undefined);
    }

    reorderSections(_orderedIds: string[]): Observable<void> {
        return of(undefined);
    }

    setSectionTools(_sectionId: string, _portletIds: string[]): Observable<void> {
        return of(undefined);
    }

    createCustomTool(form: DotToolsToolForm): Observable<DotToolsCatalogEntry> {
        return of({
            id: normalizeCustomId(form.portletId),
            title: form.portletName,
            isCustom: true
        });
    }

    updateCustomTool(form: DotToolsToolForm): Observable<DotToolsCatalogEntry> {
        return of({
            id: normalizeCustomId(form.portletId),
            title: form.portletName,
            isCustom: true
        });
    }

    deleteCustomTool(_id: string): Observable<void> {
        return of(undefined);
    }
}

function slugify(value: string): string {
    return value
        .toLowerCase()
        .trim()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '');
}

// Custom portlet ids in the backend are prefixed with `c_` (see the design's
// `c_press-releases` etc.). The dialog collects a plain id; this normalizer
// applies the prefix so the mock output matches the shape real writes will
// return once the backend lands.
function normalizeCustomId(id: string): string {
    const slug = slugify(id);

    return slug.startsWith('c_') ? slug : `c_${slug}`;
}

// Fixtures derived from the design's DATA/LIB arrays (Tools.dc.html:399-433).
// Kept here so the store can render against realistic shapes until the backend
// endpoints in #37353 land. Delete this block when swapping to real HTTP.
const MOCK_SECTIONS: DotToolsSection[] = [
    {
        id: 'getting-started',
        name: 'Getting Started',
        icon: 'rocket_launch',
        tabOrder: 0,
        portletIds: ['welcome']
    },
    {
        id: 'site',
        name: 'Site',
        icon: 'language',
        tabOrder: 1,
        portletIds: ['pages', 'browser', 'templates', 'containers', 'publishing-queue']
    },
    {
        id: 'content',
        name: 'Content',
        icon: 'article',
        tabOrder: 2,
        portletIds: [
            'search',
            'blogs',
            'call-to-action',
            'events',
            'products',
            'content-drive',
            'c_press-releases',
            'c_product-catalog'
        ]
    },
    {
        id: 'digital-assets',
        name: 'Digital Assets',
        icon: 'perm_media',
        tabOrder: 3,
        portletIds: ['images', 'videos', 'documents']
    },
    {
        id: 'marketing',
        name: 'Marketing',
        icon: 'campaign',
        tabOrder: 4,
        portletIds: ['personas', 'vanity-urls', 'rules', 'usage']
    },
    {
        id: 'content-model',
        name: 'Content Model',
        icon: 'schema',
        tabOrder: 5,
        portletIds: [
            'content-types',
            'tag-manager',
            'categories',
            'workflows',
            'locales',
            'language-variables'
        ]
    },
    {
        id: 'dev-tools',
        name: 'Dev Tools',
        icon: 'terminal',
        tabOrder: 6,
        portletIds: [
            'es-search',
            'query-tool',
            'graphql',
            'velocity-playground',
            'api-playground',
            'dot-ai'
        ]
    },
    {
        id: 'settings',
        name: 'Settings',
        icon: 'settings',
        tabOrder: 7,
        portletIds: [
            'apps',
            'configuration',
            'maintenance',
            'plugins',
            'roles',
            'sites',
            'site-search',
            'users'
        ]
    }
];

const MOCK_CATALOG: DotToolsCatalogEntry[] = [
    { id: 'apps', title: 'Apps', isCustom: false },
    { id: 'blogs', title: 'Blogs', isCustom: false },
    { id: 'browser', title: 'Browser', isCustom: false },
    { id: 'call-to-action', title: 'Call To Action', isCustom: false },
    { id: 'categories', title: 'Categories', isCustom: false },
    { id: 'configuration', title: 'Configuration', isCustom: false },
    { id: 'containers', title: 'Containers', isCustom: false },
    { id: 'content-drive', title: 'Content Drive', isCustom: false },
    { id: 'content-types', title: 'Content Types', isCustom: false },
    { id: 'documents', title: 'Documents', isCustom: false },
    { id: 'dot-ai', title: 'dotAI', isCustom: false },
    { id: 'es-search', title: 'ES Search', isCustom: false },
    { id: 'events', title: 'Events', isCustom: false },
    { id: 'graphql', title: 'GraphQL', isCustom: false },
    { id: 'images', title: 'Images', isCustom: false },
    { id: 'language-variables', title: 'Language Variables', isCustom: false },
    { id: 'locales', title: 'Locales', isCustom: false },
    { id: 'maintenance', title: 'Maintenance', isCustom: false },
    { id: 'pages', title: 'Pages', isCustom: false },
    { id: 'personas', title: 'Personas', isCustom: false },
    { id: 'plugins', title: 'Plugins', isCustom: false },
    { id: 'products', title: 'Products', isCustom: false },
    { id: 'publishing-queue', title: 'Publishing Queue', isCustom: false },
    { id: 'roles', title: 'Roles & Tools', isCustom: false },
    { id: 'rules', title: 'Rules', isCustom: false },
    { id: 'search', title: 'Search', isCustom: false },
    { id: 'sites', title: 'Sites', isCustom: false },
    { id: 'site-search', title: 'Site Search', isCustom: false },
    { id: 'tag-manager', title: 'Tag Manager', isCustom: false },
    { id: 'templates', title: 'Templates', isCustom: false },
    { id: 'usage', title: 'Usage', isCustom: false },
    { id: 'users', title: 'Users', isCustom: false },
    { id: 'vanity-urls', title: 'Vanity URLs', isCustom: false },
    { id: 'velocity-playground', title: 'Velocity Playground', isCustom: false },
    { id: 'videos', title: 'Videos', isCustom: false },
    { id: 'welcome', title: 'Welcome', isCustom: false },
    { id: 'workflows', title: 'Workflows', isCustom: false },
    { id: 'c_press-releases', title: 'Press Releases', isCustom: true },
    { id: 'c_product-catalog', title: 'Product Catalog', isCustom: true },
    { id: 'c_campaign-briefs', title: 'Campaign Briefs', isCustom: true },
    { id: 'c_legal-docs', title: 'Legal Docs', isCustom: true }
];
