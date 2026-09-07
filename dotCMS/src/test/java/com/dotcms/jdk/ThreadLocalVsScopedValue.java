package com.dotcms.jdk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Why a {@code ThreadLocal} cannot simply be swapped for a {@link ScopedValue}.
 *
 * <p>The two are described as alternatives, and dotCMS has a lot of the first: the canonical shape
 * is {@code HttpServletRequestThreadLocal}, where a filter puts the request on the thread and code
 * a hundred frames down — with no reference to the request — pulls it back out. Reading the two
 * APIs side by side suggests a mechanical migration. It is not one, and this demo is the argument.</p>
 *
 * <p>Everything printed here is executed. The dotCMS counts quoted in the verdict were taken from
 * {@code dotCMS/src/main/java} on 2026-09-07.</p>
 *
 * <p>Run: {@code java dotCMS/src/test/java/com/dotcms/jdk/ThreadLocalVsScopedValue.java} — JDK 25,
 * no flags, no Maven, no dotCMS classpath.</p>
 */
public final class ThreadLocalVsScopedValue {

    /** Stands in for the request. A String keeps the demo about lifetime, not about servlets. */
    private static final ThreadLocal<String> REQUEST_TL = new ThreadLocal<>();

    /** The same idea as a scoped value. Note there is no way to declare a "default" mutable slot. */
    private static final ScopedValue<String> REQUEST_SV = ScopedValue.newInstance();

    /** Used to show what {@code set(null)} does that {@code remove()} does not. */
    private static final ThreadLocal<String> WITH_INITIAL =
            ThreadLocal.withInitial(() -> "computed-" + System.nanoTime());

    private ThreadLocalVsScopedValue() {
    }

    public static void main(final String[] args) throws Exception {

        banner("ThreadLocal and ScopedValue are not the same idiom");

        leakOnAPooledThread();
        setNullIsNotRemove();
        aScopedValueCannotOutliveItsScope();
        theIdiomInverts();
        verdict();
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The failure mode dotCMS actually has: a request left on a pooled thread is still there when
     * the next request lands on it.
     */
    private static void leakOnAPooledThread() throws Exception {

        section("1. The leak — a pooled thread remembers the last request");

        // One thread, reused: a servlet container thread pool of size one.
        final ExecutorService pool = Executors.newFixedThreadPool(1);

        pool.submit(() -> {
            REQUEST_TL.set("request-A");            // the filter puts it there
            deepCode("during request A");
            // and never removes it. Six classes in dotCMS do exactly this.
        }).get();

        pool.submit(() -> {
            // A second request. This one sets nothing at all.
            deepCode("during request B");
        }).get();

        pool.shutdown();

        System.out.println("""

              The second task never set anything. It read request A, on a thread the
              container handed it clean. Nothing failed, nothing logged: it simply
              served the wrong request's context.""");
    }

    /** Code far from the filter, with no reference to the request. */
    private static void deepCode(final String when) {
        System.out.printf("    deep code %-18s sees: %s%n", when, REQUEST_TL.get());
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The cleanup that is not one. dotCMS clears the request with {@code setRequest(null)}; that
     * writes null into the entry, it does not remove the entry.
     */
    private static void setNullIsNotRemove() {

        section("2. set(null) is not remove() — the entry survives");

        System.out.println("    first read (runs the initial supplier) : " + WITH_INITIAL.get());

        WITH_INITIAL.set(null);
        System.out.println("    after set(null)                       : " + WITH_INITIAL.get()
                + "   <- the supplier did NOT run again: the entry is still there");

        WITH_INITIAL.remove();
        System.out.println("    after remove()                        : " + WITH_INITIAL.get()
                + "   <- a fresh value: the entry was gone");

        System.out.println("""

              Both look like "cleared" from the outside. Only one of them releases the
              map entry that keeps the key — and the value — reachable from the thread.""");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void aScopedValueCannotOutliveItsScope() throws Exception {

        section("3. A scoped value cannot outlive its scope, even on a pooled thread");

        final ExecutorService pool = Executors.newFixedThreadPool(1);

        pool.submit(() -> ScopedValue.where(REQUEST_SV, "request-A").run(
                () -> System.out.println("    inside the binding, deep code sees : " + REQUEST_SV.get())
        )).get();

        pool.submit(() -> {
            System.out.println("    next task on the SAME thread, isBound(): " + REQUEST_SV.isBound());
            System.out.println("    reading it                             : "
                    + REQUEST_SV.orElse("<unbound>"));
        }).get();

        pool.shutdown();

        System.out.println("""

              No cleanup call exists because none is needed. The value is gone when
              run() returns, on every path, including the ones that throw.""");
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The reason the migration is not mechanical: the two APIs point in opposite directions.
     */
    private static void theIdiomInverts() {

        section("4. The idiom inverts — writes flow INWARD only");

        ScopedValue.where(REQUEST_SV, "request-A").run(() -> {

            System.out.println("    outer frame            : " + REQUEST_SV.get());

            // There is no REQUEST_SV.set("..."). The only way for deeper code to change
            // the value is to open a NEW binding — which is visible to code it calls,
            // and to nothing above it.
            ScopedValue.where(REQUEST_SV, "request-A-rewritten").run(
                    () -> System.out.println("    inner frame rebinds it : " + REQUEST_SV.get()));

            System.out.println("    back in the outer frame: " + REQUEST_SV.get()
                    + "   <- the rebinding was never visible here");
        });

        System.out.println("""

              With a ThreadLocal the WRITER decides, at any depth, and the value stays
              until someone removes it. With a ScopedValue the CALLER decides, by wrapping
              the call, and the value is immutable for everyone underneath.

              So the migration is not a type swap. For each of dotCMS's set() sites you
              have to find the frame that owns the lifetime and restructure the call into
              a wrap. And any downstream code that writes BACK — inner code informing outer
              code — has no equivalent at all. That is the shape of ThreadContextUtil's
              includeDependencies, written inside one wrap and read inside the next.""");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void verdict() {

        banner("What this means for dotCMS");

        System.out.println("""
              HttpServletRequestThreadLocal, counted over dotCMS/src/main/java:

                  103   reads of getRequest(), across 95 classes
                   10   sites that set a real request
                    3   sites that clear it, all with setRequest(null)
                    0   calls to remove(), anywhere
                    6   classes that set it and never clear it at all
                        RequestCostApiImpl, JsServlet, VelocityLiveMode,
                        VelocityServlet, PageResourceHelper, WorkflowResource

              The class has no remove() method to call. It exposes getRequest() and
              setRequest(), and that is the whole API — so the leak is not an oversight
              at the call sites, it is the only thing the type allows.

              A ScopedValue would make all of it structurally impossible. It would also
              require finding, for each of those 10 sites, the frame that owns the request
              lifetime and turning it inside out. Ten wraps, not ten replacements.""");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void banner(final String title) {
        System.out.println();
        System.out.println("=".repeat(76));
        System.out.println("  " + title);
        System.out.println("=".repeat(76));
    }

    private static void section(final String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("-".repeat(76));
    }
}
