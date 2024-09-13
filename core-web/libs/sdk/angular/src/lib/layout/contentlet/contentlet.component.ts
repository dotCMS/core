import { ChangeDetectionStrategy, Component, HostBinding, Input, OnChanges } from '@angular/core';

import { DotCMSContentlet } from '../../models';

/**
 * This component is responsible to display a contentlet.
 *
 * @export
 * @class ContentletComponent
 * @implements {OnChanges}
 */
@Component({
    selector: 'dotcms-contentlet-wrapper',
    standalone: true,
    template: '<ng-content></ng-content>',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContentletComponent implements OnChanges {
    /**
     * The contentlet object containing content data.
     *
     * @type {DotCMSContentlet}
     * @memberof ContentletComponent
     */
    @Input({ required: true }) contentlet!: DotCMSContentlet;
    /**
     * The container data (as string) where the contentlet is located.
     *
     * @type {string}
     * @memberof ContentletComponent
     */
    @Input() container!: string;

    /**
     * The identifier of contentlet component.
     *
     * @type {(string | null)}
     * @memberof ContentletComponent
     */
    @HostBinding('attr.data-dot-identifier') identifier: string | null = null;
    /**
     * The base type of contentlet component.
     *
     * @type {(string | null)}
     * @memberof ContentletComponent
     */
    @HostBinding('attr.data-dot-basetype') baseType: string | null = null;
    /**
     * The title of contentlet component.
     *
     * @type {(string | null)}
     * @memberof ContentletComponent
     */
    @HostBinding('attr.data-dot-title') title: string | null = null;
    /**
     * The inode of contentlet component.
     *
     * @type {(string | null)}
     * @memberof ContentletComponent
     */
    @HostBinding('attr.data-dot-inode') inode: string | null = null;
    /**
     * The type of contentlet component.
     *
     * @type {(string | null)}
     * @memberof ContentletComponent
     */
    @HostBinding('attr.data-dot-type') dotType: string | null = null;
    /**
     * The container of contentlet component.
     *
     * @type {(string | null)}
     * @memberof ContentletComponent
     */
    @HostBinding('attr.data-dot-container') dotContainer: string | null = null;
    /**
     * The number of pages where the contentlet appears
     *
     * @type {(string | null)}
     * @memberof ContentletComponent
     */
    @HostBinding('attr.data-dot-on-number-of-pages') numberOfPages: string | null = null;
    /**
     * The content of contentlet component.
     *
     * @type {(string | null)}
     * @memberof ContentletComponent
     */
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
