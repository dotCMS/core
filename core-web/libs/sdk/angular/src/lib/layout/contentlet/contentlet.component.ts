import { ChangeDetectionStrategy, Component, HostBinding, Input, OnChanges } from '@angular/core';

import { DotCMSContentlet } from '../../models';

@Component({
    selector: 'dotcms-contentlet-wrapper',
    standalone: true,
    template: '<ng-content></ng-content>',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContentletComponent implements OnChanges {
    @Input({ required: true }) contentlet!: DotCMSContentlet;
    @Input() container!: string;

    @HostBinding('attr.data-dot-identifier') identifier: string | null = null;
    @HostBinding('attr.data-dot-basetype') baseType: string | null = null;
    @HostBinding('attr.data-dot-title') title: string | null = null;
    @HostBinding('attr.data-dot-inode') inode: string | null = null;
    @HostBinding('attr.data-dot-type') dotType: string | null = null;
    @HostBinding('attr.data-dot-container') dotContainer: string | null = null;
    @HostBinding('attr.data-dot-on-number-of-pages') numberOfPages: string | null = null;
    @HostBinding('attr.data-dot-object') dotContent: string | null = null;

    ngOnChanges() {
        this.identifier = this.contentlet.identifier;
        this.baseType = this.contentlet.baseType;
        this.title = this.contentlet.title;
        this.inode = this.contentlet.inode;
        this.dotType = this.contentlet.contentType;
        this.dotContainer = this.container;
        this.numberOfPages = this.contentlet['onNumberOfPages'];
        this.dotContent = 'contentlet';
    }
}
