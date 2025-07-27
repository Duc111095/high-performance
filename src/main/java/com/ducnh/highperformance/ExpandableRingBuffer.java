package com.ducnh.highperformance;

import static com.ducnh.highperformance.BitUtil.SIZE_OF_LONG;
import static com.ducnh.highperformance.BitUtil.SIZE_OF_INT;

public class ExpandableRingBuffer {

	public static final int MAX_CAPACITY = 1 << 30;
	public static final int HEADER_ALIGNMENT = SIZE_OF_LONG;
	public static final int HEADER_LENGTH = SIZE_OF_INT + SIZE_OF_INT;
	
	@FunctionalInterface
	public interface MessageConsumer{
		boolean onMessage(MutableDirectBuffer buffer, int offset, int length, int headOffset);
	}
	
	private static final int MESSAGE_LENGTH_OFFSET = 0;
	private static final int MESSAGE_TYPE_OFFSET = MESSAGE_LENGTH_OFFSET + SIZE_OF_INT;
	private static final int MESSAGE_TYPE_PADDING = 0;
	private static final int MESSAGE_TYPE_DATA = 1;
	
	private final int maxCapacity;
	private int capacity;
	private int mask;
	private long head;
	private long tail;
	private final UnsafeBuffer buffer = new UnsafeBuffer();
}
