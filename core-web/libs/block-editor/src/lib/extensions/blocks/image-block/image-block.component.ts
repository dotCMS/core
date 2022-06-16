import { Component, OnInit, ElementRef, OnDestroy } from '@angular/core';
import { AngularNodeViewComponent } from '../../../NodeViewRenderer';

interface ImageData {
    asset: string;
    name: string;
}

@Component({
    selector: 'dotcms-image-block',
    templateUrl: './image-block.component.html',
    styleUrls: ['./image-block.component.scss']
})
export class ImageBlockComponent extends AngularNodeViewComponent implements OnInit, OnDestroy {
    public data: ImageData;
    public href: string;

    constructor(private _elementRef: ElementRef) {
        super();
    }

    ngOnInit(): void {
        this.data = this.node.attrs.data;
        this.editor.on('update', this.updateImageAttributes.bind(this));
        this.updateImageAttributes();
    }

    ngOnDestroy(): void {
        this.editor.off('update', this.updateImageAttributes.bind(this));
    }

    private updateImageAttributes(): void {
        this._elementRef.nativeElement.style.textAlign = this.node.attrs.textAlign;
        this.href = this.node.attrs.href;
    }
}
