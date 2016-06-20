import {Component} from "@angular/core";
import {HTTP_PROVIDERS, Http} from '@angular/http';
import {ApiRoot} from "../../api/persistence/ApiRoot";

@Component({
    selector: "ANGULAR_PORTLET3",
    template: `
        <h1>Porlet 1</h1>
        <br>
        <h1>This response is coming from a RESTful resource (/api/jwt/poc1): {{poc1Response?.Success}}</h1>
    `,
    providers: [HTTP_PROVIDERS]
})

export class ANGULAR_PORTLET3 {
    poc1Response:any;
    constructor(http:Http, apiRoot:ApiRoot) {
        http.get(`${apiRoot.baseUrl}api/jwt/poc1`).subscribe(res => {
            this.poc1Response = res.json();
        });
    }
}
