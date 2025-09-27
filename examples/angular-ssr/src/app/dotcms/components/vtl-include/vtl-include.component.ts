import { Component, input } from '@angular/core';

import { DotCMSShowWhenDirective } from '@dotcms/angular';
import { UVE_MODE } from '@dotcms/types';

import { DestinationListingComponent } from './components/destination-listing/destination-listing.component';

import { VTLIncludeWithVariations } from '../../types/contentlet.model';

@Component({
  selector: 'app-vtl-include',
  imports: [DotCMSShowWhenDirective, DestinationListingComponent],
  template: `
    @switch (contentlet().componentType) {
      @case ('destinationListing') {
        <!-- There is a gap between angular templates and typescript, although the type is correct
          Angular is not able to infer the type correctly. So we use $any to tell Angular to ignore the type -->
        <app-destination-listing
          [widgetCodeJSON]="$any(contentlet()).widgetCodeJSON"
        />
      }
      @default {
          <div *dotCMSShowWhen="UVE_MODE.EDIT" class="bg-blue-100 p-4">
            <h4>
              No Component Type:
              {{ contentlet().componentType || 'generic' }} Found for VTL
              Include
            </h4>
          </div>
      }
    }
  `,
})
export class VtlIncludeComponent {
  contentlet = input.required<VTLIncludeWithVariations>();

  UVE_MODE = UVE_MODE;
}
