package com.dotcms.util;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.lang.StringUtils;

/**
 * Simple Precondition checks, wrapping Google's version.
 * This implementation adds the ability to specify the exception that is to
 * be thrown, streamlining validation of public API methods by removing the need
 * to wrap the generic exceptions and rethrow as API specific versions.
 *
 * See https://code.google.com/p/guava-libraries/wiki/PreconditionsExplained
 *
 * @author Geoff M. Granum
 */
public final class DotPreconditions {

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 */
	public static void checkArgument(boolean expression) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(expression);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be
	 *            converted to a string using {@link String#valueOf(Object)}
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 */
	public static void checkArgument(boolean expression, @NotNull Object errorMessage) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(expression, errorMessage);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessageTemplate
	 *            a template for the exception message should the check fail.
	 *            The message is formed by replacing each {@code %s} placeholder
	 *            in the template with an argument. These are matched by
	 *            position - the first {@code %s} gets {@code
	 *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to
	 *            the formatted message in square braces. Unmatched placeholders
	 *            will be left as-is.
	 * @param errorMessageArgs
	 *            the arguments to be substituted into the message template.
	 *            Arguments are converted to strings using
	 *            {@link String#valueOf(Object)}.
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 * @throws NullPointerException
	 *             if the check fails and either {@code errorMessageTemplate} or
	 *             {@code errorMessageArgs} is null (don't let this happen)
	 */
	public static void checkArgument(boolean expression, @NotNull String errorMessageTemplate,
			@NotNull Object... errorMessageArgs) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(expression, errorMessageTemplate, errorMessageArgs);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, char p1) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, int p1) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, long p1) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1);
	}

	/**
	 * If <code>expression</code> is true then throw a RuntimeExcpetion according to the <code>exceptionCOde</code>
	 * parameter.
	 * @param expression if it is truw the RuntimeException is thrown, otherwise the method do nothing
	 * @param exceptionCLass Exception class to be thrown
	 * @param messageTemplate Message template for the Exception
	 * @param messageArgs message parameters
	 */
	public static void checkArgument(boolean expression, final Class<? extends RuntimeException> exceptionCLass,
			@NotNull String messageTemplate, @NotNull Object... messageArgs) {

		if (!expression) {
			throw newException(messageTemplate, exceptionCLass, messageArgs);
		}
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, char p1, char p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, char p1, int p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, char p1, long p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, char p1, @NotNull Object p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, int p1, char p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, int p1, int p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, int p1, long p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, int p1, @NotNull Object p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, long p1, char p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, long p1, int p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, long p1, long p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, long p1, @NotNull Object p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1, char p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1, int p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1, long p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2, @NotNull Object p3) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2);
	}

	/**
	 * Ensures the truth of an expression involving one or more parameters to
	 * the calling method.
	 *
	 * <p>
	 * See {@link #checkArgument(boolean, String, Object...)} for details.
	 */
	public static void checkArgument(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2, @NotNull Object p3, @NotNull Object p4) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkArgument(b,errorMessageTemplate,p1,p2,p3,p4);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @throws IllegalStateException
	 *             if {@code expression} is false
	 */
	public static void checkState(boolean expression) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(expression);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be
	 *            converted to a string using {@link String#valueOf(Object)}
	 * @throws IllegalStateException
	 *             if {@code expression} is false
	 */
	public static void checkState(boolean expression, @NotNull Object errorMessage) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(expression, errorMessage);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * @param expression
	 *            a boolean expression
	 * @param errorMessageTemplate
	 *            a template for the exception message should the check fail.
	 *            The message is formed by replacing each {@code %s} placeholder
	 *            in the template with an argument. These are matched by
	 *            position - the first {@code %s} gets {@code
	 *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to
	 *            the formatted message in square braces. Unmatched placeholders
	 *            will be left as-is.
	 * @param errorMessageArgs
	 *            the arguments to be substituted into the message template.
	 *            Arguments are converted to strings using
	 *            {@link String#valueOf(Object)}.
	 * @throws IllegalStateException
	 *             if {@code expression} is false
	 * @throws NullPointerException
	 *             if the check fails and either {@code errorMessageTemplate} or
	 *             {@code errorMessageArgs} is null (don't let this happen)
	 */
	public static void checkState(boolean expression, @NotNull String errorMessageTemplate,
			@NotNull Object... errorMessageArgs) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(expression, errorMessageTemplate, errorMessageArgs);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, char p1) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, int p1) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, long p1) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, char p1, char p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, char p1, int p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, char p1, long p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, char p1, @NotNull Object p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, int p1, char p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, int p1, int p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, int p1, long p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, int p1, @NotNull Object p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, long p1, char p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, long p1, int p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, long p1, long p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, long p1, @NotNull Object p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1, char p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1, int p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1, long p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2, @NotNull Object p3) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2, p3);
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling
	 * instance, but not involving any parameters to the calling method.
	 *
	 * <p>
	 * See {@link #checkState(boolean, String, Object...)} for details.
	 */
	public static void checkState(boolean b, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2, @NotNull Object p3, @NotNull Object p4) {
		com.dotcms.repackage.com.google.common.base.Preconditions.checkState(b, errorMessageTemplate, p1, p2, p3, p4);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * @param reference
	 *            an object reference
	 * @return the non-null reference that was validated
	 * @throws NullPointerException
	 *             if {@code reference} is null
	 */
	public static <T> T checkNotNull(T reference) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(reference);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * @param reference
	 *            an object reference
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be
	 *            converted to a string using {@link String#valueOf(Object)}
	 * @return the non-null reference that was validated
	 * @throws NullPointerException
	 *             if {@code reference} is null
	 */
	public static <T> T checkNotNull(T reference, @NotNull Object errorMessage) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(reference, errorMessage);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * @param reference
	 *            an object reference
	 * @param errorMessageTemplate
	 *            a template for the exception message should the check fail.
	 *            The message is formed by replacing each {@code %s} placeholder
	 *            in the template with an argument. These are matched by
	 *            position - the first {@code %s} gets {@code
	 *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to
	 *            the formatted message in square braces. Unmatched placeholders
	 *            will be left as-is.
	 * @param errorMessageArgs
	 *            the arguments to be substituted into the message template.
	 *            Arguments are converted to strings using
	 *            {@link String#valueOf(Object)}.
	 * @return the non-null reference that was validated
	 * @throws NullPointerException
	 *             if {@code reference} is null
	 */
	public static <T> T checkNotNull(T reference, @NotNull String errorMessageTemplate,
			@NotNull Object... errorMessageArgs) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(reference, errorMessageTemplate, errorMessageArgs);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, char p1) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, int p1) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, long p1) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, @NotNull Object p1) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, char p1, char p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, char p1, int p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, char p1, long p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, char p1, @NotNull Object p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, int p1, char p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, int p1, int p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, int p1, long p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, int p1, @NotNull Object p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, long p1, char p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, long p1, int p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, long p1, long p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, long p1, @NotNull Object p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, @NotNull Object p1, char p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, @NotNull Object p1, int p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, @NotNull Object p1, long p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2, @NotNull Object p3) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2, p3);
	}

	/**
	 * Ensures that an object reference passed as a parameter to the calling
	 * method is not null.
	 *
	 * <p>
	 * See {@link #checkNotNull(Object, String, Object...)} for details.
	 */
	public static <T> T checkNotNull(T obj, @NotNull String errorMessageTemplate, @NotNull Object p1,
			@NotNull Object p2, @NotNull Object p3, @NotNull Object p4) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkNotNull(obj, errorMessageTemplate, p1, p2, p3, p4);
	}

	/*
	 * All recent hotspots (as of 2009) *really* like to have the natural code
	 *
	 * if (guardExpression) { throw new BadException(messageExpression); }
	 *
	 * refactored so that messageExpression is moved to a separate
	 * String-returning method.
	 *
	 * if (guardExpression) { throw new BadException(badMsg(...)); }
	 *
	 * The alternative natural refactorings into void or Exception-returning
	 * methods are much slower. This is a big deal - we're talking factors of
	 * 2-8 in microbenchmarks, not just 10-20%. (This is a hotspot optimizer
	 * bug, which should be fixed, but that's a separate, big project).
	 *
	 * The coding pattern above is heavily used in java.util, e.g. in ArrayList.
	 * There is a RangeCheckMicroBenchmark in the JDK that was used to test
	 * this.
	 *
	 * But the methods in this class want to throw different exceptions,
	 * depending on the args, so it appears that this pattern is not directly
	 * applicable. But we can use the ridiculous, devious trick of throwing an
	 * exception in the middle of the construction of another exception. Hotspot
	 * is fine with that.
	 */

	/**
	 * Ensures that {@code index} specifies a valid <i>element</i> in an array,
	 * list or string of size {@code size}. An element index may range from
	 * zero, inclusive, to {@code size}, exclusive.
	 *
	 * @param index
	 *            a user-supplied index identifying an element of an array, list
	 *            or string
	 * @param size
	 *            the size of that array, list or string
	 * @return the value of {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or is not less than {@code size}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static int checkElementIndex(int index, int size) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkElementIndex(index, size);
	}

	/**
	 * Ensures that {@code index} specifies a valid <i>element</i> in an array,
	 * list or string of size {@code size}. An element index may range from
	 * zero, inclusive, to {@code size}, exclusive.
	 *
	 * @param index
	 *            a user-supplied index identifying an element of an array, list
	 *            or string
	 * @param size
	 *            the size of that array, list or string
	 * @param desc
	 *            the text to use to describe this index in an error message
	 * @return the value of {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or is not less than {@code size}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static int checkElementIndex(int index, int size, @NotNull String desc) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkElementIndex(index, size, desc);
	}

	/**
	 * Ensures that {@code index} specifies a valid <i>position</i> in an array,
	 * list or string of size {@code size}. A position index may range from zero
	 * to {@code size}, inclusive.
	 *
	 * @param index
	 *            a user-supplied index identifying a position in an array, list
	 *            or string
	 * @param size
	 *            the size of that array, list or string
	 * @return the value of {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or is greater than {@code size}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static int checkPositionIndex(int index, int size) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkPositionIndex(index, size);
	}

	/**
	 * Ensures that {@code index} specifies a valid <i>position</i> in an array,
	 * list or string of size {@code size}. A position index may range from zero
	 * to {@code size}, inclusive.
	 *
	 * @param index
	 *            a user-supplied index identifying a position in an array, list
	 *            or string
	 * @param size
	 *            the size of that array, list or string
	 * @param desc
	 *            the text to use to describe this index in an error message
	 * @return the value of {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or is greater than {@code size}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static int checkPositionIndex(int index, int size, @NotNull String desc) {
		return com.dotcms.repackage.com.google.common.base.Preconditions.checkPositionIndex(index, size, desc);
	}

	private static String badPositionIndex(int index, int size, String desc) {
		if (index < 0) {
			return format("%s (%s) must not be negative", desc, index);
		} else if (size < 0) {
			throw new IllegalArgumentException("negative size: " + size);
		} else { // index > size
			return format("%s (%s) must not be greater than size (%s)", desc, index, size);
		}
	}

	/**
	 * Ensures that {@code start} and {@code end} specify a valid
	 * <i>positions</i> in an array, list or string of size {@code size}, and
	 * are in order. A position index may range from zero to {@code size},
	 * inclusive.
	 *
	 * @param start
	 *            a user-supplied index identifying a starting position in an
	 *            array, list or string
	 * @param end
	 *            a user-supplied index identifying a ending position in an
	 *            array, list or string
	 * @param size
	 *            the size of that array, list or string
	 * @throws IndexOutOfBoundsException
	 *             if either index is negative or is greater than {@code size},
	 *             or if {@code end} is less than {@code start}
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative
	 */
	public static void checkPositionIndexes(int start, int end, int size) {
		// Carefully optimized for execution by hotspot (explanatory comment
		// above)
		if (start < 0 || end < start || end > size) {
			throw new IndexOutOfBoundsException(badPositionIndexes(start, end, size));
		}
	}

	private static String badPositionIndexes(int start, int end, int size) {
		if (start < 0 || start > size) {
			return badPositionIndex(start, size, "start index");
		}
		if (end < 0 || end > size) {
			return badPositionIndex(end, size, "end index");
		}
		// end < start
		return format("end index (%s) must not be less than start index (%s)", end, start);
	}

	/**
	 * Substitutes each {@code %s} in {@code template} with an argument. These
	 * are matched by position: the first {@code %s} gets {@code args[0]}, etc.
	 * If there are more arguments than placeholders, the unmatched arguments
	 * will be appended to the end of the formatted message in square braces.
	 *
	 * @param template
	 *            a non-null string containing 0 or more {@code %s}
	 *            placeholders.
	 * @param args
	 *            the arguments to be substituted into the message template.
	 *            Arguments are converted to strings using
	 *            {@link String#valueOf(Object)}. Arguments can be null.
	 */
	// Note that this is somewhat-improperly used from Verify.java as well.
	static String format(String template, @NotNull Object... args) {
		template = String.valueOf(template); // null -> "null"

		// start substituting the arguments into the '%s' placeholders
		StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
		int templateStart = 0;
		int i = 0;
		while (i < args.length) {
			int placeholderStart = template.indexOf("%s", templateStart);
			if (placeholderStart == -1) {
				break;
			}
			builder.append(template, templateStart, placeholderStart);
			builder.append(args[i++]);
			templateStart = placeholderStart + 2;
		}
		builder.append(template, templateStart, template.length());

		// if we run out of placeholders, append the extra args in square braces
		if (i < args.length) {
			builder.append(" [");
			builder.append(args[i++]);
			while (i < args.length) {
				builder.append(", ");
				builder.append(args[i++]);
			}
			builder.append(']');
		}

		return builder.toString();
	}

	public static <T> T checkNotNull(T argument, Class<? extends RuntimeException> exceptionType, String message,
			Object... messageArgs) {
		if (argument == null) {
			throw newException(message, exceptionType, messageArgs);
		}
		return argument;
	}

	public static String checkNotEmpty(String argument, Class<? extends RuntimeException> exceptionType, String message,
			Object... messageArgs) {
		if (StringUtils.isEmpty(argument)) {
			throw newException(message, exceptionType, messageArgs);
		}
		return argument;
	}

	public static <T> T[] checkNotEmpty(T[] argument, Class<? extends RuntimeException> exceptionType, String message,
			Object... messageArgs) {
		if (argument == null || argument.length == 0) {
			throw newException(message, exceptionType, messageArgs);
		}
		return argument;
	}

	public static RuntimeException newException(String message, Class<? extends RuntimeException> exceptionType,
			Object... messageArgs) {
		RuntimeException e;
		message = String.format(message, messageArgs);
		if (exceptionType == null) {
			e = new IllegalArgumentException(message);
		} else {
			try {
				Constructor<? extends RuntimeException> constructor = exceptionType.getConstructor(String.class);
				e = constructor.newInstance(message);
			} catch (NoSuchMethodException | InvocationTargetException | InstantiationException
					| IllegalAccessException e1) {
				throw new RuntimeException("Exception Types provided to Preconditions must have a constructor "
						+ "that takes a single string argument.", e1);
			}
		}
		return e;
	}


	/**
	 * Assert true, throws IllegalArgumentException if the expression is false.
	 *
	 * <pre >DotAssert.isTrue(!contentletAPI.canLock(contentlet, user), "Can not lock the content" );</pre>
	 * @param expression boolean expression to test
	 * @param errorMessage String message to pass to the IllegalArgumentException
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static void isTrue(final boolean expression, final String errorMessage) {

		if (!expression) {
			throw new IllegalArgumentException(errorMessage);
		}
	} // isTrue.

	/**
	 * Assert true, throws IllegalArgumentException if the expression is false.
	 *
	 * <pre >DotAssert.isTrue(contentletAPI.canLock(contentlet, user), ()->"Can not lock the content" );</pre>
	 * @param expression boolean expression to test
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static void isTrue(final boolean expression, final Supplier<String> errorMessage) {

		if (!expression) {
			throw new IllegalArgumentException(errorMessage.get());
		}
	} // isTrue.

	/**
	 * Assert true, throws Custom Throwable if the expression is false.
	 *
	 * <pre >DotAssert.isTrue(contentletAPI.canLock(contentlet, user), ()->"Can not lock the content", CannotLockContent.class );</pre>
	 * @param expression boolean expression to test
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static <T extends Throwable>  void isTrue(final boolean expression, final Supplier<String> errorMessage,
													 final Class<T> errorClazz) throws T {

		if (!expression) {
			throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
		}
	} // isTrue.

	/**
	 * Assert true, throws RuntimeException if the expression is false.
	 *
	 * <pre >DotAssert.isTrue(contentletAPI.canLock(contentlet, user), CannotLockContent.class, ()->"Can not lock the content");</pre>
	 * @param expression boolean expression to test
	 * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static void isTrue(final boolean expression,
							  final Class<? extends RuntimeException> errorClazz,
							  final Supplier<String> errorMessage) {

		if (!expression) {
			throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
		}
	} // isTrue.

	//////////
	// is NULL

	/**
	 * Assert isNull, throws IllegalArgumentException if the parameter is not null.
	 *
	 * <pre >DotAssert.isNull(user, "User must be null" );</pre>
	 * @param parameter Object parameter to test if it is null
	 * @param errorMessage String message to pass to the IllegalArgumentException
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static void isNull(final Object parameter, final String errorMessage) {

		isTrue(null == parameter, errorMessage);
	} // isNull.

	/**
	 * Assert isNull, throws IllegalArgumentException if the parameter is not null.
	 *
	 * <pre >DotAssert.isNull(user, ()->"User must be null" );</pre>
	 * @param parameter Object parameter to test if it is null
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static void isNull(final Object parameter, final Supplier<String> errorMessage) {

		isTrue(null == parameter, errorMessage);
	} // isNull.

	/**
	 * Assert isNull, throws Custom Throwable if the parameter is not null.
	 *
	 * <pre >DotAssert.isNull(user, ()->"User must be null", UserMustBeNullException.class );</pre>
	 * @param parameter Object parameter to test if it is null
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
	 */
	public static <T extends Throwable>  void isNull(final Object parameter, final Supplier<String> errorMessage,
													 final Class<T> errorClazz) throws T {

		isTrue(null == parameter, errorMessage, errorClazz);
	} // isNull.

	/**
	 * Assert isNull, throws RuntimeException if the parameter is not null.
	 *
	 * <pre >DotAssert.isNull(user, UserMustBeNullException.class, ()->"User must be null");</pre>
	 * @param parameter Object parameter to test if it is null
	 * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 */
	public static void isNull(final Object parameter,
							  final Class<? extends RuntimeException> errorClazz,
							  final Supplier<String> errorMessage) {

		isTrue(null == parameter, errorClazz, errorMessage);
	} // isNull.

	//////////
	// is NOT NULL

	/**
	 * Assert notNull, throws IllegalArgumentException if the parameter is null.
	 *
	 * <pre >DotAssert.notNull(user, "User must be not null" );</pre>
	 * @param parameter Object parameter to test if it is not null
	 * @param errorMessage String message to pass to the IllegalArgumentException
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static void notNull(final Object parameter, final String errorMessage) {

		isTrue(null != parameter, errorMessage);
	} // notNull.

	/**
	 * Assert notNull, throws IllegalArgumentException if the parameter is  null.
	 *
	 * <pre >DotAssert.notNull(user, ()->"User must be not null" );</pre>
	 * @param parameter Object parameter to test if it is not null
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static void notNull(final Object parameter, final Supplier<String> errorMessage) {

		isTrue(null != parameter, errorMessage);
	} // notNull.

	/**
	 * Assert notNull, throws Custom Throwable if the parameter is null.
	 *
	 * <pre >DotAssert.notNull(user, ()->"User must be not null", UserMustBeNullException.class );</pre>
	 * @param parameter Object parameter to test if it is not null
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static <T extends Throwable>  void notNull(final Object parameter, final Supplier<String> errorMessage,
													  final Class<T> errorClazz) throws T {

		isTrue(null != parameter, errorMessage, errorClazz);
	} // notNull.

	/**
	 * Assert notNull, throws RuntimeException if the parameter is null.
	 *
	 * <pre >DotAssert.notNull(user, UserMustBeNullException.class, ()->"User must be  notnull");</pre>
	 * @param parameter Object parameter to test if it is not null
	 * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @throws IllegalArgumentException if expression is <code>false</code>
	 */
	public static void notNull(final Object parameter,
							   final Class<? extends RuntimeException> errorClazz,
							   final Supplier<String> errorMessage) {

		isTrue(null != parameter, errorClazz, errorMessage);
	} // notNull.


	////////
	// NOT EMPTY

	/**
	 * Assert notEmpty, throws IllegalArgumentException if the array is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(array, "Array is empty" );</pre>
	 * @param array   Object array
	 * @param message String message to pass to the IllegalArgumentException
	 */
	public static void notEmpty(final Object[] array,
								final String message) {

		if (null == array || 0 == array.length) {
			throw new IllegalArgumentException(message);
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws IllegalArgumentException if the array is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(array, ()->"Array is empty" );</pre>
	 * @param array   Object array
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 */
	public static void notEmpty(final Object[] array,
								final Supplier<String> errorMessage) {

		if (null == array || 0 == array.length) {
			throw new IllegalArgumentException(errorMessage.get());
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws Custom Throwable if the array is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(array, ()->"Array is empty", ArrayEmptyException.class );</pre>
	 * @param array   Object array
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
	 */
	public static <T extends Throwable> void notEmpty(final Object[] array, final Supplier<String> errorMessage,
													  final Class<T> errorClazz) throws T {

		if (null == array || 0 == array.length) {
			throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws RuntimeException if the array is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(array, ArrayEmptyException.class, ()->"Array is empty");</pre>
	 * @param array   Object array
	 * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 */
	public static void notEmpty(final Object[] array,
								final Class<? extends RuntimeException> errorClazz,
								final Supplier<String> errorMessage) {

		if (null == array || 0 == array.length) {
			throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
		}
	} // notEmpty.

	///

	/**
	 * Assert notEmpty, throws IllegalArgumentException if the collection is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(list, "Collection is empty" );</pre>
	 * @param collection   Collection
	 * @param message String message to pass to the IllegalArgumentException
	 */
	public static void notEmpty(final Collection collection,
								final String message) {

		if (null == collection || collection.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws IllegalArgumentException if the collection is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(list, ()->"Collection is empty" );</pre>
	 * @param collection   Collection
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 */
	public static void notEmpty(final Collection collection,
								final Supplier<String> errorMessage) {

		if (null == collection || collection.isEmpty()) {
			throw new IllegalArgumentException(errorMessage.get());
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws Custom Throwable if the collection is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(list, ()->"Collection is empty"  );</pre>
	 * @param collection   Collection
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
	 */
	public static <T extends Throwable> void notEmpty(final Collection collection, final Supplier<String> errorMessage,
													  final Class<T> errorClazz) throws T {

		if (null == collection || collection.isEmpty()) {
			throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws RuntimeException if the collection is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(list, CollectionEmptyException.class, ()->"Collection is empty");</pre>
	 * @param collection   Collection
	 * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 */
	public static void notEmpty(final Collection collection,
								final Class<? extends RuntimeException> errorClazz,
								final Supplier<String> errorMessage) {

		if (null == collection || collection.isEmpty()) {
			throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
		}
	} // notEmpty.


	///

	/**
	 * Assert notEmpty, throws IllegalArgumentException if the collection is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(list, "Collection is empty" );</pre>
	 * @param collection   Map
	 * @param message String message to pass to the IllegalArgumentException
	 */
	public static void notEmpty(final Map collection,
								final String message) {

		if (null == collection || collection.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws IllegalArgumentException if the collection is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(list, ()->"Collection is empty" );</pre>
	 * @param collection   Map
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 */
	public static void notEmpty(final Map collection,
								final Supplier<String> errorMessage) {

		if (null == collection || collection.isEmpty()) {
			throw new IllegalArgumentException(errorMessage.get());
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws Custom Throwable if the collection is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(list, ()->"Collection is empty"  );</pre>
	 * @param collection   Map
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 * @param errorClazz   Class of Throwable, pre: the Throwable must have a constructor that receives a string (the error message)
	 */
	public static <T extends Throwable> void notEmpty(final Map collection, final Supplier<String> errorMessage,
													  final Class<T> errorClazz) throws T {

		if (null == collection || collection.isEmpty()) {
			throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
		}
	} // notEmpty.

	/**
	 * Assert notEmpty, throws RuntimeException if the collection is null or empty (zero items).
	 *
	 * <pre >DotAssert.notEmpty(list, CollectionEmptyException.class, ()->"Collection is empty");</pre>
	 * @param collection   Map
	 * @param errorClazz   Class of RuntimeException, pre: the RuntimeException must have a constructor that receives a string (the error message)
	 * @param errorMessage Supplier String message to pass to the IllegalArgumentException (lazy mode)
	 */
	public static void notEmpty(final Map collection,
								final Class<? extends RuntimeException> errorClazz,
								final Supplier<String> errorMessage) {

		if (null == collection || collection.isEmpty()) {
			throw ReflectionUtils.newInstance(errorClazz, errorMessage.get());
		}
	} // notEmpty.

}
