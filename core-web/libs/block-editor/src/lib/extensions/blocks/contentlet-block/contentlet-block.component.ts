import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { AngularNodeViewComponent } from '../../../NodeViewRenderer';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dotcms-contentlet-block',
    templateUrl: './contentlet-block.component.html',
    styleUrls: ['./contentlet-block.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class ContentletBlockComponent extends AngularNodeViewComponent implements OnInit {
    public data: DotCMSContentlet;

    ngOnInit() {
        this.data = this.node.attrs.data;
    }
}
