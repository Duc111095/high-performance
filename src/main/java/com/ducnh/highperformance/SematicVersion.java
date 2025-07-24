package com.ducnh.highperformance;

public class SematicVersion {
	private SematicVersion() {}
	public static int compose(final int major, final int minor, final int patch) {
		if (major < 0 || major > 255) {
			throw new IllegalArgumentException("major must be 0-255: " + major);
		}
		if (minor < 0 || minor > 255) {
			throw new IllegalArgumentException("minor must be 0-255: " + minor);
		} 
		if (patch < 0 || patch > 255) {
			throw new IllegalArgumentException("patch must be 0-255: " + patch);
		}
		if (major + minor + patch == 0) {
			throw new IllegalArgumentException("all parts cannot be zero");
		}
		return (major << 16) | (minor << 8) | patch;
	}
	
	public static int major(final int version) {
		return (version >> 16) & 0xFF;
	}
	
	public static int minor(final int version) {
		return (version >> 8) & 0xFF;
	}
	
	public static int patch(final int version) {
		return version & 0xFF; 
	}
	
	public static String toString(final int version) {
		return major(version) + "." + minor(version) + patch(version);
	}
}
