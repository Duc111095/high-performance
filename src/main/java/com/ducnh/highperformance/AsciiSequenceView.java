package com.ducnh.highperformance;

public class AsciiSequenceView implements CharSequence{

	private DirectBuffer buffer;
	private int offset;
	private int length;
	
	public AsciiSequenceView() {}
	
	public AsciiSequenceView(final DirectBuffer buffer, final int offset, final int length) {
		this.buffer = buffer;
		this.offset = offset;
		this.length = length;
	}
	

	@Override
	public int length() {
		return length;
	}

	public DirectBuffer buffer() {
		return buffer;
	}
	
	public int offset() {
		return offset;
	}
	
	@Override
	public char charAt(int index) {
		if (index < 0 || index >= length) {
			throw new StringIndexOutOfBoundsException("index=" + index + " length=" + length);
		}
		return (char) buffer.getByte(index + offset);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		if (start < 0) {
			throw new StringIndexOutOfBoundsException("start=" + start);
		}
		if (end > length) {
			throw new StringIndexOutOfBoundsException("end=" + end + " length=" + length);
		}
		
		if (end - start < 0) {
			throw new StringIndexOutOfBoundsException("start=" + start + " end=" + end);
		}
		return new AsciiSequenceView(buffer, offset + start, end - start);
	}
	
	public AsciiSequenceView wrap(final DirectBuffer buffer, final int offset, final int length) {
		this.buffer = buffer;
		this.offset = offset;
		this.length = length;
		return this;
	}
	
	public void reset() {
		this.buffer = null;
		this.offset = 0;
		this.length = 0;
	}
	
	public int getBytes(final MutableDirectBuffer dstBuffer, final int dstOffset) {
		if (buffer == null || length <= 0) {
			return 0;
		}
		
		dstBuffer.putBytes(dstOffset, buffer, offset, length);
		return length;
	}
	
	public String toString() {
		if (buffer == null || length <= 0) {
			return "";
		}
		return buffer.getStringWithoutLengthAscii(offset, length);
	}
}
