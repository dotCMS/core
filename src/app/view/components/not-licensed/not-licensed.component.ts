import {Component, ViewEncapsulation} from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.None,

    selector: 'not-licensed-component',
    styleUrls: ['./not-licensed.component.scss'],
    templateUrl: 'not-licensed.component.html',
})
export class NotLicensedComponent {

}
