import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output, inject } from '@angular/core';

import { DialogModule } from 'primeng/dialog';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

import { DotPageToolsSeoState, DotPageToolsSeoStore } from './store/dot-page-tools-seo.store';

export type PageScannerToolType = 'a11y' | 'geo';

@Component({
    selector: 'dot-page-tools-seo',
    providers: [DotPageToolsService, DotPageToolsSeoStore],
    imports: [AsyncPipe, DialogModule, DotColorIconComponent, DotMessagePipe],
    templateUrl: './dot-page-tools-seo.component.html',
    styleUrls: ['./dot-page-tools-seo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageToolsSeoComponent {
    private dotPageToolsSeoStore = inject(DotPageToolsSeoStore);

    currentPageUrlParams = input<DotPageToolUrlParams>();
    showPageScanner = input<boolean>(false);

    scannerToolClick = output<PageScannerToolType>();

    dialogHeader: string;
    tools$: Observable<DotPageToolsSeoState> = this.dotPageToolsSeoStore.tools$;
    visible = false;

    public toggleDialog(): void {
        if (!this.visible) {
            this.dotPageToolsSeoStore.getTools(this.currentPageUrlParams());
        }

        this.visible = !this.visible;
    }

    onScannerToolClick(type: PageScannerToolType): void {
        this.scannerToolClick.emit(type);
    }
}
