import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DropdownModule } from 'primeng/dropdown';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainersServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContainerOptionsDirective } from './dot-container-options.directive';
import { MockContainersDropdownComponent } from './mock-containers-dropdown.component';

export default {
    title: 'Container Options Directive',
    component: MockContainersDropdownComponent,
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
        })
    ]
} as Meta<DotContainerOptionsDirective>;

const OptionsDirective: Story<MockContainersDropdownComponent> = (
    args: MockContainersDropdownComponent
) => ({
    props: args
});

export const Base = OptionsDirective.bind({});
