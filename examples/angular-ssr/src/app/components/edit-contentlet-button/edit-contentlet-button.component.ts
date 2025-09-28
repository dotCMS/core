import { Component, HostBinding, input } from '@angular/core';

import { DotCMSShowWhenDirective } from '@dotcms/angular';
import { UVE_MODE } from '@dotcms/types';
import { editContentlet } from '@dotcms/uve';

import { Contentlet } from '../../dotcms/types/contentlet.model';

/**
 * Local component for rendering a list of contentlets outside the DotCmsLayout.
 *
 * @export
 * @class ContentletsComponent
 */
@Component({
  selector: 'app-edit-contentlet-button',
  imports: [DotCMSShowWhenDirective],
  template: `
    <button
      *dotCMSShowWhen="uveMode.EDIT"
      (click)="editContentlet(contentlet())"
      class="bg-teal-400 text-white text-sm rounded-md py-1 px-3 shadow-md hover:bg-teal-500 cursor-pointer"
    >
      Edit
    </button>
  `,
})
export class EditContentletButtonComponent {
  contentlet = input.required<Contentlet>();

  uveMode = UVE_MODE;

  @HostBinding('class')
  hostClass = 'absolute bottom-2 right-2 z-10';

  editContentlet(contentlet: Contentlet) {
    editContentlet(contentlet);
  }
}
