import { Component, computed, input } from '@angular/core';

import { DotCMSShowWhenDirective } from '@dotcms/angular';
import { UVE_MODE } from '@dotcms/types';

import { SimpleWidget } from '../../types/contentlet.model';

@Component({
  selector: 'app-simple-widget',
  imports: [DotCMSShowWhenDirective],
  template: `
    @if (isTravelBot()) {
      <div
        class="bg-white rounded-lg shadow-sm p-8 text-center max-w-lg mx-auto my-6"
      >
        <h2 class="text-2xl font-bold text-gray-800 mb-4">
          WELCOME TO TRAVELBOT
        </h2>

        <p class="text-gray-600 mb-4">
          TravelBot is built with <span class="font-medium">dotAI</span>, the
          dotCMS suite of AI features.
        </p>

        <p class="text-gray-600">
          Please configure the dotAI App to enable dotAI and TravelBot.
        </p>
      </div>
    } @else {
        <div *dotCMSShowWhen="UVE_MODE.EDIT"
          class="p-4 mb-4 text-sm text-blue-800 rounded-lg bg-blue-50 dark:bg-gray-800 dark:text-blue-400"
          role="alert"
        >
          <h4>Simple Widget: {{ contentlet().widgetTitle }}</h4>
        </div>
    }
  `,
})
export class SimpleWidgetComponent {
  private readonly TRAVEL_BOT_KEY = '908b8a434ad7e539632b8db57f2967c0';

  contentlet = input.required<SimpleWidget>();
  UVE_MODE = UVE_MODE;

  isTravelBot = computed(
    () => this.contentlet().identifier === this.TRAVEL_BOT_KEY,
  );
}
