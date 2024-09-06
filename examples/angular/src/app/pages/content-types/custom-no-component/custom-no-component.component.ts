import { Component } from '@angular/core';

@Component({
  selector: 'app-custom-no-component',
  standalone: true,
  template: `
    <div
      class="relative w-full bg-gray-200 h-12 flex justify-center items-center overflow-hidden"
    >
      No component for this contentlet or widget
    </div>
  `,
  styles: '',
})
export class CustomNoComponent {}
