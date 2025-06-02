import type { DotCMSBasicContentlet } from "@dotcms/types";

import { useIsEditMode } from "src/hooks/isEditMode";

interface PageFormProps extends DotCMSBasicContentlet {
  formType: string;
}

export default function PageForm(contentlet: PageFormProps) {
  const { formType } = contentlet;
  const isEditMode = useIsEditMode();

  if (formType === "contact-us") {
    // return <ContactUs {...contentlet} />;
    return <div>Contact Us</div>;
  }

  if (isEditMode) {
    return (
      <div>
        <h4>There is no form component for this form type: {formType}</h4>
      </div>
    );
  }

  return null;
}
