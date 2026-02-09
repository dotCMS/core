import { Component, ViewEncapsulation } from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-core-component',
    template: '<router-outlet />',
    standalone: false
})
export class MainCoreLegacyComponent {}
