import {Component} from "@angular/core";
// import {FooterComponent} from "./FooterComponent";
// import {HeaderComponent} from "./HeaderComponent";

@Component({
    selector: "app",
    template: `
    <div class="container">
        <h1>App</h1>
        <router-outlet></router-outlet>
    </div>
    `,
    providers: [],
    directives: []
})
export class AppComponent {
    constructor() {
    }
}