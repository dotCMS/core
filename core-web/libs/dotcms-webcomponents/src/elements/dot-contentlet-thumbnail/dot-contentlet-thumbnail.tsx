import { Component, h, Host, Prop, State } from '@stencil/core';
import { DotContentletItem } from '../../models/dot-contentlet-item.model';

@Component({
    tag: 'dot-contentlet-thumbnail',
    styleUrl: 'dot-contentlet-thumbnail.scss'
})
export class DotContentletThumbnail {
    @Prop({ reflect: true })
    height = '';

    @Prop({ reflect: true })
    width = '';

    @Prop({ reflect: true })
    alt = '';

    @Prop({ reflect: true })
    iconSize = '';

    @Prop()
    contentlet: DotContentletItem;

    @State() renderImage: boolean;

    componentWillLoad() {
        const { hasTitleImage, mimeType } = this.contentlet;
        // Some endpoints return this property as a boolean
        if (typeof hasTitleImage === 'boolean' && hasTitleImage) {
            this.renderImage = hasTitleImage;
        } else {
            this.renderImage = hasTitleImage === 'true' || mimeType === 'application/pdf';
        }
    }

    render() {
        const image = this.contentlet ? `url(${this.getImageURL()})` : '';

        return (
            <Host>
                {this.renderImage ? (
                    <div class="thumbnail" style={{ 'background-image': image }}>
                        <img
                            src={this.getImageURL()}
                            alt={this.alt}
                            aria-label={this.alt}
                            onError={() => {
                                this.switchToIcon();
                            }}
                        />
                    </div>
                ) : (
                    <dot-contentlet-icon
                        icon={
                            this.contentlet?.baseType !== 'FILEASSET'
                                ? this.contentlet?.contentTypeIcon
                                : this.contentlet?.__icon__
                        }
                        size={this.iconSize}
                        aria-label={this.alt}
                    />
                )}
            </Host>
        );
    }

    private getImageURL(): string {
        return this.contentlet.mimeType === 'application/pdf'
            ? `/contentAsset/image/${this.contentlet.inode}/${this.contentlet.titleImage}/pdf_page/1/resize_w/250/quality_q/45`
            : `/dA/${this.contentlet.inode}/500w/20q?r=${this.contentlet.modDateMilis}`;
    }

    private switchToIcon(): any {
        this.renderImage = false;
    }
}
