import { Component, input } from '@angular/core';
import { DotCMSBasicContentlet } from '@dotcms/types';

@Component({
  selector: 'app-custom-no-component',
  standalone: true,
  template: `
    <div
      class="relative w-full bg-gray-200 h-12 flex justify-center items-center overflow-hidden"
    >
      No component for this "{{ contentlet().contentType }}" contentlet or
      widget
    </div>
  `,
  styles: '',
})
export class CustomNoComponent {
  contentlet = input.required<DotCMSBasicContentlet>();
}
