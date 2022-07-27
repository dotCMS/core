import { NgModule } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { NgxTiptapModule } from "@dotcms/block-editor";
import {
    DotEditBlockEditorComponent
} from "@portlets/dot-edit-page/content/components/dot-edit-block-editor/dot-edit-block-editor.component";
import {CommonModule} from "@angular/common";

@NgModule({
    declarations:[DotEditBlockEditorComponent],
    exports:[DotEditBlockEditorComponent],
    imports: [
        FormsModule,
        NgxTiptapModule,
        CommonModule
    ]
})
export class DotEditBlockEditorModule {}
