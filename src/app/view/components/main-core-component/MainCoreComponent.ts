import {Component, ViewEncapsulation} from '@angular/core';

@Component({
    directives: [],
    encapsulation: ViewEncapsulation.None,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'dot-main-core-component',
    template: '<router-outlet></router-outlet>',
})
export class MainCoreComponent {
}