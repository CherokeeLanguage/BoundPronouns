package com.cherokeelessons.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {
	private static String LOG_TAG = "[LOGGER] ";
	public static String getLOG_TAG() {
		return LOG_TAG;
	}

	public static void setLOG_TAG(String lOG_TAG) {
		LOG_TAG = lOG_TAG;
	}

	@GwtIncompatible
	private static final ConsoleHandler handler = new ConsoleHandler();
	
	@GwtIncompatible
	private static final Formatter f1 = new Formatter() {
		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder(256);
			sb.append(LOG_TAG);
			sb.append(new java.util.Date(record.getMillis()));
			appendSourceClassName(sb, record);
			sb.append(", ");
			sb.append(record.getMessage());
			sb.append(System.lineSeparator());
			return sb.toString();
		}

	};

	@GwtIncompatible
	private static void appendSourceClassName(StringBuilder sb, LogRecord record) {
		String sourceClassName = record.getSourceClassName();
		if (sourceClassName != null) {
			sb.append(" ");
			sb.append(sourceClassName);
		}
	}

	@GwtIncompatible
	public static Logger getLogger() {
		return getLogger(LOG_TAG);
	}

	public static Logger getGwtLogger(Handler h) {
		return getGwtLogger(h, LOG_TAG);
	}
	
	@GwtIncompatible
	public static Logger getLogger(String tag) {
		Logger log = Logger.getLogger(tag);
		handler.setFormatter(f1);
		log.setUseParentHandlers(false);
		log.addHandler(handler);
		return log;
	}

	public static Logger getGwtLogger(Handler h, final String tag) {
		Logger log = Logger.getLogger(tag);
		h.setFormatter(new Formatter() {
			@Override
			public String format(LogRecord record) {
				StringBuilder sb = new StringBuilder(256);
				sb.append("["+tag+"] ");
				sb.append(new java.util.Date(record.getMillis()));
				sb.append(", ");
				sb.append(record.getMessage());
				sb.append("\n");
				return sb.toString();
			}

		});
		log.setUseParentHandlers(false);
		log.addHandler(h);
		return log;
	}
}

@Retention(RetentionPolicy.CLASS)
@Target({
    ElementType.TYPE, ElementType.METHOD,
    ElementType.CONSTRUCTOR, ElementType.FIELD })
@Documented
@interface GwtIncompatible {
  /**
   * An attribute that can be used to explain why the code is incompatible.
   * A GwtIncompatible annotation can have any number of attributes; attributes
   * are for documentation purposes and are ignored by the GWT compiler.
   */
  String value() default "";
}