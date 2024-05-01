import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

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
        CommonModule,
        DotMessagePipe,
        DotContentAsideInformationComponent,
        DotContentAsideWorkflowComponent
    ]
})
export class DotEditContentAsideComponent {
    @Input() contentlet!: DotCMSContentlet;
    @Input() contentType!: DotCMSContentType;
    @Input() loading!: boolean;
}
