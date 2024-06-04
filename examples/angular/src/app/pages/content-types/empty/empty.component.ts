import { Component } from '@angular/core';

@Component({
  selector: 'app-empty',
  standalone: true,
  template: ` <div class="relative w-full bg-gray-200 h-12 flex justify-center items-center overflow-hidden">Dont have a component for this contentlet.</div> `,
  styles: '',
})
export class EmptyComponent {}
