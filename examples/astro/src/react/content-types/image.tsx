import type { DotCMSContentlet } from "@dotcms/types";
import useImageSrc from "@react/hooks/useImageSrc";
import type { FC } from "react";

export type ImageComponentProps = DotCMSContentlet;

export const ImageComponent: FC<ImageComponentProps> = ({
  fileAsset,
  title,
  description,
}) => {
  const src = useImageSrc({ src: fileAsset?.idPath ?? fileAsset });

  return (
    <div className="relative overflow-hidden bg-white rounded shadow-lg group">
      <div className="relative w-full bg-gray-200 h-96">
        {fileAsset && (
          <img
            src={src}
            className="object-cover w-full h-full absolute top-0 left-0"
            alt={title}
          />
        )}
      </div>
      <div className="absolute bottom-0 w-full px-6 py-8 text-white transition-transform duration-300 translate-y-full bg-orange-500 bg-opacity-80 w-100 group-hover:translate-y-0">
        <div className="mb-2 text-2xl font-bold">{title}</div>
        <p className="text-base">{description}</p>
      </div>
    </div>
  );
};
