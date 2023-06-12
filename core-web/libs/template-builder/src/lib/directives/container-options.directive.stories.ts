import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DropdownModule } from 'primeng/dropdown';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainersServiceMock } from '@dotcms/utils-testing';

import { ContainerOptionsDirective } from './container-options.directive';

import {
    DOT_MESSAGE_SERVICE_TB_MOCK,
    MockContainersDropdownComponent
} from '../components/template-builder/utils/mocks';

export default {
    title: 'Container Options Directive',
    component: MockContainersDropdownComponent,
    decorators: [
        moduleMetadata({
            imports: [BrowserAnimationsModule, ContainerOptionsDirective, DropdownModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                },
                {
                    provide: DotContainersService,
                    useValue: new DotContainersServiceMock()
                }
            ]
        })
    ]
} as Meta<ContainerOptionsDirective>;

const OptionsDirective: Story<MockContainersDropdownComponent> = (
    args: MockContainersDropdownComponent
) => ({
    props: args
});

export const Base = OptionsDirective.bind({});
