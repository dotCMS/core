import { Component, Host, h, Prop, State } from '@stencil/core';
import { DotContentletItem } from '../../models/dot-contentlet-item.model';

@Component({
    tag: 'dot-video-thumbnail',
    styleUrl: 'dot-video-thumbnail.scss'
})
export class DotVideoThumbnail {
    @State() src: string = null;

    /**
     *
     *
     * @type {DotContentletItem}
     * @memberof DotVideoThumbnail
     */
    @Prop() contentlet: DotContentletItem;

    /**
     *
     *
     * @type {string}
     * @memberof variable
     */
    @Prop() variable: string;

    /**
     * If the video is playable or not.
     *
     * @type {boolean}
     * @memberof DotVideoThumbnail
     */
    @Prop() playable: boolean = false;

    /**
     *
     *
     * @type {boolean}
     * @memberof DotVideoThumbnail
     */
    @Prop() cover: boolean = true;

    render() {
        const cssClass = this.cover ? 'cover' : '';
        const bgImage = this.cover ? { 'background-image': `url(${this.src})` } : {};
        const { title } = this.contentlet;

        return (
            <Host>
                {this.playable ? (
                    <video
                        src={`/dA/${this.contentlet.inode}/${
                            this.variable || this.contentlet.titleImage
                        }`}
                        controls></video>
                ) : (
                    this.src && (
                        <div class={`thumbnail ${cssClass}`} style={bgImage}>
                            <img src={this.src} alt={title} />
                        </div>
                    )
                )}
            </Host>
        );
    }

    componentDidLoad() {
        this.setVideoThumbnail();
    }

    /**
     *
     * @private
     * @memberof DotVideoThumbnail
     */
    private setVideoThumbnail() {
        const video = document.createElement('video') as HTMLVideoElement;
        video.preload = 'metadata';
        video.addEventListener('canplaythrough', () => this.createVideoImage(video));
        /**
         * The `#t=0.1` is to only download the first frame of the video.
         * See more about it here: https://stackoverflow.com/questions/7323053/dynamically-using-the-first-frame-as-poster-in-html5-video
         */
        video.src = `/dA/${this.contentlet.inode}#t=0.1`;
    }

    /**
     * Create a thumbnail usint the first frame of a video.
     *
     * @private
     * @param {HTMLVideoElement} video
     * @memberof DotVideoThumbnail
     */
    private createVideoImage(video: HTMLVideoElement) {
        const { videoWidth, videoHeight } = video;

        // The image thumbnail component is limited to a 500 with
        const width = videoWidth < 500 ? videoWidth : 500;
        const height = this.determineNewHeight(videoHeight, videoWidth, width);

        const canvas = document.createElement('canvas') as HTMLCanvasElement;
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0, width, height);

        this.src = canvas.toDataURL('image/jpeg');
    }

    /**
     * Calculates the proper height of an image with a custom width, preserving the original aspect ratio.
     *
     * @param originalHeight
     * @param originalWidth
     * @param newWidth
     */
    private determineNewHeight(originalHeight, originalWidth, newWidth) {
        return (originalHeight / originalWidth) * newWidth;
    }
}
