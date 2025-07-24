package com.ducnh.highperformance;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_INT;

public interface DirectBuffer {
	/**
	 * Length of the header of strings on denote the length of the string in bytes.
	 */
	int STR_HEADER_LEN = SIZE_OF_INT;
	String DISABLE_ARRAY_CONTENT_PRINTOUT_PROP_NAME = "com.ducnh.array.printout";
	String DISABLE_BOUNDS_CHECKS_PROP_NAME = "com.ducnh.bounds.checks";
	boolean SHOULD_BOUNDS_CHECKS = !"true".equals(DISABLE_ARRAY_CONTENT_PRINTOUT_PROP_NAME)
}
