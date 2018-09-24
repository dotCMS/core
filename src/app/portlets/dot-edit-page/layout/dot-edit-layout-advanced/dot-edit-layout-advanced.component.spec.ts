import { of as observableOf, Observable } from 'rxjs';
import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { LoginService } from 'dotcms-js/dotcms-js';

import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotEditLayoutAdvancedComponent } from './dot-edit-layout-advanced.component';
import { DotMenuService } from '@services/dot-menu.service';
import { IFrameModule } from '@components/_common/iframe';
import { LoginServiceMock, mockUser } from '../../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { IframeComponent } from '@components/_common/iframe/iframe-component';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
import { DotPageStateServiceMock } from '../../../../test/dot-page-state.service.mock';
import { DotRenderedPageState } from '@models/dot-rendered-page-state.model';
import { mockDotRenderedPage } from '../../../../test/dot-rendered-page.mock';

class DotMenuServiceMock {
    getDotMenuId(): Observable<string> {
        return observableOf('123');
    }
}

let dotGlobalMessageService: DotGlobalMessageService;
let dotPageStateService: DotPageStateService;

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.saved': 'Salvado'
});

const basicModule = {
    imports: [IFrameModule, RouterTestingModule],
    declarations: [DotEditLayoutAdvancedComponent],
    providers: [
        DotGlobalMessageService,
        {
            provide: LoginService,
            useClass: LoginServiceMock
        },
        {
            provide: DotMenuService,
            useClass: DotMenuServiceMock
        },
        {
            provide: DotMessageService,
            useValue: messageServiceMock
        },
        {
            provide: DotPageStateService,
            useClass: DotPageStateServiceMock
        }
    ]
};

describe('DotEditLayoutAdvancedComponent - Basic', () => {
    let component: DotEditLayoutAdvancedComponent;
    let fixture: ComponentFixture<DotEditLayoutAdvancedComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule(basicModule);
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotEditLayoutAdvancedComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);

        fixture.detectChanges();
        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
        dotPageStateService = de.injector.get(DotPageStateService);
    });

    it('should have dot-iframe component', () => {
        const dotIframe = de.query(By.css('dot-iframe'));
        expect(dotIframe).toBeTruthy();
    });

    it('should set the dot-iframe url correctly', () => {
        let result: string;
        component.url.subscribe((url) => {
            result = url;
        });
        expect(result).toEqual(
            // tslint:disable-next-line:max-line-length
            'c/portal/layout?ng=true&p_l_id=123&p_p_id=templates&p_p_action=1&p_p_state=maximized&_templates_struts_action=%2Fext%2Ftemplates%2Fedit_template&_templates_cmd=edit&inode=123&r=0d618b02-f184-48fe-88f4-e98563ee6e9e'
        );
    });

    it('should handle onload even from iframe', () => {
        spyOn(component, 'onLoad');
        const dotIframe = de.query(By.css('dot-iframe')).componentInstance;
        dotIframe.load.emit('whaterer');
        expect(component.onLoad).toHaveBeenCalled();
    });

    xit('should handle custom events from the iframe', () => {
        spyOn(dotGlobalMessageService, 'display');
        spyOn(dotPageStateService, 'get');

        const dotIframe: IframeComponent = de.query(By.css('dot-iframe')).componentInstance;
        const iframe: any = dotIframe.iframeElement.nativeElement;

        dotIframe.load.emit({ target: iframe });

        const customEvent = document.createEvent('CustomEvent');
        customEvent.initCustomEvent('ng-event', false, false, {
            name: 'advanced-template-saved',
            data: {}
        });
        iframe.contentWindow.document.dispatchEvent(customEvent);

        expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Salvado');
        expect(dotPageStateService.get).toHaveBeenCalledWith(mockDotRenderedPage.page.pageURI);
    });
});
