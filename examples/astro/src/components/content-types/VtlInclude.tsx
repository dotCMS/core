import type { DotCMSBasicContentlet } from "@dotcms/types";
import { useIsEditMode } from "@/hooks";

interface VtlIncludeProps extends DotCMSBasicContentlet {
  componentType: string;
  widgetCodeJSON: {
    products: {
      inode: string;
      title: string;
      retailPrice: number;
      salePrice: number;
    }[];
  };
}
export default function VtlInclude({
  componentType,
  widgetCodeJSON,
}: VtlIncludeProps) {
  const isEditMode = useIsEditMode();

  if (componentType === "destinationListing") {
    // return <DestinationListing {...widgetCodeJSON} />;

    return <div>Destination List</div>;
  }

  if (isEditMode) {
    return (
      <div className="bg-blue-100 p-4">
        <h4>
          No Component Type: {componentType || "generic"} Found for VTL Include
        </h4>
      </div>
    );
  }

  return null;
}
