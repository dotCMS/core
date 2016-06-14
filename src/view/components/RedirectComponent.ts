import {Component} from "@angular/core";
import {QueryParams, Router } from "@ngrx/router";
import {Observable} from "rxjs/Observable";

@Component({
    selector: "redirect",
    template: `
        <h1>RedirectComponent</h1>
    `,
    providers: []
})

export class RedirectComponent {
    constructor(router:Router, queryParams: QueryParams) {

        let id: Observable<string> = queryParams.pluck<string>('id')
            .forEach( value => router.replace(`/html/ng/p/${value}`));
    }
}