import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotCMSContentType, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentAsideInformationComponent } from './components/dot-content-aside-information/dot-content-aside-information.component';
import { DotContentAsideWorkflowComponent } from './components/dot-content-aside-workflow/dot-content-aside-workflow.component';

@Component({
    selector: 'dot-edit-content-aside',
    standalone: true,
    templateUrl: './dot-edit-content-aside.component.html',
    styleUrls: ['./dot-edit-content-aside.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        DotMessagePipe,
        DotContentAsideInformationComponent,
        DotContentAsideWorkflowComponent,
        TabViewModule
    ]
})
export class DotEditContentAsideComponent {
    /**
     * A variable with the contentlet information
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    /**
     * A variable with the content type
     */
    $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });

    /**
     * A variable to control the loading state
     */
    $loading = input.required<boolean>({ alias: 'loading' });

    /**
     * A variable to control the collapsed state
     */
    $collapsed = input.required<boolean>({ alias: 'collapsed' });

    /**
     * A variable to control the toggle state
     */
    $toggle = output<boolean>({ alias: 'toggle' });
}
