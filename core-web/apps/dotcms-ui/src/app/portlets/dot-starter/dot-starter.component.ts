import { Component } from "@angular/core"

import { DotOnboardingDev } from "./components/onboarding-dev/onboarding-dev.component";

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss'],
    imports: [DotOnboardingDev]
})
export class DotStarterComponent {

}
