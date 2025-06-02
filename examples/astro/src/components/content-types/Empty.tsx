import type { DotCMSBasicContentlet } from "@dotcms/types";

export function CustomNoComponent({ contentType }: DotCMSBasicContentlet) {
  return (
    <div className="relative w-full bg-gray-200 h-12 flex justify-center items-center overflow-hidden">
      Dont have a component for this&nbsp;<strong>{contentType}</strong>.
    </div>
  );
}
