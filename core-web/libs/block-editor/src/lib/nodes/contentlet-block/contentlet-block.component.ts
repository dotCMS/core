import { Component, OnInit } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { AngularNodeViewComponent } from '../../NodeViewRenderer';

// Models

@Component({
    selector: 'dot-contentlet-block',
    templateUrl: './contentlet-block.component.html',
    styleUrls: ['./contentlet-block.component.scss']
})
export class ContentletBlockComponent extends AngularNodeViewComponent implements OnInit {
    public data: DotCMSContentlet;

    ngOnInit() {
        this.data = this.node.attrs.data;
    }
}
