import { TreeNode } from 'primeng/api';


/**
 * Mock tree structure for file navigation
 */
export const MOCK_FOLDERS: TreeNode[] = [
    {
        key: 'blog',
        label: 'Blog',
        icon: 'pi pi-folder'
    },
    {
        key: 'travelbot',
        label: 'TravelBot',
        icon: 'pi pi-folder',
        children: [
            {
                key: 'activities',
                label: 'activities',
                icon: 'pi pi-folder'
            }
        ]
    },
    {
        key: 'application',
        label: 'application',
        icon: 'pi pi-folder',
        children: [
            {
                key: 'apivtl',
                label: 'apivtl',
                icon: 'pi pi-folder'
            },
            {
                key: 'block-editor',
                label: 'block-editor',
                icon: 'pi pi-folder'
            },
            {
                key: 'containers',
                label: 'containers',
                icon: 'pi pi-folder'
            },
            {
                key: 'templates',
                label: 'templates',
                icon: 'pi pi-folder'
            },
            {
                key: 'themes',
                label: 'themes',
                icon: 'pi pi-folder',
                children: [
                    {
                        key: 'landing-page',
                        label: 'landing-page',
                        icon: 'pi pi-folder',
                        children: [
                            {
                                key: 'containers',
                                label: 'containers',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'css',
                                label: 'css',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'fonts',
                                label: 'fonts',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'img',
                                label: 'img',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'js',
                                label: 'js',
                                icon: 'pi pi-folder'
                            }
                        ]
                    },
                    {
                        key: 'travel',
                        label: 'travel',
                        icon: 'pi pi-folder',
                        children: [
                            {
                                key: 'css',
                                label: 'css',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'images',
                                label: 'images',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'js',
                                label: 'js',
                                icon: 'pi pi-folder'
                            }
                        ]
                    },
                    {
                        key: 'vtl',
                        label: 'vtl',
                        icon: 'pi pi-folder',
                        children: [
                            {
                                key: 'activities',
                                label: 'activities',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'blog',
                                label: 'blog',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'breadcrumbs',
                                label: 'breadcrumbs',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'carousel',
                                label: 'carousel',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'custom-workflow',
                                label: 'custom-workflow',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'destinations',
                                label: 'destinations',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'dotAI',
                                label: 'dotAI',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'events',
                                label: 'events',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'faq',
                                label: 'faq',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'forms',
                                label: 'forms',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'image-gallery',
                                label: 'image-gallery',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'login',
                                label: 'login',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'site-search',
                                label: 'site-search',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'store',
                                label: 'store',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'videos',
                                label: 'videos',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'visitor-profile',
                                label: 'visitor-profile',
                                icon: 'pi pi-folder'
                            },
                            {
                                key: 'wysiwyg',
                                label: 'wysiwyg',
                                icon: 'pi pi-folder'
                            }
                        ]
                    }
                ]
            },
            {
                key: 'campaigns',
                label: 'campaigns',
                icon: 'pi pi-folder'
            },
            {
                key: 'contact-us',
                label: 'contact-us',
                icon: 'pi pi-folder'
            },
            {
                key: 'destinations',
                label: 'destinations',
                icon: 'pi pi-folder'
            },
            {
                key: 'events',
                label: 'events',
                icon: 'pi pi-folder'
            }
        ]
    }
];
