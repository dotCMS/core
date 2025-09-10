import { Component, input } from '@angular/core';

import { DotCMSShowWhenDirective } from '@dotcms/angular';

import { UVE_MODE } from '@dotcms/types';
import { VTLIncludeWithVariations } from '../../shared/contentlet.model';
import { DestinationListingComponent } from './components/destination-listing/destination-listing.component';

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
        <ng-container [dotCMSShowWhen]="UVE_MODE.EDIT">
          <div class="bg-blue-100 p-4">
            <h4>
              No Component Type:
              {{ contentlet().componentType || 'generic' }} Found for VTL
              Include
            </h4>
          </div>
        </ng-container>
      }
    }
  `,
})
export class VtlIncludeComponent {
  contentlet = input.required<VTLIncludeWithVariations>();

  UVE_MODE = UVE_MODE;
}
