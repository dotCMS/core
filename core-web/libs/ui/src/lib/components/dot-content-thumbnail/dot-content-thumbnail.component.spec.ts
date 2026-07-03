import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentThumbnailComponent } from './dot-content-thumbnail.component';
import { DotContentThumbnail } from './models/dot-content-thumbnail.model';

const IMAGE_THUMBNAIL: DotContentThumbnail = {
    type: 'image',
    src: '/dA/inode-123/asset/500w/50q?r=99',
    icon: 'image',
    alt: 'photo.jpg'
};

const SVG_THUMBNAIL: DotContentThumbnail = {
    type: 'svg',
    src: '/contentAsset/image/inode-123/asset',
    icon: 'image',
    alt: 'logo.svg'
};

const PDF_THUMBNAIL: DotContentThumbnail = {
    type: 'pdf',
    src: '/contentAsset/image/inode-123/asset/pdf_page/1/resize_w/250/quality_q/45',
    icon: 'insert_drive_file',
    alt: 'doc.pdf'
};

const PLAYABLE_VIDEO_THUMBNAIL: DotContentThumbnail = {
    type: 'video',
    src: '/dA/inode-123/asset',
    icon: 'videocam',
    alt: 'clip.mp4',
    playable: true
};

const FRAME_VIDEO_THUMBNAIL: DotContentThumbnail = {
    type: 'video',
    src: '/dA/inode-123#t=0.1',
    icon: 'videocam',
    alt: 'clip.mp4',
    playable: false
};

const ICON_THUMBNAIL: DotContentThumbnail = {
    type: 'icon',
    src: '',
    icon: 'audiotrack',
    alt: 'song.mp3'
};

describe('DotContentThumbnailComponent', () => {
    let spectator: Spectator<DotContentThumbnailComponent>;

    const createComponent = createComponentFactory({
        component: DotContentThumbnailComponent
    });

    const create = (thumbnail: DotContentThumbnail, props: Record<string, unknown> = {}) => {
        spectator = createComponent({ props: { thumbnail, ...props } });
    };

    describe('contentlet input', () => {
        const contentlet = {
            inode: 'inode-123',
            title: 'A file',
            modDate: '99',
            baseType: 'DOTASSET',
            contentType: 'dotAsset',
            hasTitleImage: true
        } as unknown as DotCMSContentlet;

        it('resolves the thumbnail model from a contentlet', () => {
            spectator = createComponent({ props: { contentlet } });

            const img = spectator.query<HTMLImageElement>(byTestId('dot-content-thumbnail-image'));

            expect(img.getAttribute('src')).toBe('/dA/inode-123/500w/50q?r=99');
        });

        it('applies the mapping options', () => {
            spectator = createComponent({
                props: {
                    contentlet: { ...contentlet, mimeType: 'video/mp4' } as DotCMSContentlet,
                    options: { playableVideo: true, fieldVariable: 'asset' }
                }
            });

            const video = spectator.query<HTMLVideoElement>(
                byTestId('dot-content-thumbnail-video')
            );

            expect(video.getAttribute('src')).toBe('/dA/inode-123/asset');
        });

        it('prefers an explicit thumbnail model over the contentlet', () => {
            spectator = createComponent({ props: { contentlet, thumbnail: ICON_THUMBNAIL } });

            expect(spectator.query(byTestId('dot-content-thumbnail-icon'))).toBeTruthy();
        });

        it('renders the default icon when neither input is provided', () => {
            spectator = createComponent();

            const icon = spectator.query(byTestId('dot-content-thumbnail-icon'));

            expect(icon.textContent.trim()).toBe('insert_drive_file');
        });
    });

    describe('rendering by type', () => {
        it('renders an image with object-cover for type image', () => {
            create(IMAGE_THUMBNAIL);

            const img = spectator.query<HTMLImageElement>(byTestId('dot-content-thumbnail-image'));

            expect(img).toBeTruthy();
            expect(img.getAttribute('src')).toBe(IMAGE_THUMBNAIL.src);
            expect(img.getAttribute('alt')).toBe(IMAGE_THUMBNAIL.alt);
            expect(img.classList).not.toContain('thumbnail-image--contain');
        });

        it('renders an image with object-contain for type svg', () => {
            create(SVG_THUMBNAIL);

            const img = spectator.query<HTMLImageElement>(byTestId('dot-content-thumbnail-image'));

            expect(img.getAttribute('src')).toBe(SVG_THUMBNAIL.src);
            expect(img.classList).toContain('thumbnail-image--contain');
        });

        it('renders an image with object-cover for type pdf', () => {
            create(PDF_THUMBNAIL);

            const img = spectator.query<HTMLImageElement>(byTestId('dot-content-thumbnail-image'));

            expect(img.getAttribute('src')).toBe(PDF_THUMBNAIL.src);
            expect(img.classList).not.toContain('thumbnail-image--contain');
        });

        it('renders a video with controls when playable', () => {
            create(PLAYABLE_VIDEO_THUMBNAIL);

            const video = spectator.query<HTMLVideoElement>(
                byTestId('dot-content-thumbnail-video')
            );

            expect(video).toBeTruthy();
            expect(video.getAttribute('src')).toBe(PLAYABLE_VIDEO_THUMBNAIL.src);
            expect(video.hasAttribute('controls')).toBe(true);
        });

        it('renders a muted first-frame video without controls when not playable', () => {
            create(FRAME_VIDEO_THUMBNAIL);

            const video = spectator.query<HTMLVideoElement>(
                byTestId('dot-content-thumbnail-video-frame')
            );

            expect(video).toBeTruthy();
            expect(video.getAttribute('src')).toBe(FRAME_VIDEO_THUMBNAIL.src);
            expect(video.hasAttribute('controls')).toBe(false);
            expect(video.getAttribute('preload')).toBe('metadata');
        });

        it('renders the material icon glyph for type icon', () => {
            create(ICON_THUMBNAIL);

            const icon = spectator.query(byTestId('dot-content-thumbnail-icon'));

            expect(icon).toBeTruthy();
            expect(icon.textContent.trim()).toBe('audiotrack');
            expect(icon.classList).toContain('material-symbols-outlined');
        });

        it('falls back to insert_drive_file when the icon model has no glyph', () => {
            create({ ...ICON_THUMBNAIL, icon: '' });

            const icon = spectator.query(byTestId('dot-content-thumbnail-icon'));

            expect(icon.textContent.trim()).toBe('insert_drive_file');
        });
    });

    describe('icon sizing', () => {
        it('auto-scales by default (no inline font-size)', () => {
            create(ICON_THUMBNAIL);

            const icon = spectator.query<HTMLElement>(byTestId('dot-content-thumbnail-icon'));

            expect(icon.style.fontSize).toBe('');
        });

        it('applies the explicit iconSize override', () => {
            create(ICON_THUMBNAIL, { iconSize: '48px' });

            const icon = spectator.query<HTMLElement>(byTestId('dot-content-thumbnail-icon'));

            expect(icon.style.fontSize).toBe('48px');
        });
    });

    describe('state machine', () => {
        it('starts as loading for media types (pulse visible, media hidden)', () => {
            create(IMAGE_THUMBNAIL);

            const loading = spectator.query(byTestId('dot-content-thumbnail-loading'));

            expect(loading).toBeTruthy();
            expect(loading.classList).not.toContain('thumbnail-loading--hidden');
            expect(spectator.query('dot-content-thumbnail-image').classList).toContain(
                'thumbnail-media--hidden'
            );
        });

        it('starts as loaded for icon type (no pulse)', () => {
            create(ICON_THUMBNAIL);

            expect(spectator.query(byTestId('dot-content-thumbnail-loading'))).toBeFalsy();
        });

        it('fades out the pulse and reveals the media on load', () => {
            create(IMAGE_THUMBNAIL);

            spectator.dispatchFakeEvent(byTestId('dot-content-thumbnail-image'), 'load');
            spectator.detectChanges();

            expect(spectator.query(byTestId('dot-content-thumbnail-loading')).classList).toContain(
                'thumbnail-loading--hidden'
            );
            expect(spectator.query('dot-content-thumbnail-image').classList).not.toContain(
                'thumbnail-media--hidden'
            );
        });

        it('falls back to the icon renderer on media error', () => {
            create(IMAGE_THUMBNAIL);

            spectator.dispatchFakeEvent(byTestId('dot-content-thumbnail-image'), 'error');
            spectator.detectChanges();

            const icon = spectator.query(byTestId('dot-content-thumbnail-icon'));

            expect(spectator.query(byTestId('dot-content-thumbnail-image'))).toBeFalsy();
            expect(icon).toBeTruthy();
            expect(icon.textContent.trim()).toBe(IMAGE_THUMBNAIL.icon);
            expect(spectator.query(byTestId('dot-content-thumbnail-loading'))).toBeFalsy();
        });

        it('falls back to the icon renderer when the video errors', () => {
            create(PLAYABLE_VIDEO_THUMBNAIL);

            spectator.dispatchFakeEvent(byTestId('dot-content-thumbnail-video'), 'error');
            spectator.detectChanges();

            expect(spectator.query(byTestId('dot-content-thumbnail-icon'))).toBeTruthy();
        });

        it('never masks playable videos (no skeleton, video visible while loading)', () => {
            create(PLAYABLE_VIDEO_THUMBNAIL);

            expect(spectator.query(byTestId('dot-content-thumbnail-loading'))).toBeFalsy();
            expect(spectator.query('dot-content-thumbnail-video').classList).not.toContain(
                'thumbnail-media--hidden'
            );
        });

        it('keeps the skeleton for non-playable first-frame videos until loadeddata', () => {
            create(FRAME_VIDEO_THUMBNAIL);

            const loading = spectator.query(byTestId('dot-content-thumbnail-loading'));

            expect(loading).toBeTruthy();
            expect(loading.classList).not.toContain('thumbnail-loading--hidden');
            expect(spectator.query('dot-content-thumbnail-video').classList).toContain(
                'thumbnail-media--hidden'
            );

            spectator.dispatchFakeEvent(
                byTestId('dot-content-thumbnail-video-frame'),
                'loadeddata'
            );
            spectator.detectChanges();

            expect(spectator.query(byTestId('dot-content-thumbnail-loading')).classList).toContain(
                'thumbnail-loading--hidden'
            );
            expect(spectator.query('dot-content-thumbnail-video').classList).not.toContain(
                'thumbnail-media--hidden'
            );
        });

        it('emits stateChange transitions', () => {
            spectator = createComponent({
                props: { thumbnail: IMAGE_THUMBNAIL },
                detectChanges: false
            });

            const states: string[] = [];
            spectator.output('stateChange').subscribe((state) => states.push(state as string));
            spectator.detectChanges();

            spectator.dispatchFakeEvent(byTestId('dot-content-thumbnail-image'), 'load');
            spectator.detectChanges();

            expect(states).toEqual(['loading', 'loaded']);
        });

        it('resets to the model type and loading state when the thumbnail input changes', () => {
            create(IMAGE_THUMBNAIL);

            spectator.dispatchFakeEvent(byTestId('dot-content-thumbnail-image'), 'error');
            spectator.detectChanges();
            expect(spectator.query(byTestId('dot-content-thumbnail-icon'))).toBeTruthy();

            spectator.setInput('thumbnail', { ...IMAGE_THUMBNAIL, src: '/dA/other/500w' });

            expect(spectator.query(byTestId('dot-content-thumbnail-image'))).toBeTruthy();
            expect(spectator.query(byTestId('dot-content-thumbnail-loading'))).toBeTruthy();
        });
    });
});
