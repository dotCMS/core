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

    // JSP elements need the image to be rendered as a background image because of the way they are styled
    // New elements should use the default image tag for accessibility
    @Prop({ reflect: true })
    backgroundImage = false;

    @Prop()
    showVideoThumbnail = true;

    @Prop()
    playableVideo = false;

    @Prop()
    contentlet: DotContentletItem;

    @Prop({ reflect: true })
    fieldVariable = '';

    @State() renderImage: boolean;
    @State() isSVG: boolean;

    componentWillLoad() {
        const { hasTitleImage, mimeType } = this.contentlet;
        this.isSVG = mimeType === 'image/svg+xml';
        // Some endpoints return this property as a boolean
        if (typeof hasTitleImage === 'boolean' && hasTitleImage) {
            this.renderImage = hasTitleImage;
        } else {
            this.renderImage =
                hasTitleImage === 'true' ||
                mimeType === 'application/pdf' ||
                this.shouldShowVideoThumbnail();
        }
    }

    render() {
        const backgroundImageURL =
            this.contentlet && this.backgroundImage ? `url(${this.getImageURL()})` : '';
        const imgClass = this.backgroundImage ? 'background-image' : '';
        const svgClass = this.isSVG ? ' svg-thumbnail' : '';

        return (
            <Host>
                {this.shouldShowVideoThumbnail() ? (
                    <dot-video-thumbnail
                        contentlet={this.contentlet}
                        variable={this.fieldVariable}
                        cover={this.backgroundImage}
                        playable={this.playableVideo}
                    />
                ) : this.renderImage ? (
                    <div
                        class={`thumbnail ${imgClass}${svgClass}`}
                        style={{
                            'background-image': backgroundImageURL
                        }}>
                        <img
                            src={this.getImageURL()}
                            alt={this.alt}
                            aria-label={this.alt}
                            onError={() => this.switchToIcon()}
                        />
                    </div>
                ) : (
                    <dot-contentlet-icon
                        icon={this.getIcon()}
                        size={this.iconSize}
                        aria-label={this.alt}
                    />
                )}
            </Host>
        );
    }

    private getImageURL(): string {
        if (this.contentlet.mimeType === 'application/pdf')
            return `/contentAsset/image/${this.contentlet.inode}/${
                this.fieldVariable || this.contentlet.titleImage
            }/pdf_page/1/resize_w/250/quality_q/45`;

        if (this.isSVG) return `/contentAsset/image/${this.contentlet.inode}/asset`;

        return `/dA/${this.contentlet.inode}/${this.fieldVariablePath()}500w/50q?r=${
            this.contentlet.modDateMilis || this.contentlet.modDate
        }`;
    }

    private fieldVariablePath(): string {
        if (!this.fieldVariable) {
            return '';
        }

        return `${this.fieldVariable || this.contentlet.titleImage}/`;
    }

    private switchToIcon(): any {
        this.renderImage = false;
    }

    private getIcon() {
        return this.contentlet?.baseType !== 'FILEASSET'
            ? this.contentlet?.contentTypeIcon
            : this.contentlet?.__icon__;
    }

    private shouldShowVideoThumbnail() {
        return this.contentlet?.mimeType?.includes('video') && this.showVideoThumbnail;
    }
}
