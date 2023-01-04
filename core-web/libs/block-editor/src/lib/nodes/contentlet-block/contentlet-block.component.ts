import { Component, OnInit } from '@angular/core';
import { AngularNodeViewComponent } from '../../NodeViewRenderer';

// Models
import { DotCMSContentlet } from '@dotcms/dotcms-models';

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
