import * as md5 from 'md5';
import { Observable, of } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';

import { DotAvatarDirective } from '@directives/dot-avatar/dot-avatar.directive';
import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';

import { DotGravatarComponent } from './dot-gravatar.component';

@Component({
    selector: 'dot-test-component',
    template: `<dot-gravatar [email]="email"> </dot-gravatar>`
})
class HostTestComponent {
    email: string;
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

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [HostTestComponent],
            imports: [DotGravatarComponent, AsyncPipe, DotAvatarDirective, AvatarModule],
            providers: [{ provide: DotGravatarService, useClass: DotGravatarServiceMock }]
        }).compileComponents();

        fixture = TestBed.createComponent(HostTestComponent);
        fixture.detectChanges();
        avatarComponent = fixture.debugElement.query(By.css('p-avatar'));
        dotGravatarService = fixture.debugElement.injector.get(DotGravatarService);
    });

    it('should have a p-avatar', () => {
        expect(avatarComponent).not.toBeNull();
    });

    it('should set p-avatar label', (done) => {
        fixture.componentInstance.email = 'a@a.com';
        fixture.detectChanges();

        setTimeout(() => {
            fixture.detectChanges();
            expect(avatarComponent.componentInstance.label).toEqual('A');
            done();
        }, 100);
    });

    it('should set p-avatar image', () => {
        fixture.componentInstance.email = 'a@a.com';
        spyOn(dotGravatarService, 'getPhoto').and.callThrough();

        fixture.detectChanges();

        expect(avatarComponent.componentInstance.image).toEqual('/avatar_url');

        expect(dotGravatarService.getPhoto).toHaveBeenCalledWith(
            md5(fixture.componentInstance.email)
        );
    });
});
