import { DotCmsClient } from '@dotcms/client'
// export type DotCMSImageProps = Omit<ImageProps, 'src'> & {
//     path?: string;
//     identifier?: string;
//     name?: string;
// };

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type DotCMSImageProps = Record<string, any>;

export const DotCMSImage = ({ data, alt, src, ...props }: DotCMSImageProps) => {
    const client = DotCmsClient.instance;

    const srcUrl = data.identifier ? `${client.dotcmsUrl}${src}` : src
    // console.log('props ... => ', { data, alt, props });
    // console.log({srcUrl});

    return <img {...props} alt={alt} src={srcUrl} />;
};

// export const DotImage = ({ attrs: { textAlign, data } }: ContentNode<any>) => {
//     const { asset, title } = data
//     const [imgTitle] = title.split('.')

//     return (
//       <div className="w-full h-64 mb-4 relative" style={{ textAlign: textAlign }}>
//         <DotCMSImage
//           alt={`Cover Image for ${title}`}
//           className={cn('shadow-small', {
//             'hover:shadow-medium transition-shadow  duration-200': imgTitle,
//           })}
//           layout="fill"
//           path={asset}
//         />
//       </div>
//     )
// }
