import { DotCMSEditableText } from "@dotcms/react";
import type { DotCMSBasicContentlet } from "@dotcms/types";

interface BannerProps extends DotCMSBasicContentlet {
  caption: string;
  inode: string;
  image: string;
  link: string;
  buttonText: string;
}

function Banner(contentlet: BannerProps) {
  const { title, caption, inode, image, link, buttonText } = contentlet;

  return (
    <div className="relative bg-gray-200 h-96 mb-4">
      {image && (
        <img src={`/dA/${inode}`} className="w-full h-full object-cover" alt={title} />
      )}
      <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-center text-white">
        <h2 className="mb-2 text-6xl font-bold text-shadow">
          <DotCMSEditableText contentlet={contentlet} fieldName="title" />
        </h2>
        <p className="mb-4 text-xl text-shadow">{caption}</p>
        {link && (
          <a
            className="p-4 text-xl transition duration-300 bg-violet-800 rounded-sm hover:bg-blue-700"
            href={link}
          >
            {buttonText || "See more"}
          </a>
        )}
      </div>
    </div>
  );
}

export default Banner;
