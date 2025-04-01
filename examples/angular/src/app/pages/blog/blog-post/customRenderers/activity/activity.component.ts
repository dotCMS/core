import { Component, Input } from "@angular/core";

@Component({
    selector: 'app-activity',
    template: '<div>{{contentlet.title}}</div>',
    standalone: true,
    styles: `
        :host {
            display: block;
        }
    `
})
export class ActivityComponent {
    @Input() contentlet!: any;

}