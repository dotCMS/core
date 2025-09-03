import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';
import { DestinationListingWidgetJSON } from '../../../../shared/contentlet.model';
import { EditContentletButtonComponent } from '../../../../shared/components/edit-contentlet-button/edit-contentlet-button.component';

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
