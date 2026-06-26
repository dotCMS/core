'use client';

import { DotCMSEditableText } from '@dotcms/react';
import Image from 'next/image';
import Link from 'next/link';

import type { ContentTypeProps, DotCMSImage } from '@/types/content';

type BannerProps = ContentTypeProps & {
    caption?: string;
    image?: DotCMSImage;
    link?: string;
    buttonText?: string;
};

function Banner(contentlet: BannerProps) {
    const { title, caption, image, link, buttonText, dotStyleProperties } = contentlet;

    // Extract style properties with defaults
    const titleSize = (dotStyleProperties?.['title-size'] as string) || 'text-6xl';
    const captionSize = (dotStyleProperties?.['caption-size'] as string) || 'text-xl';
    const titleStyle = (dotStyleProperties?.['title-style'] as Record<string, boolean>) || {};
    const textAlignment = (dotStyleProperties?.['text-alignment'] as string) || 'center';
    const overlayStyle = (dotStyleProperties?.['overlay-style'] as string) || 'none';
    const buttonColor = (dotStyleProperties?.['button-color'] as string) || 'blue';
    const buttonSize = (dotStyleProperties?.['button-size'] as string) || 'medium';
    const buttonStyle = (dotStyleProperties?.['button-style'] as Record<string, boolean>) || {};

    // Build title classes
    const titleClasses = [
        'mb-2',
        titleSize,
        titleStyle.bold ? 'font-bold' : 'font-normal',
        titleStyle.italic ? 'italic' : '',
        titleStyle.underline ? 'underline' : '',
        'text-white',
        'text-shadow'
    ]
        .filter(Boolean)
        .join(' ');

    // Build caption classes
    const captionClasses = ['mb-4', captionSize, 'text-white', 'text-shadow'].join(' ');

    // Get alignment classes
    const getAlignmentClasses = () => {
        switch (textAlignment) {
            case 'left':
                return 'items-start text-left';
            case 'right':
                return 'items-end text-right';
            case 'center':
            default:
                return 'items-center text-center';
        }
    };

    // Get overlay classes
    const getOverlayClasses = () => {
        switch (overlayStyle) {
            case 'dark':
                return 'bg-black/40';
            case 'light':
                return 'bg-white/20';
            case 'gradient':
                return 'bg-gradient-to-b from-black/50 via-transparent to-black/50';
            case 'none':
            default:
                return '';
        }
    };

    // Get button color classes
    const getButtonColorClasses = () => {
        const colorMap: Record<string, string> = {
            blue: 'bg-blue-500 hover:bg-blue-700',
            green: 'bg-green-500 hover:bg-green-700',
            red: 'bg-red-500 hover:bg-red-700',
            purple: 'bg-purple-500 hover:bg-purple-700',
            orange: 'bg-orange-500 hover:bg-orange-700',
            teal: 'bg-teal-500 hover:bg-teal-700'
        };
        return colorMap[buttonColor] || colorMap.blue;
    };

    // Get button size classes
    const getButtonSizeClasses = () => {
        switch (buttonSize) {
            case 'small':
                return 'px-3 py-2 text-base';
            case 'large':
                return 'px-6 py-4 text-2xl';
            case 'medium':
            default:
                return 'px-4 py-2 text-xl';
        }
    };

    // Get button style classes
    const getButtonStyleClasses = () => {
        const classes = [];
        if (buttonStyle.rounded) {
            classes.push('rounded-lg');
        } else if (buttonStyle['full-rounded']) {
            classes.push('rounded-full');
        } else {
            classes.push('rounded-sm');
        }
        if (buttonStyle.shadow) {
            classes.push('shadow-lg');
        }
        return classes.join(' ');
    };

    const buttonClasses = [
        'transition duration-300',
        'text-white',
        'font-bold',
        getButtonColorClasses(),
        getButtonSizeClasses(),
        getButtonStyleClasses()
    ].join(' ');

    return (
        <div className="relative w-full p-4 bg-gray-200 h-96">
            {image?.identifier && (
                <Image src={image?.identifier} fill={true} className="object-cover" alt={title} />
            )}
            {overlayStyle !== 'none' && (
                <div className={`absolute inset-0 ${getOverlayClasses()}`} />
            )}
            <div
                className={`absolute inset-0 flex flex-col justify-center p-4 ${getAlignmentClasses()} text-white`}>
                <h2 className={titleClasses}>
                    <DotCMSEditableText contentlet={contentlet} fieldName="title" />
                </h2>
                {caption && <p className={captionClasses}>{caption}</p>}
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
