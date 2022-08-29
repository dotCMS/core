import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsListComponent } from './dot-experiments-list.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
const messageServiceMock = new MockDotMessageService({});
describe('ExperimentsListComponent', () => {
    let component: DotExperimentsListComponent;
    let fixture: ComponentFixture<DotExperimentsListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotMessagePipeModule],
            declarations: [DotExperimentsListComponent],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
