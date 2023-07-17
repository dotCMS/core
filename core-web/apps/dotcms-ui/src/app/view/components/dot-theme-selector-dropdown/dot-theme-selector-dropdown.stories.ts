/* eslint-disable no-console */
import { Meta, moduleMetadata } from '@storybook/angular';
import { of } from 'rxjs';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotMessageService, DotThemesService, PaginatorService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotThemeSelectorDropdownComponent } from './dot-theme-selector-dropdown.component';

const messageServiceMock = new MockDotMessageService({
    'dot.common.select.themes': 'Select Themes',
    'Last-Updated': 'Last updated'
});

export default {
    title: 'DotCMS/ThemeSelector',
    component: DotThemeSelectorDropdownComponent,
    decorators: [
        moduleMetadata({
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotFormatDateService,
                    useValue: {}
                },
                {
                    provide: PaginatorService,
                    useValue: {
                        setExtraParams() {
                            /* */
                        },
                        getWithOffset() {
                            return of([
                                {
                                    defaultFileType: '33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d',
                                    filesMasks: '',
                                    hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                                    iDate: 1606163218691,
                                    identifier: '0db87cc5-e185-421a-8b83-abd0ee18d68d',
                                    inode: 'e98512c2-940d-4de0-9fd0-7e97ff1cef9d',
                                    modDate: 1606163218693,
                                    name: 'sports 42',
                                    path: '/application/themes/sports 42/',
                                    showOnMenu: false,
                                    sortOrder: 0,
                                    themeThumbnail: null,
                                    title: 'sports 42',
                                    type: 'folder'
                                },
                                {
                                    defaultFileType: '33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d',
                                    filesMasks: '',
                                    hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                                    iDate: 1606163218769,
                                    identifier: 'a9a6317b-9cc3-4367-b8e0-c9a32828dcd0',
                                    inode: '35f3ef80-885a-4aef-ae8e-aac2834fa825',
                                    modDate: 1606163218771,
                                    name: 'sports 43',
                                    path: '/application/themes/sports 43/',
                                    showOnMenu: false,
                                    sortOrder: 0,
                                    themeThumbnail: null,
                                    title: 'sports 43',
                                    type: 'folder'
                                },
                                {
                                    defaultFileType: '33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d',
                                    filesMasks: '',
                                    hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                                    iDate: 1606163218809,
                                    identifier: '05a949af-f4f4-4a0a-ab36-52c3f2907fc8',
                                    inode: '670d9340-6c5c-44cf-896a-c91832027354',
                                    modDate: 1606163218811,
                                    name: 'sports 44',
                                    path: '/application/themes/sports 44/',
                                    showOnMenu: false,
                                    sortOrder: 0,
                                    themeThumbnail: null,
                                    title: 'sports 44',
                                    type: 'folder'
                                },
                                {
                                    defaultFileType: '33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d',
                                    filesMasks: '',
                                    hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                                    iDate: 1606163218893,
                                    identifier: '7343bbdf-6565-4813-84c7-e1c97b77e3b0',
                                    inode: 'f1c6ab6f-c697-4208-be62-63ea1f1b23f1',
                                    modDate: 1606163218895,
                                    name: 'sports 45',
                                    path: '/application/themes/sports 45/',
                                    showOnMenu: false,
                                    sortOrder: 0,
                                    themeThumbnail: null,
                                    title: 'sports 45',
                                    type: 'folder'
                                },
                                {
                                    defaultFileType: '33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d',
                                    filesMasks: '',
                                    hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                                    iDate: 1606163218938,
                                    identifier: '9b991334-6d00-4a5d-942c-468be13e2f1c',
                                    inode: 'debac457-fb94-4ebf-8e97-d783dff4df47',
                                    modDate: 1606163218940,
                                    name: 'sports 46',
                                    path: '/application/themes/sports 46/',
                                    showOnMenu: false,
                                    sortOrder: 0,
                                    themeThumbnail: null,
                                    title: 'sports 46',
                                    type: 'folder'
                                },
                                {
                                    defaultFileType: '33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d',
                                    filesMasks: '',
                                    hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                                    iDate: 1606163219018,
                                    identifier: 'defca879-7f86-49f7-821c-5f3e6036d1d9',
                                    inode: '305841e1-b73b-4e46-a6bb-0e8826fa1b5b',
                                    modDate: 1606163219020,
                                    name: 'sports 47',
                                    path: '/application/themes/sports 47/',
                                    showOnMenu: false,
                                    sortOrder: 0,
                                    themeThumbnail: null,
                                    title: 'sports 47',
                                    type: 'folder'
                                },
                                {
                                    defaultFileType: '33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d',
                                    filesMasks: '',
                                    hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                                    iDate: 1606163219057,
                                    identifier: '6da4db70-693c-4ddb-9ab3-d8b7188e81f8',
                                    inode: '434aad2c-3964-48c5-9d43-394fbdd31228',
                                    modDate: 1606163219058,
                                    name: 'sports 48',
                                    path: '/application/themes/sports 48/',
                                    showOnMenu: false,
                                    sortOrder: 0,
                                    themeThumbnail: null,
                                    title: 'sports 48',
                                    type: 'folder'
                                },
                                {
                                    defaultFileType: '33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d',
                                    filesMasks: '',
                                    hostId: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                                    iDate: 1606163219151,
                                    identifier: 'da820a0d-194c-4734-a267-892536b7150c',
                                    inode: 'd88cca05-c098-4560-ab66-97d979b4da39',
                                    modDate: 1606163219152,
                                    name: 'sports 49',
                                    path: '/application/themes/sports 49/',
                                    showOnMenu: false,
                                    sortOrder: 0,
                                    themeThumbnail: null,
                                    title: 'sports 49',
                                    type: 'folder'
                                }
                            ]);
                        }
                    }
                },
                {
                    provide: SiteService,
                    useValue: {
                        getCurrentSite() {
                            return of({ identifier: 'asasasa' });
                        }
                    }
                },
                {
                    provide: DotThemesService,
                    useValue: {
                        get() {
                            return of({});
                        }
                    }
                }
            ],
            imports: [SearchableDropDownModule, BrowserAnimationsModule, DotMessagePipe],
            declarations: [DotThemeSelectorDropdownComponent]
        })
    ],
    parameters: {
        docs: {
            description: {
                component: 'DotCMS Theme Selector'
            },
            iframeHeight: 800
        }
    },
    args: {
        onThemeSelectorChange: (event) => {
            console.log(event);
        },
        totalRecords: 8,
        paginationPerPage: 5,
        rows: 5
    }
} as Meta;

const ThemeSelectorTemplate = `
  <dot-theme-selector-dropdown
          (change)="onThemeSelectorChange($event)"
  ></dot-theme-selector-dropdown>
`;

export const Basic = (props) => {
    return {
        template: ThemeSelectorTemplate,
        props
    };
};

Basic.parameters = {
    docs: {
        source: {
            code: ThemeSelectorTemplate
        }
    }
};
