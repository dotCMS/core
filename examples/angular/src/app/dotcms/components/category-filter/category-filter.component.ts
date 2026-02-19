import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CategoryFilter } from '../../types/contentlet.model';

@Component({
  selector: 'app-category-filter',
  imports: [RouterLink],
  template: `
    @if (hasCategories()) {
      <div class="bg-white rounded-lg shadow-sm p-4">
        <h3 class="text-lg font-medium text-gray-900 mb-4">Categories</h3>
        <div class="space-y-2">
          @for (category of categories(); track category.url) {
            <div class="border-b border-gray-100 pb-2">
              <div class="flex items-center justify-between">
                <a
                  [routerLink]="category.url"
                  class="text-red-500 hover:text-red-600 transition-colors duration-200 py-1 block"
                >
                  {{ category.title }}
                </a>
              </div>
            </div>
          }
        </div>
      </div>
    }
  `,
})
export class CategoryFilterComponent {
  contentlet = input.required<CategoryFilter>();

  categories = computed(
    () => this.contentlet().widgetCodeJSON?.categories || [],
  );
  hasCategories = computed(
    () => this.categories() && this.categories().length > 0,
  );
}
