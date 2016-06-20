
import {Component} from "@angular/core";
import {HTTP_PROVIDERS, Http} from '@angular/http';

@Component({
    selector: "ANGULAR_PORTLET4",
    template: `
        <h1>Porlet 2</h1>
        <br>
        <h1>This response is coming from a RESTful resource (/api/jwt/poc2): {{poc2Response?.Success}}</h1>
    `,
    providers: [HTTP_PROVIDERS]
})

export class ANGULAR_PORTLET4 {
    poc2Response: any;
    constructor(http:Http) {
        http.get('/api/jwt/poc2').subscribe(res => {
            this.poc2Response = res.json();
        });
    }
}