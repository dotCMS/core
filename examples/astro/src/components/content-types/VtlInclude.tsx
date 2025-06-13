import type { DotCMSBasicContentlet } from "@dotcms/types";
import { useIsEditMode } from "@/hooks";
import { DestinationListing, type Destination } from "../ui";

interface VtlIncludeProps extends DotCMSBasicContentlet {
  componentType: string;
  widgetCodeJSON: {
    destinations: Destination[];
  };
}
export default function VtlInclude({
  componentType,
  widgetCodeJSON,
}: VtlIncludeProps) {
  const isEditMode = useIsEditMode();

  if (componentType === "destinationListing") {
    return <DestinationListing destinations={widgetCodeJSON.destinations} />;
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
