import {Component, NgModule} from '@angular/core';
import {Subscription} from 'rxjs';
import {Treeable} from '../../core/treeable/shared/treeable.model';
import {SiteBrowserState} from '../../core/util/site-browser.state';
import {SettingsStorageService} from '../../core/util/settings-storage.service';
import {CommonModule} from '@angular/common';
import {InputTextModule} from 'primeng/components/inputtext/inputtext';
import {FormsModule} from '@angular/forms';

@Component({
    selector: 'treeable-detail',
    styleUrls: ['./../app.css'],
    template: `<div *ngIf="treeable.type=='file_asset'">
        <div>
            <img *ngIf="treeable.isImage() && treeable.extension!='ico'" src="{{dotCMSURL}}/contentAsset/image/{{treeable.identifier}}/fileAsset/filter/Thumbnail/thumbnail_w/225/thumbnail_h/225">
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Inode</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="inode" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.inode" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Identifier</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="identifier" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.identifier" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Filename</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="fileName" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.fileName" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Title</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="title" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.title" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Edititor</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="modUserName" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.modUserName" /></div>
        </div>

        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Path</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="path" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.path" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Mine Type</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="mimeType" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.mimeType" /></div>
        </div>
    </div>

    <div *ngIf="treeable.type=='folder'">
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Inode</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="inode" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.inode" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Identifier</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="identifier" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.identifier" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Name</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="name" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.name" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Title</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="title" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.title" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Path</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="path" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.path" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Show On Menu</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="showOnMenu" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.showOnMenu" /></div>
        </div>
        <div class="ui-g">
            <div class="ui-g-3"><label class="treeable-detail-label">Default File Type</label></div>
            <div class="ui-g-9"><input class="treeable-detail-input" id="defaultFileType" type="text" pInputText [disabled]="disabled" [(ngModel)]="treeable.defaultFileType" /></div>
        </div>
    </div>`
})
export class TreeableDetailComponent {

    dotCMSURL = '';
    treeable: Treeable = new Treeable();
    subscription: Subscription;

    constructor(
        private updateService: SiteBrowserState,
        private settingsStorageService: SettingsStorageService
    ) {
        if (settingsStorageService.getSettings() !== null) {this.dotCMSURL = settingsStorageService.getSettings().site; }
        if (updateService.getSelectedTreeable()) {this.treeable = updateService.getSelectedTreeable(); }
        this.subscription = updateService.currentTreeable
            .subscribe(treeable => {
                if (treeable) {
                    this.treeable = treeable;
                }else {
                    this.treeable = new Treeable();
                }
            });
        setTimeout(() => {
        }, 100);
    }

}

@NgModule({
    declarations: [TreeableDetailComponent],
    exports: [TreeableDetailComponent],
    imports: [CommonModule, FormsModule, InputTextModule],
})
export class DotcmsTreeableDetailModule { }
