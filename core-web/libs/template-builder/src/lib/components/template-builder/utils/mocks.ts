import { v4 as uuid } from 'uuid';

import { DotLayoutBody } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotGridStackWidget } from '../models/models';

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

export const CONTAINERS_DATA_MOCK = [
    {
        identifier: '//demo.dotcms.com/application/containers/banner/',
        uuid: '1'
    },
    {
        identifier: '//demo.dotcms.com/application/containers/banner/',
        uuid: '2'
    },
    {
        identifier: '//demo.dotcms.com/application/containers/banner/',
        uuid: '3'
    },
    {
        identifier: '//demo.dotcms.com/application/containers/banner/',
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
                            identifier: '//demo.dotcms.com/application/containers/banner/',
                            uuid: '1'
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
                            identifier: '//demo.dotcms.com/application/containers/banner/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
                            identifier: '//demo.dotcms.com/application/containers/default/',
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
    'dot.template.builder.footer': 'Footer'
};

export const DOT_MESSAGE_SERVICE_TB_MOCK = new MockDotMessageService(MESSAGES_MOCK);

export const MOCK_TEXT = 'Header';
