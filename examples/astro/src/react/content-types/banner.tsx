import type { DotCMSContentlet } from "@dotcms/types";
import useImageSrc from "@react/hooks/useImageSrc";
import type { FC } from "react";

export type BannerProps = DotCMSContentlet;

export const Banner: FC<BannerProps> = ({
  title,
  image,
  caption,
  buttonText,
  link,
}) => {
  const src = useImageSrc({ src: image?.idPath ?? image });

  return (
    <div className="relative w-full p-4 bg-gray-200 h-96">
      {image && (
        <img
          src={src}
          className="object-cover absolute w-full h-full top-0 left-0"
          alt={title}
        />
      )}
      <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-center text-white">
        <h2 className="mb-2 text-6xl font-bold text-shadow">{title}</h2>
        <p className="mb-4 text-xl text-shadow">{caption}</p>
        <a
          className="p-4 text-xl transition duration-300 bg-blue-500 rounded hover:bg-blue-600"
          href={link || "#"}
        >
          {buttonText}
        </a>
      </div>
    </div>
  );
};
