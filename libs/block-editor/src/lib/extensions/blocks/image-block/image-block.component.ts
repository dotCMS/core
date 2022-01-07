import { Component, OnInit } from '@angular/core';
import { AngularNodeViewComponent } from '../../../NodeViewRenderer';

@Component({
    selector: 'dotcms-image-block',
    templateUrl: './image-block.component.html',
    styleUrls: ['./image-block.component.scss']
})
export class ImageBlockComponent extends AngularNodeViewComponent implements OnInit {
    data: {
        asset: string;
    };

    ngOnInit(): void {
        this.data = this.node.attrs.data;
    }
}
