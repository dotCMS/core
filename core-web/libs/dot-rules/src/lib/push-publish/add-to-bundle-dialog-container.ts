import {
    Component,
    ChangeDetectionStrategy,
    Input,
    Output,
    EventEmitter,
    OnChanges
} from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { BundleService, IBundle } from '../services/bundle-service';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'cw-add-to-bundle-dialog-container',
    template: `<cw-add-to-bundle-dialog-component
        [bundleStores]="bundleService.bundles$ | async"
        [hidden]="hidden"
        [errorMessage]="errorMessage | async"
        (cancel)="onClose()"
        (addToBundle)="addToBundle($event)"
    ></cw-add-to-bundle-dialog-component>`
})
// tslint:disable-next-line:component-class-suffix
export class AddToBundleDialogContainer implements OnChanges {
    @Input() assetId: string;
    @Input() hidden = false;

    @Output() close: EventEmitter<{ isCanceled: boolean }> = new EventEmitter(false);
    @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);

    errorMessage: BehaviorSubject<string> = new BehaviorSubject(null);

    constructor(public bundleService: BundleService) {}

    ngOnChanges(change): void {
        if (change.hidden && !this.hidden) {
            this.bundleService.loadBundleStores();
        }
    }

    addToBundle(bundle: IBundle): void {
        this.bundleService.addRuleToBundle(this.assetId, bundle).subscribe((result: any) => {
            if (!result.errors) {
                this.close.emit({ isCanceled: false });
                this.errorMessage.next(null);
                this.bundleService.loadBundleStores();
            } else {
                this.errorMessage.next(result.errors);
            }
        });
    }

    onClose(): void {
        this.hidden = true;
        this.close.emit(null);
        this.errorMessage.next(null);
    }
}
