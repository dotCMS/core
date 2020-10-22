import { Component, OnInit } from '@angular/core';
import { DotLoginUserSystemInformation } from '@models/dot-login';
import { pluck, take } from 'rxjs/operators';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';

@Component({
    selector: 'dot-login-page-component',
    styleUrls: ['./dot-login-page.component.scss'],
    templateUrl: 'dot-login-page.component.html'
})
/**
 * The login component allows set the background image and background color.
 */
export class DotLoginPageComponent implements OnInit {
    constructor(public loginPageStateService: DotLoginPageStateService) {}

    ngOnInit(): void {
        this.loginPageStateService
            .get()
            .pipe(take(1), pluck('entity'))
            .subscribe((dotLoginUserSystemInformation: DotLoginUserSystemInformation) => {
                document.body.style.backgroundColor =
                    dotLoginUserSystemInformation.backgroundColor || '';
                document.body.style.backgroundImage = dotLoginUserSystemInformation.backgroundPicture
                    ? `url('${dotLoginUserSystemInformation.backgroundPicture}')`
                    : '';
            });
    }
}
