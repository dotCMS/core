import { v4 as uuid } from 'uuid';

import { DotLayoutBody } from '@dotcms/dotcms-models';
import { containersMapMock, MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotGridStackWidget,
    DotTemplateBuilderContainer,
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
            styleClass: 'bg-white py-5'
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
    'dot.template.builder.add.box': 'Add Box',
    'dot.template.builder.add.row': 'Add Row',
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
    'dot.template.builder.sidebar.header.title': 'Sidebar'
};

export const DOT_MESSAGE_SERVICE_TB_MOCK = new MockDotMessageService(MESSAGES_MOCK);

export const MOCK_TEXT = 'Header';

export const MOCK_SELECTED_STYLE_CLASSES = [
    'd-flex',
    'flex-column',
    'justify-content-center',
    'align-items-center',
    'justify-content-start',
    'justify-content-end',
    'justify-content-center',
    'justify-content-between',
    'justify-content-around',
    'justify-content-evenly',
    'align-items-start',
    'align-items-end',
    'align-items-center'
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
        'flex-column',
        'flex-row-reverse',
        'flex-column-reverse',
        'flex-grow-0',
        'flex-grow-1',
        'flex-shrink-0',
        'flex-shrink-1',
        'flex-fill',
        'justify-content-start',
        'justify-content-end',
        'justify-content-center',
        'justify-content-between',
        'justify-content-around',
        'justify-content-evenly',
        'align-items-start',
        'align-items-end',
        'align-items-center',
        'align-items-baseline',
        'align-items-stretch',
        'align-self-start',
        'align-self-end',
        'align-self-center',
        'align-self-baseline',
        'align-self-stretch',
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
