interface GenericProps {
  contentType?: string;
}

export default function Generic({ contentType }: GenericProps) {
  return (
    <div className="flex h-12 w-full items-center justify-center overflow-hidden bg-gray-100 text-sm text-gray-600">
      No component mapped for <strong className="ml-1">{contentType}</strong>
    </div>
  );
}
