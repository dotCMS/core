import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DropdownModule } from 'primeng/dropdown';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainersServiceMock } from '@dotcms/utils-testing';

import { DotContainerOptionsDirective } from './dot-container-options.directive';
import { MockContainersDropdownComponent } from './mock-containers-dropdown.component';

import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';

export default {
    title: 'Container Options Directive',
    component: MockContainersDropdownComponent,
    decorators: [
        moduleMetadata({
            imports: [BrowserAnimationsModule, DotContainerOptionsDirective, DropdownModule],
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
} as Meta<DotContainerOptionsDirective>;

const OptionsDirective: Story<MockContainersDropdownComponent> = (
    args: MockContainersDropdownComponent
) => ({
    props: args
});

export const Base = OptionsDirective.bind({});
