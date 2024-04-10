import { Observable, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';

import { DotWorkflowService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSWorkflowStatus } from '@dotcms/dotcms-models';
import {
    DotApiLinkComponent,
    DotCopyButtonComponent,
    DotLinkComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

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
        DotRelativeDatePipe
    ],
    providers: [DotWorkflowService]
})
export class DotEditContentAsideComponent implements OnInit {
    @Input() contentlet!: DotCMSContentlet;
    @Input() contentType!: string;

    private readonly workflowService = inject(DotWorkflowService);

    workflow$!: Observable<DotCMSWorkflowStatus>;

    ngOnInit() {
        this.workflow$ = this.contentlet?.inode
            ? this.workflowService.getWorkflowStatus(this.contentlet.inode)
            : of({ scheme: null, step: null, task: null });
    }
}
