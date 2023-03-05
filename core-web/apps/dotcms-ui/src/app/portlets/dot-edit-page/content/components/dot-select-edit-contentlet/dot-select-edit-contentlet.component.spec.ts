import { of } from 'rxjs';

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotCopyContentService } from '@dotcms/data-access';

import { DotSelectEditContentletComponent } from './dot-select-edit-contentlet.component';

describe('DotSelectEditContentletComponent', () => {
    let component: DotSelectEditContentletComponent;
    /**
     * let dotCopyContentService: DotCopyContentService;
     * let dynamicDialogConfig: DynamicDialogConfig;
     * let dynamicDialogRef: DynamicDialogRef;
     */
    let fixture: ComponentFixture<DotSelectEditContentletComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotSelectEditContentletComponent, DotMessagePipeModule],
            providers: [
                DotHttpErrorManagerService,
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: jasmine.createSpy()
                    }
                },
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {}
                    }
                },
                {
                    provide: DotCopyContentService,
                    useValue: {
                        copyContentInPage: of({})
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotSelectEditContentletComponent);
        component = fixture.componentInstance;

        /**
         * dotCopyContentService = TestBed.inject(DotCopyContentService);
         * dynamicDialogConfig = TestBed.inject(DynamicDialogConfig);
         * dynamicDialogRef = TestBed.inject(DynamicDialogRef);
         */

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
