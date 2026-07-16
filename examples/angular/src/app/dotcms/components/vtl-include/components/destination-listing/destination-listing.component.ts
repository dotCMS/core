
import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { EditContentletButtonComponent } from '../../../../../components/edit-contentlet-button/edit-contentlet-button.component';
import { DestinationListingWidgetJSON } from '../../../../types/contentlet.model';

@Component({
  selector: 'app-destination-listing',
  templateUrl: './destination-listing.component.html',
  imports: [RouterLink, NgOptimizedImage, EditContentletButtonComponent],
})
export class DestinationListingComponent {
  widgetCodeJSON = input.required<DestinationListingWidgetJSON>();

  destinations = computed(() => this.widgetCodeJSON().destinations || []);

  currentYear = new Date().getFullYear();

  hasDestinations = computed(
    () =>
      this.widgetCodeJSON().destinations &&
      this.widgetCodeJSON().destinations.length > 0,
  );
}
