import { Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

// import { DotCounterComponent } from './components/dot-counter/dot-counter.component';

@Component({
    standalone: true,
    imports: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    selector: 'dotcms-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent {
    title = 'angular-elements';

    alertFlag(flag: Event) {
        alert(`Current flag: ${flag}`);
    }
}
