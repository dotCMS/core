import { ErrorLayout } from "@/components/ErrorLayout";
import { ERROR_COPY } from "@/components/error";

export default function NotFound() {
    return (
        <ErrorLayout
            status={404}
            heading={ERROR_COPY[404].heading}
            body={ERROR_COPY[404].body}
        />
    );
}
