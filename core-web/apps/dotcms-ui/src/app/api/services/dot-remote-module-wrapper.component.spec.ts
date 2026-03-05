import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { DotRemoteModuleWrapperComponent } from './dot-remote-module-wrapper.component';

describe('DotRemoteModuleWrapperComponent', () => {
    let fixture: ComponentFixture<DotRemoteModuleWrapperComponent>;
    let component: DotRemoteModuleWrapperComponent;
    let mockCleanup: jest.Mock;
    let mockMount: jest.Mock;

    function createComponent(mountFn?: unknown): void {
        TestBed.configureTestingModule({
            imports: [DotRemoteModuleWrapperComponent],
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            data: mountFn !== undefined ? { mount: mountFn } : {}
                        }
                    }
                }
            ]
        });

        fixture = TestBed.createComponent(DotRemoteModuleWrapperComponent);
        component = fixture.componentInstance;
    }

    beforeEach(() => {
        mockCleanup = jest.fn();
        mockMount = jest.fn().mockResolvedValue(mockCleanup);
    });

    it('should call mount with the container element', fakeAsync(() => {
        createComponent(mockMount);
        fixture.detectChanges();
        tick();

        expect(mockMount).toHaveBeenCalledWith(component.container.nativeElement);
    }));

    it('should store and call cleanup on destroy', fakeAsync(() => {
        createComponent(mockMount);
        fixture.detectChanges();
        tick();

        component.ngOnDestroy();

        expect(mockCleanup).toHaveBeenCalled();
    }));

    it('should not throw when mount is not provided in route data', fakeAsync(() => {
        createComponent();
        fixture.detectChanges();
        tick();

        expect(mockMount).not.toHaveBeenCalled();
        expect(() => component.ngOnDestroy()).not.toThrow();
    }));

    it('should handle mount error gracefully', fakeAsync(() => {
        const errorMount = jest.fn().mockRejectedValue(new Error('mount failed'));
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

        createComponent(errorMount);
        fixture.detectChanges();
        tick();

        expect(consoleSpy).toHaveBeenCalledWith(
            '[DotRemoteModuleWrapper] Failed to mount remote module:',
            expect.any(Error)
        );

        // cleanup should not have been set
        expect(() => component.ngOnDestroy()).not.toThrow();
        consoleSpy.mockRestore();
    }));

    it('should handle cleanup error gracefully', fakeAsync(() => {
        const throwingCleanup = jest.fn().mockImplementation(() => {
            throw new Error('cleanup failed');
        });
        const errorMount = jest.fn().mockResolvedValue(throwingCleanup);
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

        createComponent(errorMount);
        fixture.detectChanges();
        tick();

        expect(() => component.ngOnDestroy()).not.toThrow();
        expect(consoleSpy).toHaveBeenCalledWith(
            '[DotRemoteModuleWrapper] Failed to unmount remote module:',
            expect.any(Error)
        );
        consoleSpy.mockRestore();
    }));
});
