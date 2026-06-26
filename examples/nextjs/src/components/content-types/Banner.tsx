'use client';

import { cva } from 'class-variance-authority';
import { DotCMSEditableText } from '@dotcms/react';
import Image from 'next/image';
import Link from 'next/link';

import { cn, variant } from '@/lib/utils';
import type { ContentTypeProps, DotCMSImage } from '@/types/content';
import { BUTTON_COLORS, buttonColorVariants } from './styles';

const ALIGNMENTS = ['left', 'center', 'right'] as const;
const OVERLAYS = ['dark', 'light', 'gradient', 'none'] as const;
const BUTTON_SIZES = ['small', 'medium', 'large'] as const;

type BannerProps = ContentTypeProps & {
    caption?: string;
    image?: DotCMSImage;
    link?: string;
    buttonText?: string;
};

// Layout variants driven by `dotStyleProperties`. Unknown values fall back to
// the `defaultVariants` below.
// The wrapping JSX only renders this when the overlay is not 'none', so the
// `none`/default case just contributes no background.
const overlayVariants = cva('absolute inset-0', {
    variants: {
        style: {
            dark: 'bg-black/40',
            light: 'bg-white/20',
            gradient: 'bg-gradient-to-b from-black/50 via-transparent to-black/50',
            none: ''
        }
    },
    defaultVariants: { style: 'none' }
});

const contentVariants = cva(
    'absolute inset-0 flex flex-col justify-center p-4 text-white',
    {
        variants: {
            alignment: {
                left: 'items-start text-left',
                center: 'items-center text-center',
                right: 'items-end text-right'
            }
        },
        defaultVariants: { alignment: 'center' }
    }
);

const buttonSizeVariants = cva('', {
    variants: {
        size: {
            small: 'px-3 py-2 text-base',
            medium: 'px-4 py-2 text-xl',
            large: 'px-6 py-4 text-2xl'
        }
    },
    defaultVariants: { size: 'medium' }
});

function Banner(contentlet: BannerProps) {
    const { title, caption, image, link, buttonText, dotStyleProperties } = contentlet;

    const titleSize = dotStyleProperties?.['title-size'] || 'text-6xl';
    const captionSize = dotStyleProperties?.['caption-size'] || 'text-xl';
    const titleStyle = dotStyleProperties?.['title-style'] || {};
    const textAlignment = dotStyleProperties?.['text-alignment'];
    const overlayStyle = dotStyleProperties?.['overlay-style'] || 'none';
    const buttonColor = dotStyleProperties?.['button-color'];
    const buttonSize = dotStyleProperties?.['button-size'];
    const buttonStyle = dotStyleProperties?.['button-style'] || {};

    const titleClasses = cn(
        'mb-2 text-white text-shadow',
        titleSize,
        titleStyle.bold ? 'font-bold' : 'font-normal',
        titleStyle.italic && 'italic',
        titleStyle.underline && 'underline'
    );

    const buttonClasses = cn(
        'transition duration-300 text-white font-bold',
        buttonColorVariants({ color: variant(buttonColor, BUTTON_COLORS) }),
        buttonSizeVariants({ size: variant(buttonSize, BUTTON_SIZES) }),
        buttonStyle.rounded ? 'rounded-lg' : buttonStyle['full-rounded'] ? 'rounded-full' : 'rounded-sm',
        buttonStyle.shadow && 'shadow-lg'
    );

    return (
        <div className="relative w-full p-4 bg-gray-200 h-96">
            {image?.identifier && (
                <Image src={image?.identifier} fill={true} className="object-cover" alt={title} />
            )}
            {overlayStyle !== 'none' && (
                <div className={overlayVariants({ style: variant(overlayStyle, OVERLAYS) })} />
            )}
            <div className={contentVariants({ alignment: variant(textAlignment, ALIGNMENTS) })}>
                <h2 className={titleClasses}>
                    <DotCMSEditableText contentlet={contentlet} fieldName="title" />
                </h2>
                {caption && <p className={cn('mb-4 text-white text-shadow', captionSize)}>{caption}</p>}
                {link && (
                    <Link className={buttonClasses} href={link}>
                        {buttonText || 'See more'}
                    </Link>
                )}
            </div>
        </div>
    );
}

export default Banner;
