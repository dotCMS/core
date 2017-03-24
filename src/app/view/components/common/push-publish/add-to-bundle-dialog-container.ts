import {Component, ChangeDetectionStrategy, Input, Output, EventEmitter} from '@angular/core';
import {AddToBundleDialogComponent} from './add-to-bundle-dialog-component';
import {BehaviorSubject} from 'rxjs/Rx';
import {BundleService, IBundle} from '../../../../api/services/bundle-service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'cw-add-to-bundle-dialog-container',
  template: `
  <cw-add-to-bundle-dialog-component
  [bundleStores]="bundleService.bundles$ | async"
  [hidden]="hidden"
  [errorMessage]="errorMessage | async"
  (cancel)="hidden = true; close.emit($event); errorMessage = null;"
  (addToBundle)="addToBundle($event)"
  ></cw-add-to-bundle-dialog-component>`
})
export class AddToBundleDialogContainer {
  @Input() assetId: string;
  @Input() hidden = false;

  @Output() close: EventEmitter<{isCanceled: boolean}> = new EventEmitter(false);
  @Output() cancel: EventEmitter<boolean> = new EventEmitter(false);

  errorMessage: BehaviorSubject<string> = new BehaviorSubject(null);
  bundlesLoaded = false;

  constructor(public bundleService: BundleService) {

  }

  ngOnChanges(change): void {
    if (change.hidden && !this.hidden && !this.bundlesLoaded) {
      this.bundlesLoaded = true;
      this.bundleService.loadBundleStores();
    }
  }

  addToBundle(bundle: IBundle): void {
    this.bundleService.addRuleToBundle(this.assetId, bundle).subscribe((result: any) => {
      if (!result.errors) {
        this.close.emit({isCanceled: false});
        this.errorMessage = null;
      } else {
        this.errorMessage.next(result.errors);
      }
    });
  }
}
