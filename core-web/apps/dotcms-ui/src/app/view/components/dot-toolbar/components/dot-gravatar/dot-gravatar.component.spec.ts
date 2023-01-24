import * as md5 from 'md5';
import { Observable, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';

import { DotGravatarComponent } from './dot-gravatar.component';

import { DotAvatarModule } from '../../../_common/dot-avatar/dot-avatar.module';

@Component({
    selector: 'dot-test-component',
    template: `<dot-gravatar [email]="email" [size]="size"> </dot-gravatar>`
})
class HostTestComponent {
    @Input()
    email: string;

    @Input()
    size: number;
}

class DotGravatarServiceMock {
    getPhoto(): Observable<string> {
        return of('/avatar_url');
    }
}

describe('DotGravatarComponent', () => {
    let fixture: ComponentFixture<HostTestComponent>;
    let avatarComponent: DebugElement;
    let dotGravatarService: DotGravatarService;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [HostTestComponent, DotGravatarComponent],
            imports: [DotAvatarModule, CommonModule],
            providers: [{ provide: DotGravatarService, useClass: DotGravatarServiceMock }]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(HostTestComponent);
        fixture.detectChanges();
        avatarComponent = fixture.debugElement.query(By.css('dot-avatar'));

        dotGravatarService = fixture.debugElement.injector.get(DotGravatarService);
    });

    it('should have a dot-avatar', () => {
        expect(avatarComponent).not.toBeNull();
    });

    it('should set dot-avatar size', () => {
        fixture.componentInstance.size = 20;
        fixture.detectChanges();
        expect(avatarComponent.componentInstance.size).toEqual(20);
    });

    it('should set dot-avatar label', () => {
        fixture.componentInstance.email = 'a@a.com';
        fixture.detectChanges();
        expect(avatarComponent.componentInstance.label).toEqual('a@a.com');
    });

    it('should set dot-avatar label', () => {
        fixture.componentInstance.email = 'a@a.com';
        spyOn(dotGravatarService, 'getPhoto').and.callThrough();

        fixture.detectChanges();
        expect(avatarComponent.componentInstance.url).toEqual('/avatar_url');

        expect(dotGravatarService.getPhoto).toHaveBeenCalledWith(
            md5(fixture.componentInstance.email)
        );
    });
});
