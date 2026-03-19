import {
    AfterViewInit,
    Component,
    ElementRef,
    inject,
    NgZone,
    OnDestroy,
    ViewChild
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';

type MountFn = (el: HTMLElement) => Promise<() => void>;

/**
 * Wrapper component that hosts a remote Module Federation module.
 *
 * Remote modules export a `mount(element)` function that bootstraps
 * their own Angular app inside the provided DOM element. This component
 * provides that element and manages the mount/unmount lifecycle.
 *
 * The mount is run outside of the host's NgZone to prevent zone conflicts
 * between the host and remote Angular runtimes.
 */
@Component({
    selector: 'dot-remote-module-wrapper',
    standalone: true,
    template: '<div #container style="width: 100%; height: 100%;"></div>'
})
export class DotRemoteModuleWrapperComponent implements AfterViewInit, OnDestroy {
    @ViewChild('container', { static: true }) container!: ElementRef<HTMLElement>;

    private readonly route = inject(ActivatedRoute);
    private readonly ngZone = inject(NgZone);
    private destroyFn?: () => void;

    ngAfterViewInit(): void {
        const mountFn = this.route.snapshot.data['mount'] as MountFn | undefined;

        if (mountFn) {
            // Run outside the host's NgZone so the remote Angular app
            // bootstraps with its own zone and doesn't conflict.
            this.ngZone.runOutsideAngular(() => {
                mountFn(this.container.nativeElement)
                    .then((cleanup) => {
                        this.destroyFn = cleanup;
                    })
                    .catch((err) => {
                        console.error(
                            '[DotRemoteModuleWrapper] Failed to mount remote module:',
                            err
                        );
                    });
            });
        }
    }

    ngOnDestroy(): void {
        try {
            this.destroyFn?.();
        } catch (err) {
            console.error('[DotRemoteModuleWrapper] Failed to unmount remote module:', err);
        }
    }
}
