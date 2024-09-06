import { Observable } from 'rxjs';

import { Component, Input, OnChanges, OnInit, SimpleChanges, inject, signal } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

import { map } from 'rxjs/operators';

import { DotWorkflowService } from '@dotcms/data-access';
import { DotCMSContentType, DotCMSWorkflowStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-content-aside-workflow',
    standalone: true,
    imports: [DotMessagePipe, SkeletonModule],
    providers: [DotWorkflowService],
    templateUrl: './dot-content-aside-workflow.component.html',
    styleUrl: './dot-content-aside-workflow.component.scss'
})
export class DotContentAsideWorkflowComponent implements OnInit, OnChanges {
    @Input() inode: string;
    @Input() contentType: DotCMSContentType;

    private readonly workflowService = inject(DotWorkflowService);
    protected readonly $workflowStatus = signal<DotCMSWorkflowStatus>(null);

    ngOnInit() {
        this.setContentStatus();
    }

    ngOnChanges(SimpleChanges: SimpleChanges) {
        if (SimpleChanges.inode?.currentValue) {
            this.setContentStatus();
        }
    }

    private setContentStatus() {
        const obs$ = this.inode ? this.getWorkflowStatus() : this.getNewContentStatus();
        obs$.subscribe((workflowStatus) => this.$workflowStatus.set(workflowStatus));
    }

    /**
     * Get the current workflow status
     *
     * @private
     * @return {*}
     * @memberof DotContentAsideWorkflowComponent
     */
    private getWorkflowStatus(): Observable<DotCMSWorkflowStatus> {
        return this.workflowService.getWorkflowStatus(this.inode);
    }

    /**
     * Get the new content status
     *
     * @private
     * @return {*}
     * @memberof DotContentAsideWorkflowComponent
     */
    private getNewContentStatus() {
        return this.workflowService.getSchemaContentType(this.contentType.id).pipe(
            map(({ contentTypeSchemes }) => {
                return {
                    scheme: contentTypeSchemes[0],
                    step: null,
                    task: null
                };
            })
        );
    }
}
