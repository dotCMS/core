import { Component, ViewEncapsulation } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-core-component',
    template: '<router-outlet></router-outlet>',
    imports: [RouterOutlet]
})
export class MainCoreLegacyComponent {}
