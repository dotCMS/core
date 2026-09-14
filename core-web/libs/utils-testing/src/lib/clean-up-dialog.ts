export function cleanUpDialog(fixture: { nativeElement: unknown }) {
    try {
        (fixture.nativeElement as HTMLElement).remove();
    } catch {
        // do nothing
    }
}
