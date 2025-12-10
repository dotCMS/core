import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { CheckboxModule } from 'primeng/checkbox';

import { map, mergeMap } from 'rxjs/operators';

import { DotAccountService } from '@dotcms/app/api/services/dot-account-service';
import { DotCurrentUserService } from '@dotcms/data-access';
import {
    DotCurrentUser,
    DotPermissionsType,
    PermissionsType,
    UserPermissions
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';


@Component({
    selector: 'dot-onboarding-author',
    templateUrl: './onboarding-author.component.html',
    styleUrls: ['./onboarding-author.component.scss'],
    providers: [DotAccountService],
    standalone: true,
    imports: [AsyncPipe, DotMessagePipe, CheckboxModule, RouterLink]
})
export class DotOnboardingAuthorComponent implements OnInit {
    private dotAccountService = inject(DotAccountService);

    userData$: Observable<{
        username: string;
        showCreateContentLink: boolean;
        showCreateDataModelLink: boolean;
        showCreatePageLink: boolean;
        showCreateTemplateLink: boolean;
    }>;
    username: string;
    showCreateContentLink: boolean;
    showCreateDataModelLink: boolean;
    showCreatePageLink: boolean;
    showCreateTemplateLink: boolean;

    readonly #destroyRef = inject(DestroyRef);

    private dotCurrentUserService = inject(DotCurrentUserService);

    ngOnInit() {
        this.userData$ = this.dotCurrentUserService.getCurrentUser().pipe(
            mergeMap((user: DotCurrentUser) => {
                return this.dotCurrentUserService
                    .getUserPermissions(
                        user.userId,
                        [UserPermissions.WRITE],
                        [
                            PermissionsType.HTMLPAGES,
                            PermissionsType.STRUCTURES,
                            PermissionsType.TEMPLATES,
                            PermissionsType.CONTENTLETS
                        ]
                    )
                    .pipe(
                        map((permissionsType: DotPermissionsType) => {
                            return { user, permissions: permissionsType };
                        }),
                        map(
                            ({
                                user,
                                permissions
                            }: {
                                user: DotCurrentUser;
                                permissions: DotPermissionsType;
                            }) => {
                                return {
                                    username: user.givenName,
                                    showCreateContentLink:
                                        permissions[PermissionsType.CONTENTLETS].canWrite,
                                    showCreateDataModelLink:
                                        permissions[PermissionsType.STRUCTURES].canWrite,
                                    showCreatePageLink:
                                        permissions[PermissionsType.HTMLPAGES].canWrite,
                                    showCreateTemplateLink:
                                        permissions[PermissionsType.TEMPLATES].canWrite
                                };
                            }
                        )
                    );
            })
        );
    }

    /**
     * Hit the endpoint to show/hide the tool group in the menu.
     * @param {boolean} hide
     * @memberof DotStarterComponent
     */
    handleVisibility(hide: boolean): void {
        const subscription = hide
            ? this.dotAccountService.removeStarterPage()
            : this.dotAccountService.addStarterPage();

        subscription.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe();
    }
}
