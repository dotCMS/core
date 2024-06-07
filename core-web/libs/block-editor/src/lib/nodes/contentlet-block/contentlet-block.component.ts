import { Component, OnInit, signal } from '@angular/core';

import { AngularNodeViewComponent } from '../../NodeViewRenderer';

@Component({
    selector: 'dot-contentlet-block',
    templateUrl: './contentlet-block.component.html',
    styleUrls: ['./contentlet-block.component.scss']
})
export class ContentletBlockComponent extends AngularNodeViewComponent implements OnInit {
    protected readonly data = signal(null);

    ngOnInit() {
        this.data.set(this.node.attrs.data);
    }
}
