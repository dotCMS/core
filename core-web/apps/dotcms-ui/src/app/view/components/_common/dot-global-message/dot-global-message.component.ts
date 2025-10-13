import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectorRef,
    Component,
    HostBinding,
    OnDestroy,
    OnInit,
    inject
} from '@angular/core';

import { filter, takeUntil } from 'rxjs/operators';

import { DotEventsService } from '@dotcms/data-access';
import { DotEvent, DotGlobalMessage } from '@dotcms/dotcms-models';
import { DotSpinnerComponent } from '@dotcms/ui';

/**
 * Set a listener to display Global Messages in the main top toolbar
 * and hold icon classes to display them with the message.
 * @export
 * @class DotGlobalMessageComponent
 */
@Component({
    selector: 'dot-global-message',
    templateUrl: './dot-global-message.component.html',
    styleUrls: ['./dot-global-message.component.scss'],
    imports: [CommonModule, DotSpinnerComponent]
})
export class DotGlobalMessageComponent implements OnInit, OnDestroy {
    private dotEventsService = inject(DotEventsService);
    private cd = inject(ChangeDetectorRef);

    @HostBinding('class')
    get classes(): string {
        return `${this.visibility ? 'dot-global-message--visible' : ''} ${this.message.type}`;
    }

    message: DotGlobalMessage = { value: '' };

    private visibility = false;
    private icons = {
        loading: 'loading',
        success: 'pi pi-check-circle',
        error: 'pi pi-exclamation-circle',
        warning: 'pi pi-exclamation-triangle'
    };
    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    ngOnInit() {
        this.dotEventsService
            .listen('dot-global-message')
            .pipe(
                filter((event: DotEvent<DotGlobalMessage>) => !!event.data),
                takeUntil(this.destroy$)
            )
            .subscribe((event: DotEvent<DotGlobalMessage>) => {
                this.message = event.data;
                this.visibility = true;
                this.message.icon = this.icons[this.message.type] || '';

                if (this.message.life) {
                    setTimeout(() => {
                        this.visibility = false;
                        this.cd.markForCheck();
                    }, this.message.life);
                }
            });
    }
}
