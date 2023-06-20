import { KeyValuePipe, NgClass, NgForOf, NgIf, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ContentChild,
    Input,
    TemplateRef
} from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

import { ComponentStatus, DotExperimentVariantDetail } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@dotcms/ui';
import { DotStringTemplateOutletDirective } from '@portlets/shared/directives/dot-string-template-outlet.directive';

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
    standalone: true,
    imports: [
        NgIf,
        NgTemplateOutlet,
        NgForOf,
        NgClass,
        KeyValuePipe,
        SkeletonModule,
        DotStringTemplateOutletDirective,
        DotMessagePipeModule
    ],
    templateUrl: './dot-experiments-details-table.component.html',
    styleUrls: ['./dot-experiments-details-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsDetailsTableComponent {
    @Input()
    title!: string | TemplateRef<unknown>;

    //** List of data to display, without templates, use the index of the objet as a header */
    @Input()
    data!: DotExperimentVariantDetail[];

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
