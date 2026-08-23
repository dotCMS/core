import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { Component, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';

import { AngularNodeViewComponent } from '../../NodeViewRenderer';

@Component({
    selector: 'dot-contentlet-block',
    templateUrl: './contentlet-block.component.html',
    styleUrls: ['./contentlet-block.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ContentletBlockComponent extends AngularNodeViewComponent implements OnInit {
    protected readonly data = signal<DotCMSContentlet | null>(null);

    ngOnInit() {
        this.data.set(this.node.attrs['data']);
    }
}
