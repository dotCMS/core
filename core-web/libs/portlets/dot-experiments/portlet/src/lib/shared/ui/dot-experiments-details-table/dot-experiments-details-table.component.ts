import { KeyValuePipe, NgClass, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ContentChild,
    Input,
    TemplateRef
} from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

import {
    ComponentStatus,
    DotExperimentVariantDetail,
    ReachPageGoalCondition,
    UrlParameterGoalCondition
} from '@dotcms/dotcms-models';
import { DotMessagePipe, DotStringTemplateOutletDirective } from '@dotcms/ui';

/**
 *
 * Component to display a table with a title and a list of data with its headers
 * using the index of the object as headers.
 * Can send the `headers` and `rows` as templates with content projection to
 * replace the defaults templates.
 *
 * @export
 * @class DotExperimentsDetailsTableComponent
 */
@Component({
    selector: 'dot-experiments-details-table',
    imports: [
        NgTemplateOutlet,
        NgClass,
        KeyValuePipe,
        SkeletonModule,
        DotStringTemplateOutletDirective,
        DotMessagePipe
    ],
    templateUrl: './dot-experiments-details-table.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsDetailsTableComponent {
    @Input()
    title!: string | TemplateRef<unknown>;

    //** List of data to display, without templates, use the index of the objet as a header */
    @Input()
    data!: DotExperimentVariantDetail[] | Array<UrlParameterGoalCondition | ReachPageGoalCondition>;

    @Input()
    isLoading = false;

    @Input()
    isEmpty = false;

    //** Template to display the headers */
    @ContentChild('headers', { static: true }) headers!: TemplateRef<unknown>;
    //** Template to display the rows */
    @ContentChild('rows', { static: true }) rows!: TemplateRef<unknown>;
    protected readonly states = ComponentStatus;
}
