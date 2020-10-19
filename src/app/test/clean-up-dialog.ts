export default function (fixture) {
    try {
        (fixture.nativeElement as HTMLElement).remove();
    } catch {}
}
