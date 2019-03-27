/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.util;

import java.util.Properties;

/**
 * <a href="PropsUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.107 $
 *
 */
public class PropsUtil {

	// Portal Release

	public static final String PORTAL_RELEASE = "portal.release";

	// Portal Context

	public static final String PORTAL_CTX = "portal.ctx";
	public static final String PORTAL_INSTANCES = "portal.instances";

	// System Properties

	public static final String SYSTEM_PROPERTIES_LOAD = "system.properties.load";
	public static final String SYSTEM_PROPERTIES_FINAL = "system.properties.final";

	// Log

	public static final String LOG_CONFIGURE_LOG4J = "log.configure.log4j";

	public static final String LOG_USER_PATHS = "log.user.paths";

	// Error

	public static final String ERROR_MESSAGE_LOG = "error.message.log";

	public static final String ERROR_MESSAGE_PRINT = "error.message.print";

	public static final String ERROR_MESSAGE_SHOW = "error.message.show";

	public static final String ERROR_STACK_TRACE_LOG = "error.stack.trace.log";

	public static final String ERROR_STACK_TRACE_PRINT = "error.stack.trace.print";

	public static final String ERROR_STACK_TRACE_SHOW = "error.stack.trace.show";

	// TCK

	public static final String TCK_URL = "tck.url";

	// Upgrade

	public static final String UPGRADE_PROCESSES = "upgrade.processes";

	// Company

	public static final String COMPANY_TYPES = "company.types";

	// Users

	public static final String USERS_DELETE = "users.delete";

	public static final String USERS_ID_ALWAYS_AUTOGENERATE = "users.id.always.autogenerate";

	public static final String USERS_ID_VALIDATOR = "users.id.validator";

	// Groups and Roles

	public static final String SYSTEM_GROUPS = "system.groups";

	public static final String SYSTEM_ROLES = "system.roles";

	public static final String OMNIADMIN_USERS = "omniadmin.users";

	public static final String UNIVERSAL_PERSONALIZATION = "universal.personalization";

	public static final String GROUP_PAGES_PERSONALIZATION = "group.pages.personalization";

	public static final String TERMS_OF_USE_REQUIRED = "terms.of.use.required";

	// Languages and Time Zones

	public static final String LOCALES = "locales";

	public static final String LOCALE_DEFAULT_REQUEST = "locale.default.request";

	public static final String STRUTS_CHAR_ENCODING = "struts.char.encoding";

	public static final String TIME_ZONES = "time.zones";

	// Skins

	public static final String SKINS_MULTIPLE = "skins.multiple";

	// Session

	public static final String SESSION_TIMEOUT = "session.timeout";
	public static final String SESSION_TIMEOUT_WARNING = "session.timeout.warning";

	public static final String SERVLET_SESSION_CREATE_EVENTS = "servlet.session.create.events";
	public static final String SERVLET_SESSION_DESTROY_EVENTS = "servlet.session.destroy.events";

	// JAAS

	public static final String PRINCIPAL_FINDER = "principal.finder";

	public static final String PORTAL_CONFIGURATION = "portal.configuration";

	// Authentication Pipeline

	public static final String AUTH_PIPELINE_PRE = "auth.pipeline.pre";
	public static final String AUTH_PIPELINE_POST = "auth.pipeline.post";

	public static final String AUTH_IMPL_ADS_INITIAL_CONTEXT_FACTORY = "auth.impl.ads.initial.context.factory";
	public static final String AUTH_IMPL_ADS_SECURITY_AUTHENTICATION = "auth.impl.ads.security.authentication";
	public static final String AUTH_IMPL_ADS_HOST = "auth.impl.ads.host";
	public static final String AUTH_IMPL_ADS_PORT = "auth.impl.ads.port";
	public static final String AUTH_IMPL_ADS_USERID = "auth.impl.ads.userid";
	public static final String AUTH_IMPL_ADS_PASSWORD = "auth.impl.ads.password";
	public static final String AUTH_IMPL_ADS_DOMAINLOOKUP = "auth.impl.ads.domainlookup";

	public static final String AUTH_FAILURE = "auth.failure";
	public static final String AUTH_MAX_FAILURES = "auth.max.failures";
	public static final String AUTH_MAX_FAILURES_LIMIT = "auth.max.failures.limit";

	public static final String AUTH_SIMULTANEOUS_LOGINS = "auth.simultaneous.logins";

	public static final String AUTH_FORWARD_BY_LAST_PATH = "auth.forward.by.last.path";

	public static final String AUTH_PUBLIC_PATH = "auth.public.path.";

	// Auto Login

	public static final String AUTO_LOGIN_HOOKS = "auto.login.hooks";

	// Passwords

	public static final String PASSWORDS_TOOLKIT = "passwords.toolkit";

	public static final String PASSWORDS_REGEXPTOOLKIT_PATTERN = "passwords.regexptoolkit.pattern";
	
	public static final String PASSWORDS_REGEXPTOOLKIT_PATTERN_ERROR = "passwords.regexptoolkit.pattern.error";
	
	public static final String PASSWORDS_ALLOW_DICTIONARY_WORD = "passwords.allow.dictionary.word";

	public static final String PASSWORDS_CHANGE_ON_FIRST_USE = "passwords.change.on.first.use";

	public static final String PASSWORDS_LIFESPAN = "passwords.lifespan";

	public static final String PASSWORDS_RECYCLE = "passwords.recycle";
	
	public static final String PASSWORDS_RECYCLE_ERROR = "passwords.recycle.error";

	// Captcha

	public static final String CAPTCHA_CHALLENGE = "captcha.challenge";

	// Startup Events

	public static final String GLOBAL_STARTUP_EVENTS = "global.startup.events";

	public static final String APPLICATION_STARTUP_EVENTS = "application.startup.events";

	// Portal Events

	public static final String SERVLET_SERVICE_EVENTS_PRE = "servlet.service.events.pre";
	public static final String SERVLET_SERVICE_EVENTS_PRE_ERROR_PAGE = "servlet.service.events.pre.error.page";
	public static final String SERVLET_SERVICE_EVENTS_POST = "servlet.service.events.post";

	public static final String LOGIN_EVENTS_PRE = "login.events.pre";
	public static final String LOGIN_EVENTS_POST = "login.events.post";

	public static final String LOGOUT_EVENTS_PRE = "logout.events.pre";
	public static final String LOGOUT_EVENTS_POST = "logout.events.post";

	// Default Guest Layout

	public static final String DEFAULT_GUEST_LAYOUT_NAME = "default.guest.layout.name";

	public static final String DEFAULT_GUEST_LAYOUT_COLUMN_ORDER = "default.guest.layout.column.order";

	public static final String DEFAULT_GUEST_LAYOUT_PORTLET_KEYS = "default.guest.layout.portlet.keys";

	public static final String DEFAULT_GUEST_LAYOUT_REFRESH_RATE = "default.guest.layout.refresh.rate";

	public static final String DEFAULT_GUEST_LAYOUT_RESOLUTION = "default.guest.layout.resolution";

	// Default User Layout

	public static final String DEFAULT_USER_LAYOUT_NAME = "default.user.layout.name";

	public static final String DEFAULT_USER_LAYOUT_COLUMN_ORDER = "default.user.layout.column.order";

	public static final String DEFAULT_USER_LAYOUT_PORTLET_KEYS = "default.user.layout.portlet.keys";

	public static final String DEFAULT_USER_LAYOUT_REFRESH_RATE = "default.user.layout.refresh.rate";

	public static final String DEFAULT_USER_LAYOUT_RESOLUTION = "default.user.layout.resolution";

	// Layouts

	public static final String LAYOUT_REFRESH_RATES = "layout.refresh.rates";

	public static final String LAYOUT_ADD_PORTLETS = "layout.add.portlets";

	public static final String LAYOUT_GROUP_MARKER = "layout.group.marker";

	public static final String LAYOUT_NAME_MAX_LENGTH = "layout.name.max.length";

	public static final String LAYOUT_TABS_PER_ROW = "layout.tabs.per.row";

	public static final String LAYOUT_REMEMBER_WINDOW_STATE_MAXIMIZED = "layout.remember.window.state.maximized";

	public static final String LAYOUT_GUEST_SHOW_MAX_ICON = "layout.guest.show.max.icon";

	public static final String LAYOUT_GUEST_SHOW_MIN_ICON = "layout.guest.show.min.icon";

	public static final String LAYOUT_SHOW_PORTLET_ACCESS_DENIED = "layout.show.portlet.access.denied";

	public static final String LAYOUT_SHOW_PORTLET_INACTIVE = "layout.show.portlet.inactive";
	
	public static final String PORTLETS_EXCLUDED_FOR_LAYOUT = "portlets.excluded.from.layout";
	

	// Preferences

	public static final String PREFERENCE_VALIDATE_ON_STARTUP = "preference.validate.on.startup";

	// Images

	public static final String IMAGE_DEFAULT = "image.default";

	// Amazon License Keys

	public static final String AMAZON_LICENSE = "amazon.license.";

	// Google License Keys

	public static final String GOOGLE_LICENSE = "google.license.";

	// Instant Messenger

	public static final String AIM_LOGIN = "aim.login";
	public static final String AIM_PASSWORD = "aim.password";

	public static final String ICQ_JAR = "icq.jar";

	public static final String ICQ_LOGIN = "icq.login";
	public static final String ICQ_PASSWORD = "icq.password";

	public static final String MSN_LOGIN = "msn.login";
	public static final String MSN_PASSWORD = "msn.password";

	public static final String YM_LOGIN = "ym.login";
	public static final String YM_PASSWORD = "ym.password";

	// Lucene Search

	public static final String INDEX_ON_STARTUP = "index.on.startup";
	public static final String INDEX_WITH_THREAD = "index.with.thread";
	public static final String LUCENE_DIR = "lucene.dir";

	// Value Object

	public static final String VALUE_OBJECT_CACHEABLE = "value.object.cacheable";

	public static final String VALUE_OBJECT_MAX_SIZE = "value.object.max.size";

	// XSS (Cross Site Scripting)

	public static final String XSS_ALLOW = "xss.allow";

	// Cache Server

	public static final String CACHE_CLEAR_ON_STARTUP = "cache.clear.on.startup";

	// Document Library Server

	public static final String DL_ROOT_DIR = "dl.root.dir";
	public static final String DL_VERSION_ROOT_DIR = "dl.version.root.dir";

	public static final String DL_FILE_MAX_SIZE = "dl.file.max.size";
	public static final String DL_FILE_EXTENSIONS = "dl.file.extensions";

	public static final String DL_VERSION_CACHE_DIRECTORY_VIEWS = "dl.version.cache.directory.views";

	// Mail Server

	public static final String MAIL_MX_UPDATE = "mail.mx.update";

	public static final String MAIL_HOOK_IMPL = "mail.hook.impl";

	public static final String MAIL_HOOK_CYRUS_ADD_USER = "mail.hook.cyrus.add.user";
	public static final String MAIL_HOOK_CYRUS_DELETE_USER = "mail.hook.cyrus.delete.user";
	public static final String MAIL_HOOK_CYRUS_HOME = "mail.hook.cyrus.home";

	public static final String MAIL_HOOK_SENDMAIL_ADD_USER = "mail.hook.sendmail.add.user";
	public static final String MAIL_HOOK_SENDMAIL_CHANGE_PASSWORD = "mail.hook.sendmail.change.password";
	public static final String MAIL_HOOK_SENDMAIL_DELETE_USER = "mail.hook.sendmail.delete.user";
	public static final String MAIL_HOOK_SENDMAIL_HOME = "mail.hook.sendmail.home";
	public static final String MAIL_HOOK_SENDMAIL_VIRTUSERTABLE = "mail.hook.sendmail.virtusertable";

	public static final String MAIL_HOOK_RHEMS_ADD_USER = "mail.hook.rhems.add.user";
	public static final String MAIL_HOOK_RHEMS_DELETE_USER = "mail.hook.rhems.delete.user";

	public static final String MAIL_HOOK_RHEMS_LDAP_INITIAL_CONTEXT_FACTORY = "mail.hook.rhems.ldap.initial.context.factory";
	public static final String MAIL_HOOK_RHEMS_LDAP_SERVER_CONTEXT = "mail.hook.rhems.ldap.server.context";
	public static final String MAIL_HOOK_RHEMS_LDAP_SECURITY_AUTHENTICATION = "mail.hook.rhems.ldap.security.authentication";
	public static final String MAIL_HOOK_RHEMS_LDAP_SECURITY_PRINCIPAL = "mail.hook.rhems.ldap.security.principal";
	public static final String MAIL_HOOK_RHEMS_LDAP_SECURITY_CREDENTIALS = "mail.hook.rhems.ldap.security.credentials";

	public static final String MAIL_HOOK_RHEMS_LOGIN = "mail.hook.rhems.login";
	public static final String MAIL_HOOK_RHEMS_WEB_SERVER = "mail.hook.rhems.web.server";

	public static final String MAIL_BOX_STYLE = "mail.box.style";

	public static final String MAIL_USERNAME_REPLACE = "mail.username.replace";

	public static final String MAIL_JUNK_MAIL_WARNING_SIZE = "mail.junk-mail.warning.size";

	public static final String MAIL_TRASH_WARNING_SIZE = "mail.trash.warning.size";

	public static final String MAIL_AUDIT_TRAIL = "mail.audit.trail";

	public static final String MAIL_ATTACHMENTS_MAX_SIZE = "mail.attachments.max.size";

	// Web Server

	public static final String WEB_SERVER_HTTP_PORT = "web.server.http.port";
	public static final String WEB_SERVER_HTTPS_PORT = "web.server.https.port";

	public static final String WEB_SERVER_HOST = "web.server.host";

	public static final String WEB_SERVER_PROTOCOL = "web.server.protocol";

	// Address Book Portlet

	public static final String ADDRESS_BOOK_CONTACT_JOB_CLASSES = "address.book.contact.job.classes";

	// Calendar Portlet

	public static final String CALENDAR_EVENT_TYPES = "calendar.event.types";

	public static final String CALENDAR_SYNC_EVENTS_ON_STARTUP = "calendar.sync.events.on.startup";

	// Chat Portlet

	public static final String CHAT_SERVER_DEFAULT_PORT = "chat.server.default.port";

	// Image Gallery Portlet

	public static final String IG_IMAGE_MAX_SIZE = "ig.image.max.size";
	public static final String IG_IMAGE_EXTENSIONS = "ig.image.extensions";

	// JCVS Portlet

	public static final String JCVS_LICENSE = "jcvs.license";

	// Journal Portlet

	public static final String JOURNAL_ARTICLE_TYPES = "journal.article.types";

	public static final String JOURNAL_IMAGE_SMALL_MAX_SIZE = "journal.image.small.max.size";
	public static final String JOURNAL_IMAGE_EXTENSIONS = "journal.image.extensions";

	public static final String JOURNAL_TRANSFORMER_LISTENER = "journal.transformer.listener";

	// Shopping Portlet

	public static final String SHOPPING_CART_MIN_QTY_MULTIPLE = "shopping.cart.min.qty.multiple";

	public static final String SHOPPING_CATEGORY_FORWARD_TO_CART = "shopping.category.forward.to.cart";

	public static final String SHOPPING_CATEGORY_SHOW_ITEM_DESCRIPTION = "shopping.category.show.item.description";

	public static final String SHOPPING_CATEGORY_SHOW_ITEM_PROPERTIES = "shopping.category.show.item.properties";

	public static final String SHOPPING_CATEGORY_SHOW_ITEM_PRICES = "shopping.category.show.item.prices";

	public static final String SHOPPING_CATEGORY_SHOW_SPECIAL_ITEM_DESCRIPTION = "shopping.category.show.special.item.description";

	public static final String SHOPPING_ITEM_SHOW_AVAILABILITY = "shopping.item.show.availability";

	public static final String SHOPPING_IMAGE_SMALL_MAX_SIZE = "shopping.image.small.max.size";
	public static final String SHOPPING_IMAGE_MEDIUM_MAX_SIZE = "shopping.image.medium.max.size";
	public static final String SHOPPING_IMAGE_LARGE_MAX_SIZE = "shopping.image.large.max.size";
	public static final String SHOPPING_IMAGE_EXTENSIONS = "shopping.image.extensions";

	// Translator Portlet

	public static final String TRANSLATOR_DEFAULT_LANGUAGES ="translator.default.languages";

	public static void init() {
		PropsLoader.init();
	}

	public static boolean containsKey(String key) {
		return PropsLoader.getInstance().containsKey(key);
	}

	public static String get(String key) {
		return PropsLoader.getInstance().get(key);
	}

	public static void set(String key, String value) {
		PropsLoader.getInstance().set(key, value);
	}

	public static String[] getArray(String key) {
		return PropsLoader.getInstance().getArray(key);
	}

	public static Properties getProperties() {
		return PropsLoader.getInstance().getProperties();
	}

}