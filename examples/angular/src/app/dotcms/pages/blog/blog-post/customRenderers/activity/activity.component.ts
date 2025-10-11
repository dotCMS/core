import { Component, Input } from '@angular/core';

// You can define the type of the contentlet in the component
interface Activity {
  title: string;
  description: string;
}

@Component({
  selector: 'app-activity',
  template: '<div>{{ node.title }}</div>',
  standalone: true,
  styles: `
    :host {
      display: block;
    }
  `,
})
export class ActivityComponent {
  @Input() node!: Activity;
}
