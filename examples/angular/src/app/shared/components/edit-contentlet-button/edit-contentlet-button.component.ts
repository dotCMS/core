import { Component, HostBinding, input } from '@angular/core';

import { DatePipe, NgOptimizedImage } from '@angular/common';

import { Contentlet } from '../../models';
import { DotCMSShowWhenDirective } from '@dotcms/angular/next';
import { editContentlet } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/types';

/**
 * Local component for rendering a list of contentlets outside the DotCmsLayout.
 *
 * @export
 * @class ContentletsComponent
 */
@Component({
    selector: 'app-edit-contentlet-button',
    standalone: true,
    imports: [DotCMSShowWhenDirective],
    template: `<ng-template [dotCMSShowWhen]="uveMode.EDIT">
        <button
            (click)="editContentlet(contentlet())"
            class="bg-red-400 text-white text-sm rounded-md py-1 px-3 shadow-md hover:bg-red-500 cursor-pointer">
            Edit
        </button>
    </ng-template> `
})
export class EditContentletButtonComponent {
    contentlet = input.required<Contentlet>();

    uveMode = UVE_MODE;

    @HostBinding('class')
    hostClass = 'absolute bottom-0 right-2 z-10';

    editContentlet(contentlet: Contentlet) {
        editContentlet(contentlet);
    }
}
