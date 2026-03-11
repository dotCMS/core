import { Component, OnInit, signal } from '@angular/core';

import { Card } from 'primeng/card';

import { DotSpinnerComponent } from '@dotcms/ui';

import { AngularNodeViewComponent } from '../../core/node-view/node-view-renderer';
import { ContentletStatePipe } from '../../shared/pipes/contentlet-state/contentlet-state.pipe';

@Component({
    selector: 'dot-contentlet-block',
    templateUrl: './contentlet-block.component.html',
    styleUrls: ['./contentlet-block.component.css'],
    standalone: true,
    imports: [Card, DotSpinnerComponent, ContentletStatePipe]
})
export class ContentletBlockComponent extends AngularNodeViewComponent implements OnInit {
    protected readonly data = signal(null);

    ngOnInit() {
        this.data.set(this.node.attrs.data);
    }
}
