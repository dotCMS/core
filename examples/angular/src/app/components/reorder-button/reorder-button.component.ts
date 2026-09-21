import { Component } from '@angular/core';

import { DotCMSShowWhenDirective } from '@dotcms/angular';
import { UVE_MODE } from '@dotcms/types';
import { reorderMenu } from '@dotcms/uve';

/**
 * Local component for rendering a list of contentlets outside the DotCmsLayout.
 *
 * @export
 * @class ContentletsComponent
 */
@Component({
  selector: 'app-reorder-button',
  imports: [DotCMSShowWhenDirective],
  template: `
    <ng-template [dotCMSShowWhen]="uveMode.EDIT">
      <button
        class="bg-[#426BF0] rounded-sm flex cursor-pointer border-none px-2 py-1 gap-2"
        (click)="reorderMenu()"
      >
        <svg
          id="Sort_24"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          xmlns="http://www.w3.org/2000/svg"
        >
          <rect
            width="24"
            height="24"
            stroke="none"
            fill="#000000"
            opacity="0"
          />
          <g transform="matrix(0.83 0 0 0.83 12 12)">
            <path
              [style]="{
                stroke: 'none',
                'stroke-width': 1,
                'stroke-dasharray': 'none',
                'stroke-linecap': 'butt',
                'stroke-dashoffset': 0,
                'stroke-linejoin': 'miter',
                'stroke-miterlimit': 4,
                fill: '#FFFFFF',
                fillRule: 'nonzero',
                opacity: 1,
              }"
              transform="translate(-13, -13)"
              d="M 13.003906 1 C 12.503906 1 12.253906 1.25 12.253906 1.25 C 12.253906 1.25 7.03125 5.25
            4.296875 9.25 C 4.0507181 9.625 3.921875 10 4.046875 10.375 C 4.171875 10.75 4.671875 11
            5.042969 11 L 20.957031 11 C 21.328125 11 21.828125 10.75 21.953125 10.375 C 22.078125
            10 21.953125 9.625 21.703125 9.25 C 18.96875 5.25 13.746094 1.25 13.746094 1.25 C
            13.746094 1.25 13.496094 1 13.003906 1 Z M 5.042969 15 C 4.671875 15 4.171875 15.25
            4.046875 15.625 C 3.925781 16 4.046875 16.375 4.296875 16.75 C 7.03125 20.75 12.253906
            24.75 12.253906 24.75 C 12.253906 24.75 12.503906 25 12.996094 25 C 13.496094 25
            13.746094 24.75 13.746094 24.75 C 13.746094 24.75 18.96875 20.75 21.703125 16.75 C
            21.953125 16.375 22.074219 16 21.953125 15.625 C 21.828125 15.25 21.328125 15 20.957031
            15 Z"
            />
          </g>
        </svg>
      </button>
    </ng-template>
  `,
})
export class ReorderButtonComponent {
  uveMode = UVE_MODE;

  reorderMenu() {
    reorderMenu();
  }
}
