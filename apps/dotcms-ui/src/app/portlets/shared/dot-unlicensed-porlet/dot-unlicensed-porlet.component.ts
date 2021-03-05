import { Component, Input } from '@angular/core';
import { DotUnlicensedPortlet } from '@portlets/dot-form-builder/resolvers/dot-form-resolver.service';

@Component({
    selector: 'dot-unlicensed-porlet',
    templateUrl: './dot-unlicensed-porlet.component.html',
    styleUrls: ['./dot-unlicensed-porlet.component.scss']
})
export class DotUnlicensedPorletComponent {
    @Input() data: DotUnlicensedPortlet;

    constructor() {}
}
