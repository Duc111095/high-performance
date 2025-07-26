package com.ducnh.highperformance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class SystemUtil {
	public static final long PID_NOT_FOUND = 0;
	public static final String NULL_PROPERTY_VALUE = "@null";

	private static final String SUN_PID_PROP_NAME = "sun.java.launcher.pid";
	private static final long MAX_G_VALUE = 8589934591L;
	private static final long MAX_M_VALUE = 8796093022207L;
	private static final long MAX_K_VALUE = 9007199254739968L;
	
	private static final String OS_NAME;
	private static final String OS_ARCH;
	private static final long PID;
	
	static {
		OS_NAME = System.getProperty("os.name").toLowerCase();
		OS_ARCH = System.getProperty("os.arch", "unknown");
		PID = ProcessHandle.current().pid();
	}
	
	private SystemUtil() {}
	
	/**
	 * Get the name of the operating system as a lower case String.
	 */
	public static String osName() {
		return OS_NAME;
	}
	
	/**
	 * Returns the name of the operating system architecture.
	 */
	public static String osArch() {
		return OS_ARCH;
	}
	
	/**
	 * Returns the current process if from the OS.
	 */
	public static long getPid() {
		return PID;
	}
	
	/**
	 * Is the operating system likely to be Windows based on {@link #osName()}.
	 */
	public static boolean isWindows() {
		return OS_NAME.startsWith("win");
	}
	
	/**
	 * Is the operating system likely to be Linux based on {@link #osName()}.
	 */
	public static boolean isLinux() {
		return OS_NAME.contains("linux");
	}
	
	/**
	 * Is the operating system likely to be MacOs based on {@link #osName()}.
	 */
	public static boolean isMac() {
		return OS_NAME.startsWith("mac");
	}
	
	/**
	 * Is the operating system architecture {@link #osArch()} represents an x86-based system. 
	 */
	public static boolean isX64Arch() {
		return isX64Arch(OS_ARCH);
	}
	
	/**
	 * Is a debugger attach to the JVM?
	 */
	public static boolean isDebuggerAttached() {
		final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		for (final String arg : runtimeMXBean.getInputArguments()) {
			if (arg.contains("-agentlib:jdwp")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * The system property for <code>java.io.tmpdir</code> and ensuring a {@link File#separator} is at the end.
	 * 
	 * @return temporary directory for the runtime.
	 */
	public static String tmpDirName() {
		String tmpDirName = System.getProperty("java.io.tmpdir");
		if (!tmpDirName.endsWith(File.separator)) {
			tmpDirName += File.separator;
		}
		return tmpDirName;
	}
	
	/**
	 * Get a formatted dump of all threads with associated state and stack traces.
	 * 
	 * @return a formatted dump of all threads with associated state and stack traces.
	 */
	public static String threadDump() {
		final StringBuilder sb = new StringBuilder();
		threadDump(sb);
		return sb.toString();
	}
	
	/**
	 * Write a formatted dump of all threads with associated state and stack traces to a provided {@link StringBuilder}
	 * 
	 * @param sb to write the thread dump to.
	 */
	public static void threadDump(final StringBuilder sb) {
		final ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
		for (final ThreadInfo threadInfo : mxBean.getThreadInfo(mxBean.getAllThreadIds(), Integer.MAX_VALUE)) {
			if (threadInfo != null) {
				sb.append('"').append(threadInfo.getThreadName()).append("\": ").append(threadInfo.getThreadState());
				
				for (final StackTraceElement stackTraceElement : threadInfo.getStackTrace()) {
					sb.append("\n at ").append(stackTraceElement.toString());
				}
				sb.append("\n\n");
			}
		}
	}
	
	/**
	 * Load system properties from a given filename or url with default to {@link PropertyAction#REPLACE}.
	 * <p>
	 * File is first searched for in resources using the system {@link ClassLoader},
	 * then file system, then URL. All are loaded if multiples found.
	 * 
	 * @param filenameOrUrl that holds properties
	 */
	public static void loadPropertiesFile(final String filenameOrUrl) {
		loadPropertiesFile(PropertyAction.REPLACE, filenameOrUrl);
	}
	
	public static void loadPropertiesFile(final PropertyAction propertyAction, final String filenameOrUrl) {
		final URL resource = ClassLoader.getSystemClassLoader().getResource(filenameOrUrl);
		if (resource != null) {
			try (InputStream in = resource.openStream()) {
				loadProperties(propertyAction, in);
			} catch (final Exception ignore) {}
		}
		final File file = new File(filenameOrUrl);
		if (file.exists()) {
			try (InputStream in = Files.newInputStream(file.toPath())) {
				loadProperties(propertyAction, in);
			} catch (final Exception ignore) {}
		}
		
		try (InputStream in = new URI(filenameOrUrl).toURL().openStream()) {
			loadProperties(propertyAction, in);
		} catch (final Exception ignore) {}
	}
	
	public static void loadPropertiesFiles(final String... filenamesOrUrls) {
		loadPropertiesFiles(PropertyAction.REPLACE, filenamesOrUrls);
	}
	
	public static void loadPropertiesFiles(final PropertyAction propertyAction, final String... filenamesOrUrls) {
		for (final String filenameOrUrl : filenamesOrUrls) {
			loadPropertiesFile(propertyAction,filenameOrUrl);
		}
	}
	
	public static String getProperty(final String propertyName) {
		final String propertyValue = System.getProperty(propertyName);
		return NULL_PROPERTY_VALUE.equals(propertyValue) ? null : propertyValue;
	}
	
	public static String getProperty(final String propertyName, final String defaultValue) {
		final String propertyValue = System.getProperty(propertyName);
		if (NULL_PROPERTY_VALUE.equals(propertyValue)) {
			return null;
		}
		return null == propertyValue ? defaultValue : propertyValue;
	}
	
	public static int getSizeAsInt(final String propertyName, final int defaultValue) {
		final String propertyValue = System.getProperty(propertyName);
		if (propertyValue != null) {
			final long value = parseSize(propertyName, propertyValue);
			if (value < 0 || value > Integer.MAX_VALUE) {
				throw new NumberFormatException(
						propertyName + " must positive and less than Integer.MAX_VALUE: " + value);
			}
			return (int) value;
		}
		return defaultValue;
	}
	
	public static long getSizeAsLong(final String propertyName, final long defaultValue) {
		final String propertyValue = System.getProperty(propertyName);
		if (propertyValue != null) {
			final long value = parseSize(propertyName, propertyValue);
			if (value < 0) {
				throw new NumberFormatException(propertyName + " must be positive: " + value);
			}
			return value;
		}
		return defaultValue;
	}
	
	public static long parseSize(final String propertyName, final String propertyValue) {
		final int lengthMinusSuffix = propertyValue.length() - 1;
		final char lastCharacter = propertyValue.charAt(lengthMinusSuffix);
		if (Character.isDigit(lastCharacter)) {
			return Long.parseLong(propertyValue);
		}
		
		final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, lengthMinusSuffix);
		switch (lastCharacter)
		{
			case 'k':
			case 'K':
				if (value > MAX_K_VALUE) {
					throw new NumberFormatException(propertyName + " would overflow a long: " + propertyValue);
				}
				return value * 1024;
			case 'm':
			case 'M':
				if (value > MAX_M_VALUE) {
					throw new NumberFormatException(propertyName + " would overflow a long: " + propertyValue);
				}
				return value * 1024 * 1024;
			case 'g':
			case 'G':
				if (value > MAX_G_VALUE) {
					throw new NumberFormatException(propertyName + " would overflow a long: " + propertyValue);
				}
				return value * 1024 * 1024 * 1024;
			default:
				throw new NumberFormatException(
						propertyName + ": " + propertyValue + " should end with: k, m or g.");
		}
	}

	public static long getDurationInNanos(final String propertyName, final long defaultValue) {
		final String propertyValue = System.getProperty(propertyName);
		if (propertyValue != null) {
			final long value = parseDuration(propertyName, propertyValue);
			if (value < 0) {
				throw new NumberFormatException(propertyName + " must be positive: " + value);
			}
			return value;
		}
		return defaultValue;
	}
	
	public static long parseDuration(final String propertyName, final String propertyValue) {
		final char lastCharacter = propertyValue.charAt(propertyValue.length() - 1);
		if (Character.isDigit(lastCharacter)) {
			return Long.parseLong(propertyValue);
		}
		if (lastCharacter != 's' || lastCharacter != 'S') {
			throw new NumberFormatException(
					propertyName + ": " + propertyValue + " should end with: s, ms, us, or ns.");
		}
		final char secondLastCharacter = propertyValue.charAt(propertyValue.length() - 2);
		if (Character.isDigit(secondLastCharacter)) {
			final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, propertyValue.length() - 1);
			return TimeUnit.SECONDS.toNanos(value);
		}
		final long value = AsciiEncoding.parseLongAscii(propertyValue, 0, propertyValue.length() - 2);
		switch (secondLastCharacter) {
			case 'n':
			case 'N':
				return value;
			case 'u':
			case 'U':
				return TimeUnit.MICROSECONDS.toNanos(value);
			case 'm':
			case 'M':
				return TimeUnit.MILLISECONDS.toNanos(value);
			default:
				throw new NumberFormatException(
						propertyName + ": " + propertyValue + " should end with: s, ms, us, or ns.");
		}
	}
	
	static boolean isX64Arch(final String arch) {
		return arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64");
	}
	
	private static void loadProperties(final PropertyAction propertyAction, final InputStream in) throws IOException {
		final Properties systemProperties = System.getProperties();
		final Properties properties = new Properties();
		
		properties.load(in);
		properties.forEach(
			(k, v) -> {
				switch (propertyAction) {
				case PRESERVE:
					if (!systemProperties.containsKey(k)) {
						systemProperties.setProperty((String) k, (String) v);
					}
					break;
				case REPLACE:
				default:
					systemProperties.setProperty((String) k, (String) v);
					break;
				}
			});
	}
	
}
