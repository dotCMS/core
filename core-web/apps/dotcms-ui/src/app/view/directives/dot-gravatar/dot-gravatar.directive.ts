import { ChangeDetectorRef, Directive, Input, OnInit } from '@angular/core';

import { Avatar } from 'primeng/avatar';

import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';

@Directive({
    selector: 'p-avatar[dotGravatar]',
    standalone: true
})
export class DotGravatarDirective implements OnInit {
    @Input() readonly email: string;

    constructor(
        private avatar: Avatar,
        private cd: ChangeDetectorRef,
        private gravatarService: DotGravatarService
    ) {
        this.avatar.shape = 'circle';
    }

    ngOnInit(): void {
        this.gravatarService.getPhoto(this.email).subscribe(
            (url: string) => {
                this.avatar.image = url;
                this.cd.detectChanges();
            },
            () => {
                this.avatar.label = this.email[0]?.toUpperCase();
                this.cd.detectChanges();
            }
        );
    }
}
