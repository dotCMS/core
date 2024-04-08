import { Observable, of } from 'rxjs';

import { AsyncPipe, NgIf, NgSwitch, NgSwitchCase, SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ChipModule } from 'primeng/chip';
import { DividerModule } from 'primeng/divider';

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
        NgIf,
        NgSwitch,
        NgSwitchCase,
        SlicePipe,
        DotApiLinkComponent,
        DotCopyButtonComponent,
        DotRelativeDatePipe,
        ChipModule,
        DotMessagePipe,
        ContentletStatusPipe,
        RouterLink,
        AsyncPipe,
        DotLinkComponent,
        DividerModule
    ],
    providers: [DotWorkflowService]
})
export class DotEditContentAsideComponent implements OnInit {
    @Input() contentlet!: DotCMSContentlet;
    @Input() contentType!: string;

    private readonly workFlowService = inject(DotWorkflowService);

    workflow$!: Observable<DotCMSWorkflowStatus>;

    ngOnInit() {
        this.workflow$ = this.contentlet?.inode
            ? this.workFlowService.getWorkflowStatus(this.contentlet.inode)
            : of({ scheme: null, step: null, task: null });
    }
}
