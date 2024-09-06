import { moduleMetadata, StoryObj, Meta, componentWrapperDecorator } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DropdownModule, Dropdown } from 'primeng/dropdown';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainersServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContainerOptionsDirective } from './dot-container-options.directive';

const meta: Meta = {
    title: 'DotCMS/Container Options Directive',
    component: Dropdown,
    decorators: [
        moduleMetadata({
            imports: [BrowserAnimationsModule, DotContainerOptionsDirective, DropdownModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({})
                },
                {
                    provide: DotContainersService,
                    useValue: new DotContainersServiceMock()
                }
            ]
        }),
        componentWrapperDecorator(
            (story) =>
                `<div class="card flex justify-content-center  w-50rem h-25rem">${story}</div>`
        )
    ],
    parameters: {
        layout: 'centered'
    },
    render: (args) => ({
        props: args,
        template: `<p-dropdown placeholder="Select a container" dotContainerOptions />`
    })
};
export default meta;

type Story = StoryObj;

export const Base: Story = {};
