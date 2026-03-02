import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { CardModule } from 'primeng/card';

import { pluck, take } from 'rxjs/operators';

import { DotLoginUserSystemInformation } from '@dotcms/dotcms-models';

import { DotLoginPageStateService } from '../shared/services/dot-login-page-state.service';

@Component({
    selector: 'dot-login-page-component',
    styleUrls: ['./dot-login-page.component.scss'],
    templateUrl: 'dot-login-page.component.html',
    imports: [RouterOutlet, CardModule]
})
/**
 * The login component allows set the background image and background color.
 */
export class DotLoginPageComponent implements OnInit {
    loginPageStateService = inject(DotLoginPageStateService);

    ngOnInit(): void {
        this.loginPageStateService
            .get()
            .pipe(take(1), pluck('entity'))
            .subscribe((dotLoginUserSystemInformation: DotLoginUserSystemInformation) => {
                document.body.style.backgroundColor =
                    dotLoginUserSystemInformation.backgroundColor || '';
                document.body.style.backgroundImage =
                    dotLoginUserSystemInformation.backgroundPicture
                        ? `url('${dotLoginUserSystemInformation.backgroundPicture}')`
                        : '';
            });
    }
}
