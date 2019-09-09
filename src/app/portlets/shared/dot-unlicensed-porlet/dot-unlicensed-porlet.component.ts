import { Component, OnInit, Input } from '@angular/core';
import { DotUnlicensedPortlet } from '@portlets/dot-form-builder/resolvers/dot-form-resolver.service';
import { DotMessageService } from '@services/dot-messages-service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'dot-unlicensed-porlet',
    templateUrl: './dot-unlicensed-porlet.component.html',
    styleUrls: ['./dot-unlicensed-porlet.component.scss']
})
export class DotUnlicensedPorletComponent implements OnInit {
    @Input() data: DotUnlicensedPortlet;

    requestLicenseLabel$: Observable<string>;

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.requestLicenseLabel$ = this.dotMessageService
            .getMessages(['request.a.trial.license'])
            .pipe(
                map((messages: { [key: string]: string }) => messages['request.a.trial.license'])
            );
    }
}
