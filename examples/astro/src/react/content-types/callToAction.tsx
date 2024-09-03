import type { DotCMSContentlet } from "@dotcms/types";
import type { FC } from "react";

export type CallToActionProps = DotCMSContentlet;

export const CallToAction: FC<CallToActionProps> = ({
  title,
  subHeading,
  buttonText1,
  buttonUrl1,
  buttonText2,
  buttonUrl2,
}) => {
  return (
    <div className="flex flex-col justify-center w-full h-full p-8 bg-gradient-to-r from-blue-900 to-violet-800 rounded-xl">
      <h2 className="block mb-2 text-5xl antialiased font-semibold leading-snug tracking-normal text-blue-gray-900">
        {title}
      </h2>
      <div
        className="block mb-8 text-2xl antialiased font-normal leading-relaxed line-clamp-3"
        dangerouslySetInnerHTML={{ __html: subHeading }}
      />

      <div className="flex w-full gap-5">
        {buttonText1 && buttonUrl1 && (
          <a
            className="px-6 py-4 text-xl font-semibold text-white bg-blue-500 rounded-lg"
            href={buttonUrl1}
          >
            {buttonText1}
          </a>
        )}
        {buttonText2 && buttonUrl2 && (
          <a
            className="px-6 py-4 text-xl font-semibold text-white bg-yellow-500 rounded-lg"
            href={buttonUrl2}
          >
            {buttonText2}
          </a>
        )}
      </div>
    </div>
  );
};
