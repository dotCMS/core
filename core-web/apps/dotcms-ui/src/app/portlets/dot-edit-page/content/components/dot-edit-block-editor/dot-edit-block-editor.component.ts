import {Component, Injector, OnInit, ViewContainerRef} from '@angular/core';
import {DynamicDialogConfig, DynamicDialogRef} from "primeng/dynamicdialog";
import {Editor} from "@tiptap/core";
import StarterKit from "@tiptap/starter-kit";
import {
  ActionsMenu,
  BubbleLinkFormExtension,
  ContentletBlock, DotBubbleMenuExtension,
  DragHandler,
  ImageBlock,
  ImageUpload
} from "@dotcms/block-editor";
import {Underline} from "@tiptap/extension-underline";
import {TextAlign} from "@tiptap/extension-text-align";
import {Highlight} from "@tiptap/extension-highlight";
import {Link} from "@tiptap/extension-link";
import Placeholder from "@tiptap/extension-placeholder";

@Component({
  selector: 'dot-dot-edit-block-editor',
  templateUrl: './dot-edit-block-editor.component.html',
  styleUrls: ['./dot-edit-block-editor.component.scss']
})
export class DotEditBlockEditorComponent implements OnInit {
  data: any  = {};
  editor: Editor;
  value = '';

  constructor(public ref: DynamicDialogRef, public config: DynamicDialogConfig, private injector: Injector, public viewContainerRef: ViewContainerRef) { }

  ngOnInit(): void {
    this.data = this.config.data;

    this.editor = new Editor({
      extensions: [
        StarterKit,
        ContentletBlock(this.injector),
        ImageBlock(this.injector),
        ActionsMenu(this.viewContainerRef),
        DragHandler(this.viewContainerRef),
        ImageUpload(this.injector, this.viewContainerRef),
        BubbleLinkFormExtension(this.injector, this.viewContainerRef),
        DotBubbleMenuExtension(this.viewContainerRef),
        // Marks Extensions
        Underline,
        TextAlign.configure({ types: ['heading', 'paragraph', 'listItem', 'dotImage'] }),
        Highlight.configure({ HTMLAttributes: { style: 'background: #accef7;' } }),
        Link.configure({ openOnClick: true }),
        Placeholder.configure({
          placeholder: ({ node }) => {
            if (node.type.name === 'heading') {
              return `${this.toTitleCase(node.type.name)} ${node.attrs.level}`;
            }

            return 'Type "/" for commmands';
          }
        })
      ]
    });

    this.value = this.data.content;

    this.setEditorStorageData();
  }

  // Here we create the dotConfig name space
  // to storage information in the editor.
  private setEditorStorageData() {
    console.log('TODO: setupt storage');
    this.editor.storage.dotConfig = {
      lang: this.data.lang,
      allowedContentTypes: ''
    };
    // this.editor.storage.dotConfig = {
    //   lang: this.lang,
    //   allowedContentTypes: this.allowedContentTypes
    // };
  }

  private toTitleCase(str): void {
    return str.replace(/\p{L}+('\p{L}+)?/gu, function (txt) {
      return txt.charAt(0).toUpperCase() + txt.slice(1);
    });
  }

}
