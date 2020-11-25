import { TestBed } from '@angular/core/testing';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { of } from 'rxjs';
import { DotTemplateCreateEditResolver } from './dot-template-create-edit.resolver';

describe('DotTemplateDesignerService', () => {
    let service: DotTemplateCreateEditResolver;
    let templateService: DotTemplatesService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotTemplateCreateEditResolver,
                {
                    provide: DotTemplatesService,
                    useValue: {
                        getById: jasmine.createSpy().and.returnValue(
                            of({
                                this: {
                                    is: 'a page'
                                }
                            })
                        )
                    }
                }
            ]
        });
        service = TestBed.inject(DotTemplateCreateEditResolver);
        templateService = TestBed.inject(DotTemplatesService);
    });

    it('should return page by id from router', (done) => {
        service
            .resolve(
                {
                    paramMap: {
                        get() {
                            return 'ID';
                        }
                    }
                } as any,
                null
            )
            .subscribe((res) => {
                expect(templateService.getById).toHaveBeenCalledWith('ID');
                expect<any>(res).toEqual({ this: { is: 'a page' } });
                done();
            });
    });
});
