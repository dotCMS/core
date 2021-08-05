import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { AngularNodeViewComponent } from '../../../NodeViewRenderer';

@Component({
    selector: 'dotcms-contentlet-block',
    templateUrl: './contentlet-block.component.html',
    styleUrls: ['./contentlet-block.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class ContentletBlockComponent extends AngularNodeViewComponent implements OnInit {
    data: {
        title: string;
        inode: string;
    };

    ngOnInit() {
        this.data = this.props.node.attrs.data;
    }
}
