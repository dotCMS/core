import {Component, ViewEncapsulation} from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.None,

    selector: 'not-licensed-component',
    styles: [require('./not-licensed-component.scss')],
    templateUrl: 'not-licensed-component.html',
})
export class NotLicensedComponent {

}
