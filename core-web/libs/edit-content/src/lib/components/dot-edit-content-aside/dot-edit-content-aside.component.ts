import { Observable, of } from 'rxjs';

import { AsyncPipe, NgIf, NgSwitch, NgSwitchCase, SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ChipModule } from 'primeng/chip';

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
        DotLinkComponent
    ],
    providers: [DotWorkflowService]
})
export class DotEditContentAsideComponent implements OnInit {
    @Input() contentLet!: DotCMSContentlet;
    @Input() contentType!: string;

    private readonly workFlowService = inject(DotWorkflowService);

    workflow$!: Observable<DotCMSWorkflowStatus>;

    ngOnInit() {
        if (this.contentLet?.inode) {
            this.workflow$ = this.workFlowService.getWorkflowStatus(this.contentLet.inode);
        } else {
            this.workflow$ = of({ scheme: null, step: null, task: null });
        }
    }
}
