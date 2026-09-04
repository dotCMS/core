package com.dotcms.rest.param;

import com.dotmarketing.util.DateUtil;

import java.text.ParseException;
import java.util.Date;

/**
 * Encapsulates the logic to parse a ISO String date into a {@link Date}
 *
 * <p>Used as a JAX-RS parameter binder, so the string it receives is untrusted request input — see
 * {@code BundleResource.deleteBundlesOlderThan}, which takes it as a {@code @PathParam}.</p>
 *
 * <p><b>Why the validation sits before {@code super(...)}.</b> {@link DateUtil#parseISO(String)}
 * signals "unusable input" by returning {@code null} rather than by throwing, and
 * {@code UtilMethods.isSet} — the check it uses — rejects {@code null}, blank text <i>and the
 * literal four characters {@code "null"}</i>. A client that interpolates an unset variable into the
 * URL therefore sends a value that parses to {@code null}. Until Java 25 nothing was allowed to
 * precede an explicit constructor invocation, so the only available spelling was
 * {@code super(DateUtil.parseISO(stringDate).getTime())} — which dereferences that {@code null}
 * inside the argument list and fails with a {@link NullPointerException} carrying no indication of
 * which parameter was at fault. Flexible constructor bodies (JEP 513) allow a prologue to run
 * first, so the check can finally live in the type that owns the invariant.</p>
 *
 * @author jsanca
 */
public class ISODateParam extends Date {

    private static final long serialVersionUID = 1L;

    /**
     * Parses an ISO-8601 date.
     *
     * @param stringDate an ISO-8601 date, either {@code yyyy-MM-dd} or a full offset date-time
     * @throws ParseException if the value is missing, blank, the literal text {@code "null"}, or
     *                        not a parseable ISO-8601 date
     */
    public ISODateParam(final String stringDate) throws ParseException {

        final Date parsedDate = DateUtil.parseISO(stringDate);
        if (null == parsedDate) {
            throw new ParseException("Expected an ISO-8601 date, got: " + stringDate, 0);
        }

        super(parsedDate.getTime());
    }
}
