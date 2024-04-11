import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentType, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotApiLinkComponent,
    DotCopyButtonComponent,
    DotLinkComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { DotContentAsideWorkflowComponent } from './components/dot-content-aside-workflow/dot-content-aside-workflow.component';

import { ContentletStatusPipe } from '../../pipes/contentlet-status.pipe';

@Component({
    selector: 'dot-edit-content-aside',
    standalone: true,
    templateUrl: './dot-edit-content-aside.component.html',
    styleUrls: ['./dot-edit-content-aside.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        CommonModule,
        DotApiLinkComponent,
        DotCopyButtonComponent,
        DotRelativeDatePipe,
        ChipModule,
        DotMessagePipe,
        ContentletStatusPipe,
        RouterLink,
        DotLinkComponent,
        TooltipModule,
        DotRelativeDatePipe,
        DotContentAsideWorkflowComponent
    ]
})
export class DotEditContentAsideComponent {
    @Input() contentlet!: DotCMSContentlet;
    @Input() contentType!: DotCMSContentType;
}
