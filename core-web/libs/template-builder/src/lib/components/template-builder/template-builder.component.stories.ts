import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { NgFor, AsyncPipe } from '@angular/common';

import { DotLayoutBody } from '@dotcms/dotcms-models';

import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
const data: DotLayoutBody = {
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

export default {
    title: 'TemplateBuilderComponent',
    component: TemplateBuilderComponent,
    decorators: [
        moduleMetadata({
            imports: [NgFor, AsyncPipe, TemplateBuilderRowComponent],
            providers: [DotTemplateBuilderStore]
        })
    ]
} as Meta<TemplateBuilderComponent>;

const Template: Story<TemplateBuilderComponent> = (args: TemplateBuilderComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {
    templateLayout: { body: data }
};
