export function cleanUpDialog(fixture) {
    try {
        (fixture.nativeElement as HTMLElement).remove();
    } catch {
        // do nothing
    }
}
