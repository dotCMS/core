import { v4 as uuid } from 'uuid';

import { Component, ElementRef, inject } from '@angular/core';

import { DotContainer, DotLayoutBody, CONTAINER_SOURCE } from '@dotcms/dotcms-models';
import { containersMapMock, MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotGridStackWidget,
    DotTemplateBuilderContainer,
    DotTemplateBuilderState,
    SYSTEM_CONTAINER_IDENTIFIER
} from '../models/models';

export const EMPTY_ROWS_VALUE = [
    {
        w: 12,
        h: 1,
        x: 0,
        y: 0,
        subGridOpts: {
            children: [
                {
                    w: 3,
                    h: 1,
                    y: 0,
                    x: 0,
                    id: uuid(),
                    styleClass: [],
                    containers: [
                        {
                            identifier: SYSTEM_CONTAINER_IDENTIFIER
                        }
                    ]
                }
            ]
        },
        id: uuid(),
        styleClass: []
    }
];

export const GRIDSTACK_DATA_MOCK: DotGridStackWidget[] = [
    { x: 0, y: 0, w: 12, id: uuid() },
    { x: 0, y: 1, w: 12, id: uuid() },
    {
        x: 0,
        y: 2,
        w: 12,
        id: uuid(),
        subGridOpts: {
            children: [{ x: 0, y: 0, w: 4, id: uuid() }]
        }
    }
];

export const DEFAULT_CONTAINER_IDENTIFIER = '//demo.dotcms.com/application/containers/default/';

export const BANNER_CONTAINER_IDENTIFIER = '//demo.dotcms.com/application/containers/banner/';

// Mock containers for defaultContainer tests
export const mockDefaultContainerWithPath: DotContainer = {
    identifier: 'default-container-id',
    name: 'Default Container',
    type: 'containers',
    source: CONTAINER_SOURCE.FILE,
    live: true,
    working: true,
    deleted: false,
    locked: false,
    title: 'Default Container Title',
    path: '/default/container/path',
    archived: false,
    categoryId: 'default-category',
    parentPermissionable: {
        hostname: 'default-host'
    }
};

export const mockDefaultContainerWithoutPath: DotContainer = {
    identifier: 'default-container-id',
    name: 'Default Container',
    type: 'containers',
    source: CONTAINER_SOURCE.FILE,
    live: true,
    working: true,
    deleted: false,
    locked: false,
    title: 'Default Container Title',
    archived: false,
    categoryId: 'default-category',
    parentPermissionable: {
        hostname: 'default-host'
    }
};

export const mockTempContainer: DotContainer = {
    identifier: 'temp-container-id',
    name: 'Temp Container',
    type: 'containers',
    source: CONTAINER_SOURCE.FILE,
    live: true,
    working: true,
    deleted: false,
    locked: false,
    title: 'Temp Container Title',
    path: '/temp/container/path',
    archived: false,
    categoryId: 'temp-category',
    parentPermissionable: {
        hostname: 'temp-host'
    }
};

export const CONTAINERS_DATA_MOCK = [
    {
        identifier: BANNER_CONTAINER_IDENTIFIER,
        uuid: '1'
    },
    {
        identifier: BANNER_CONTAINER_IDENTIFIER,
        uuid: '2'
    },
    {
        identifier: BANNER_CONTAINER_IDENTIFIER,
        uuid: '3'
    },
    {
        identifier: BANNER_CONTAINER_IDENTIFIER,
        uuid: '4'
    }
];

export const MINIMAL_DATA_MOCK: DotLayoutBody = {
    rows: [
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: BANNER_CONTAINER_IDENTIFIER,
                            uuid: '1'
                        },
                        {
                            identifier: 'another-identifier',
                            uuid: '2'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: 'banner-tall'
                }
            ],
            styleClass: 'p-0 banner-tall'
        }
    ]
};

export const FULL_DATA_MOCK: DotLayoutBody = {
    rows: [
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: BANNER_CONTAINER_IDENTIFIER,
                            uuid: '1'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: 'banner-tall'
                }
            ],
            styleClass: 'p-0 banner-tall'
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '1'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: 'mt-70 booking-form'
                }
            ],
            styleClass: null
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '2'
                        }
                    ],
                    leftOffset: 1,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '3'
                        }
                    ],
                    leftOffset: 4,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '4'
                        }
                    ],
                    leftOffset: 7,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '5'
                        }
                    ],
                    leftOffset: 10,
                    width: 3,
                    styleClass: ''
                }
            ],
            styleClass: null
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '6'
                        }
                    ],
                    leftOffset: 1,
                    width: 6,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '7'
                        }
                    ],
                    leftOffset: 7,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '8'
                        }
                    ],
                    leftOffset: 10,
                    width: 3,
                    styleClass: ''
                }
            ],
            styleClass: null
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '9'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: ''
                }
            ],
            styleClass: 'bg-white py-8'
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '10'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: ''
                }
            ],
            styleClass: null
        }
    ]
};

export const FULL_DATA_MOCK_UNSORTED: DotLayoutBody = {
    rows: [
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: BANNER_CONTAINER_IDENTIFIER,
                            uuid: '1'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: 'banner-tall'
                }
            ],
            styleClass: 'p-0 banner-tall'
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '1'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: 'mt-70 booking-form'
                }
            ],
            styleClass: null
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '3'
                        }
                    ],
                    leftOffset: 4,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '2'
                        }
                    ],
                    leftOffset: 1,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '5'
                        }
                    ],
                    leftOffset: 10,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '4'
                        }
                    ],
                    leftOffset: 7,
                    width: 3,
                    styleClass: ''
                }
            ],
            styleClass: null
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '7'
                        }
                    ],
                    leftOffset: 7,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '8'
                        }
                    ],
                    leftOffset: 10,
                    width: 3,
                    styleClass: ''
                },
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '6'
                        }
                    ],
                    leftOffset: 1,
                    width: 6,
                    styleClass: ''
                }
            ],
            styleClass: null
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '9'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: ''
                }
            ],
            styleClass: 'bg-white py-8'
        },
        {
            columns: [
                {
                    containers: [
                        {
                            identifier: DEFAULT_CONTAINER_IDENTIFIER,
                            uuid: '10'
                        }
                    ],
                    leftOffset: 1,
                    width: 12,
                    styleClass: ''
                }
            ],
            styleClass: null
        }
    ]
};

export const MESSAGES_MOCK = {
    'dot.template.builder.action.cancel': 'Cancel',
    'dot.template.builder.action.create': 'Create',
    'dot.template.builder.add.container': 'Add Container',
    'dot.template.builder.add.box': 'Box',
    'dot.template.builder.add.row': 'Row',
    'dot.template.builder.edit.classes': 'Edit Classes',
    'dot.template.builder.edit.box': 'Edit Box',
    'dot.template.builder.label.classes': 'Classes',
    'dot.template.builder.comfirmation.popup.message':
        'Are you sure you want to proceed deleting this item?',
    'dot.template.builder.comfirmation.popup.option.no': 'No',
    'dot.template.builder.comfirmation.popup.option.yes': 'Yes',
    'dot.template.builder.header': 'Header',
    'dot.template.builder.footer': 'Footer',
    'dot.template.builder.toolbar.button.layout.label': 'Layout',
    'dot.template.builder.toolbar.button.theme.label': 'Theme',
    'editpage.layout.properties.header': 'Header',
    'editpage.layout.properties.footer': 'Footer',
    'editpage.layout.properties.sidebar.left': 'Sidebar Left',
    'editpage.layout.properties.sidebar.right': 'Sidebar Right',
    'dot.template.builder.box.containers.error': 'Error loading containers',
    'dot.template.builder.classes.dialog.autocomplete.label': 'Class',
    'dot.template.builder.classes.dialog.header.label': 'Edit Classes',
    'dot.template.builder.theme.dialog.header.label': 'Theme Selection',
    'editpage.layout.theme.no.records.found': 'No records found',
    'dot.common.cancel': 'Cancel',
    'dot.common.apply': 'Apply',
    'editpage.layout.theme.search': 'Search',
    'dot.template.builder.classes.dialog.update.button': 'Update',
    'dot.template.builder.sidebar.header.title': 'Sidebar',
    'dot.template.builder.row.box.wont.fit': 'Minimum 1 column needed for box drop.',
    'dot.template.builder.autocomplete.has.suggestions':
        'Type and hit enter or select from suggestions to add a class',
    'dot.template.builder.autocomplete.no.suggestions': 'Type and hit enter to add a class',
    'dot.template.builder.autocomplete.setup.suggestions':
        'You can set up predefined class suggestions. <a href="https://www.dotcms.com/docs/latest/designing-a-template-with-a-theme#ClassSuggestions">Get the setup guide</a>'
};

export const DOT_MESSAGE_SERVICE_TB_MOCK = new MockDotMessageService(MESSAGES_MOCK);

export const MOCK_TEXT = 'Header';

export const MOCK_SELECTED_STYLE_CLASSES = [
    'd-flex',
    'flex-col',
    'justify-center',
    'items-center',
    'justify-start',
    'justify-end',
    'justify-center',
    'justify-between',
    'justify-around',
    'justify-evenly',
    'items-start',
    'items-end',
    'items-center'
];

export const MOCK_STYLE_CLASSES_FILE = {
    classes: [
        'd-none',
        'd-inline',
        'd-inline-block',
        'd-block',
        'd-grid',
        'd-table',
        'd-table-row',
        'd-table-cell',
        'd-flex',
        'd-inline-flex',
        'd-sm-none',
        'd-sm-inline',
        'd-sm-inline-block',
        'flex-row',
        'flex-col',
        'flex-row-reverse',
        'flex-col-reverse',
        'grow-0',
        'grow',
        'shrink-0',
        'shrink',
        'flex-fill',
        'justify-start',
        'justify-end',
        'justify-center',
        'justify-between',
        'justify-around',
        'justify-evenly',
        'items-start',
        'items-end',
        'items-center',
        'items-baseline',
        'items-stretch',
        'self-start',
        'self-end',
        'self-center',
        'self-baseline',
        'self-stretch',
        'flex-nowrap',
        'flex-wrap',
        'flex-wrap-reverse',
        'float-start',
        'float-end',
        'text-start',
        'text-end',
        'text-center',
        'text-justify'
    ]
};

export const CONTAINER_MAP_MOCK = {
    [DEFAULT_CONTAINER_IDENTIFIER]: {
        title: 'Default',
        identifier: DEFAULT_CONTAINER_IDENTIFIER
    },
    [BANNER_CONTAINER_IDENTIFIER]: {
        title: 'Banner',
        identifier: BANNER_CONTAINER_IDENTIFIER
    },
    ...containersMapMock
};

const DEFAULT_ITEM_MOCK = { identifier: DEFAULT_CONTAINER_IDENTIFIER };
const BANNER_ITEM_MOCK = { identifier: BANNER_CONTAINER_IDENTIFIER };

export const ITEMS_MOCK = [
    DEFAULT_ITEM_MOCK,
    BANNER_ITEM_MOCK,
    DEFAULT_ITEM_MOCK,
    DEFAULT_ITEM_MOCK,
    DEFAULT_ITEM_MOCK,
    DEFAULT_ITEM_MOCK
];

const noop = () => {
    //
};

export function mockMatchMedia() {
    // needed in component specs that open a prime-ng modal
    window.matchMedia =
        window.matchMedia ||
        function () {
            return {
                matches: false,
                media: '',
                onchange: null,
                addListener: noop, // deprecated
                removeListener: noop, // deprecated
                addEventListener: noop,
                removeEventListener: noop,
                dispatchEvent: () => true
            };
        };
}

export const mockTemplateBuilderContainer: DotTemplateBuilderContainer = {
    identifier: '1',
    uuid: '1'
};

export const SIDEBAR_MOCK = {
    location: 'left',
    width: 'small',
    containers: []
};

export const STYLE_CLASS_MOCK = ['test', 'mock-class'];

export const CLASS_NAME_MOCK = 'custom-class';

export const ROWS_MOCK = [
    {
        w: 12,
        h: 1,
        x: 0,
        y: 0,
        subGridOpts: {
            children: [
                {
                    w: 7,
                    h: 1,
                    y: 0,
                    x: 0,
                    id: '59c16004-8fac-4627-a013-04086bf6d0e3',
                    styleClass: ['banner-tall'],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/banner/',
                            uuid: '1'
                        }
                    ]
                }
            ]
        },
        id: 'a9b994e9-3bb8-4f47-91ce-5d0f0afed894',
        styleClass: ['p-0', 'banner-tall']
    },
    {
        w: 12,
        h: 1,
        x: 0,
        y: 1,
        subGridOpts: {
            children: [
                {
                    w: 7,
                    h: 1,
                    y: 0,
                    x: 0,
                    id: 'd15fc2a1-e5c9-48bf-abcd-aea213d48bea',
                    styleClass: ['mt-70', 'booking-form'],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/default/',
                            uuid: '1'
                        }
                    ]
                },
                {
                    w: 3,
                    h: 1,
                    y: 0,
                    x: 9,
                    id: '8dea0760-3f1f-4f06-b14b-34fcdd86e4c1',
                    styleClass: [],
                    containers: [
                        {
                            identifier: 'SYSTEM_CONTAINER'
                        }
                    ]
                }
            ]
        },
        id: '74a97ec0-a406-458e-be83-b32b3e33f689',
        styleClass: []
    },
    {
        w: 12,
        h: 1,
        x: 0,
        y: 2,
        subGridOpts: {
            children: [
                {
                    w: 3,
                    h: 1,
                    y: 0,
                    x: 0,
                    id: '27b7b133-e5dc-4b41-99b0-bbe18ae59387',
                    styleClass: [],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/default/',
                            uuid: '2'
                        }
                    ]
                },
                {
                    w: 4,
                    h: 1,
                    y: 0,
                    x: 3,
                    id: 'ef8a4b38-336c-4c9b-bf94-1bc87f7a74ea',
                    styleClass: [],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/default/',
                            uuid: '3'
                        }
                    ]
                },
                {
                    w: 2,
                    h: 1,
                    y: 0,
                    x: 7,
                    id: 'ef8a4b38-336c-4c9b-bf94-1bc87f7a74ea',
                    styleClass: [],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/default/',
                            uuid: '3'
                        }
                    ]
                },
                {
                    w: 4,
                    h: 1,
                    y: 0,
                    x: 9,
                    id: '11f7745a-16fa-4236-b3c1-fdb182a59315',
                    styleClass: [],
                    containers: [
                        {
                            identifier: 'SYSTEM_CONTAINER'
                        }
                    ]
                }
            ]
        },
        id: '1d66ecec-e39c-4d37-8344-a02a321336fe',
        styleClass: []
    },
    {
        w: 12,
        h: 1,
        x: 0,
        y: 3,
        subGridOpts: {
            children: [
                {
                    w: 3,
                    h: 1,
                    y: 0,
                    x: 0,
                    id: 'b1c628b2-d748-4c36-90a5-449a138330fd',
                    styleClass: [],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/default/',
                            uuid: '6'
                        }
                    ]
                },
                {
                    w: 5,
                    h: 1,
                    y: 0,
                    x: 4,
                    id: '95d2cc1c-64c4-4d10-a88a-7c654947a51a',
                    styleClass: [],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/default/',
                            uuid: '7'
                        }
                    ]
                },
                {
                    w: 4,
                    h: 1,
                    y: 0,
                    x: 9,
                    id: 'd6428ef2-fb43-47cb-b65d-3920162acdf9',
                    styleClass: [],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/default/',
                            uuid: '8'
                        }
                    ]
                }
            ]
        },
        id: '60c2f2e0-9017-4469-bf22-c2d30f514392',
        styleClass: []
    },
    {
        w: 12,
        h: 1,
        x: 0,
        y: 4,
        subGridOpts: {
            children: []
        },
        id: '60c2f2e0-9017-4439-bf22-c2d30f514392'
    }
];

export const ROWS_MINIMAL_MOCK: DotGridStackWidget[] = [
    {
        w: 12,
        h: 1,
        x: 0,
        y: 0,
        subGridOpts: {
            children: [
                {
                    w: 7,
                    h: 1,
                    y: 0,
                    x: 0,
                    id: '59c16004-8fac-4627-a013-04086bf6d0e3',
                    styleClass: ['banner-tall'],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/banner/',
                            uuid: '1'
                        }
                    ]
                }
            ]
        },
        id: 'a9b994e9-3bb8-4f47-91ce-5d0f0afed894',
        styleClass: ['p-0', 'banner-tall']
    },
    {
        w: 12,
        h: 1,
        x: 0,
        y: 1,
        subGridOpts: {
            children: [
                {
                    w: 7,
                    h: 1,
                    y: 0,
                    x: 0,
                    id: 'd15fc2a1-e5c9-48bf-abcd-aea213d48bea',
                    styleClass: ['mt-70', 'booking-form'],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/banner/',
                            uuid: '2'
                        }
                    ]
                },
                {
                    w: 3,
                    h: 1,
                    y: 0,
                    x: 9,
                    id: '8dea0760-3f1f-4f06-b14b-34fcdd86e4c1',
                    styleClass: [],
                    containers: [
                        {
                            identifier: '//demo.dotcms.com/application/containers/banner/',
                            uuid: '3'
                        }
                    ]
                }
            ]
        },
        id: '74a97ec0-a406-458e-be83-b32b3e33f689',
        styleClass: []
    }
];

export const BOX_MOCK = {
    w: 3,
    h: 1,
    y: 0,
    x: 9,
    id: 'd6428ef2-fb43-47cb-b65d-3920162acdf9',
    styleClass: [],
    containers: [
        {
            identifier: '//demo.dotcms.com/application/containers/default/',
            uuid: '8'
        }
    ]
};

/**
 * Mock of an element inside the gridstack
 *
 * @class MockGridStackElementComponent
 */
@Component({
    selector: 'dotcms-grid-stack-element',
    template: '<div>Element</div>',
    standalone: false
})
export class MockGridStackElementComponent {
    el = inject(ElementRef);

    constructor() {
        this.el.nativeElement.ddElement = {
            on: () => {
                /* noop */
            }
        };
    }
}

// Mock used to maintain the state of the template builder
export const INITIAL_STATE_MOCK: DotTemplateBuilderState = {
    rows: [],
    layoutProperties: {
        header: true,
        footer: true,
        sidebar: {
            containers: [],
            location: 'left',
            width: 'small'
        }
    },
    resizingRowID: '',
    containerMap: {},
    themeId: '123',
    shouldEmit: true,
    templateIdentifier: '111'
};
