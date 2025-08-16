package com.ducnh.highperformance;

public final class PrintBufferUtil {
	private static final String NEWLINE = System.lineSeparator();
	private static final String EMPTY_STRING = "";
	private static final String[] BYTE2HEX_PAD = new String[256];
	
	static {
		int i;
		for (i = 0; i < 10; i++) {
			BYTE2HEX_PAD[i] = "0" + i;
		}
		
		for (; i < 16; i++) {
			final char c = (char)('a' + i - 10);
			BYTE2HEX_PAD[i] = "0" + c;
		}
		
		for (; i < BYTE2HEX_PAD.length; i++) {
			BYTE2HEX_PAD[i] = Integer.toHexString(i);
		}
	}
	
	private PrintBufferUtil() {
		
	}
	
	public static String hexDump(final DirectBuffer buffer) {
		return hexDump(buffer, 0, buffer.capacity());
	}
	
	public static String hexDump(final DirectBuffer buffer, final int fromIndex, final int length) {
		return HexUtil.hexDump(buffer, fromIndex, length);
	}
	
	public static String hexDump(final byte[] array) {
		return hexDump(array, 0, array.length);
	}
	
	public static String hexDump(final byte[] array, final int fromIndex, final int length) {
		return HexUtil.hexDump(array, fromIndex, length);
	}
	
	public static String prettyHexDump(final DirectBuffer buffer) {
		return prettyHexDump(buffer, 0, buffer.capacity());
	}
	
	public static String prettyHexDump(final DirectBuffer buffer, final int offset, final int length) {
		return HexUtil.prettyHexDump(buffer, offset, length);
	}
	
	public static void appendPrettyHexDump(final StringBuilder dump, final DirectBuffer buffer) {
		appendPrettyHexDump(dump, buffer, 0, buffer.capacity());
	}
	
	public static void appendPrettyHexDump(final StringBuilder dump, final DirectBuffer buffer, final int offset, final int length) {
		HexUtil.appendPrettyHexDump(dump, buffer, offset, length);
	}
	
	public static String byteToHexStringPadded(final int value) {
		return BYTE2HEX_PAD[value & 0xff];
	}
	
	static final class HexUtil {
		private static final char[] BYTE2CHAR = new char[256];
		private static final char[] HEXDUMP_TABLE = new char[256 * 4];
		private static final String[] HEX_PADDING = new String[16];
		private static final String[] HEXDUMP_ROW_PREFIXES = new String[65536 >> 4];
		private static final String[] BYTE2HEX = new String[256];
		private static final String[] BYTE_PADDING = new String[16];
		
		static {
			final char[] digits = "0123456789abcdef".toCharArray();
			for (int i = 0; i < 256; i++) {
				HEXDUMP_TABLE[i << 1] = digits[i >>> 4 & 0x0F];
				HEXDUMP_TABLE[(i << 1) + 1] = digits[i & 0x0F];
			}
			
			int i;
			
			for (i = 0; i < HEX_PADDING.length; i++) {
				final int padding = HEX_PADDING.length - i;
				final StringBuilder buf = new StringBuilder(padding * 3);
				for (int j = 0; j < padding; j++) {
					buf.append("   ");
				}
				HEX_PADDING[i] = buf.toString();
			}
			
			for (i = 0; i < HEXDUMP_ROW_PREFIXES.length; i++) {
				final StringBuilder buf = new StringBuilder(12);
				buf.append(NEWLINE);
				buf.append(Long.toHexString((long) i << 4 & 0xFFFFFFFFL | 0x100000000L));
				buf.setCharAt(buf.length() - 9, '|');
				buf.append('|');
				HEXDUMP_ROW_PREFIXES[i] = buf.toString();
			}
			
			for (i = 0; i < BYTE2HEX.length; i++) {
				BYTE2HEX[i] = ' ' + byteToHexStringPadded(i);
			}
			
			for (i = 0; i < BYTE_PADDING.length; i++) {
				final int padding = BYTE_PADDING.length - i;
				final StringBuilder buf =  new StringBuilder(padding);
				for (int j = 0; j < padding; j++) {
					buf.append(' ');
				}
				BYTE_PADDING[i] = buf.toString();
			}
			
			for (i = 0; i < BYTE2CHAR.length; i++) {
				BYTE2CHAR[i] = (i <= 0x1f || i >= 0x7f) ? '.' : (char)i;
			}
		}
		
		static short getUnsignedByte(final DirectBuffer buffer, final int index) {
			return (short) (buffer.getByte(index) & 0xFF);
		}
		
		static String hexDump(final DirectBuffer buffer, final int fromIndex, final int length) {
			if (length < 0) {
				throw new IllegalArgumentException("length: " + length);
			}
			
			if (length == 0) {
				return "";
			}
			
			final int endIndex = fromIndex + length;
			final char[] buf = new char[length << 1];
			
			for (int dstIdx = 0, srcIdx = fromIndex; srcIdx < endIndex; srcIdx++, dstIdx += 2) {
				System.arraycopy(
						HEXDUMP_TABLE, getUnsignedByte(buffer, srcIdx) << 1, buf, dstIdx, 2);
			}
			
			return new String(buf);
		}
		
		static String hexDump(final byte[] array, final int fromIndex, final int length) {
			if (length < 0) {
				throw new IllegalArgumentException("length < 0: " + length);
			}
			
			if (length == 0) {
				return "";
			}
			
			final int endIndex = fromIndex + length;
			final char[] buf = new char[length << 1];
			
			for (int dstIdx = 0, srcIdx = fromIndex; srcIdx < endIndex; srcIdx++, dstIdx += 2) {
				System.arraycopy(HEXDUMP_TABLE, (array[srcIdx] & 0xFF) << 1, buf, dstIdx, 2);
			}
			
			return new String(buf);
		}
		
		static String prettyHexDump(final DirectBuffer buffer, final int offset, final int length) {
			if (length == 0) {
				return EMPTY_STRING;
			} else {
				final int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
				final StringBuilder stringBuilder = new StringBuilder(rows  * 80);
				appendPrettyHexDump(stringBuilder, buffer, offset, length);
				
				return stringBuilder.toString();
			}
		}
		
		static boolean isOutOfBounds(final int index, final int length, final int capacity) {
			return (index | length | (index + length) | (capacity - (index + length))) < 0;
		}
		
		static void appendPrettyHexDump(
				final StringBuilder dump, final DirectBuffer buffer, final int offset, final int length) {
			if (isOutOfBounds(offset, length, buffer.capacity())) {
				throw new IndexOutOfBoundsException(
					"expected: " + " 0 <= offset(" + offset + ") <= offset + length(" + length + ") <= " + 
					"buffer.capacity(" + buffer.capacity() + ')');
			}
			
			if (length == 0) {
				return;
			}
			dump.append("         +-------------------------------------------------+")
            .append(NEWLINE)
            .append("         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |")
            .append(NEWLINE)
            .append("+--------+-------------------------------------------------+----------------+");

	        final int fullRows = length >>> 4;
	        final int remainder = length & 0xF;
	
	        // Dump the rows which have 16 bytes.
	        for (int row = 0; row < fullRows; row++)
	        {
	            final int rowStartIndex = (row << 4) + offset;
	
	            appendHexDumpRowPrefix(dump, row, rowStartIndex);
	
	            final int rowEndIndex = rowStartIndex + 16;
	            for (int j = rowStartIndex; j < rowEndIndex; j++)
	            {
	                dump.append(BYTE2HEX[getUnsignedByte(buffer, j)]);
	            }
	
	            dump.append(" |");
	
	            for (int j = rowStartIndex; j < rowEndIndex; j++)
	            {
	                dump.append(BYTE2CHAR[getUnsignedByte(buffer, j)]);
	            }
	
	            dump.append('|');
	        }
	
	        // Dump the last row which has less than 16 bytes.
	        if (remainder != 0)
	        {
	            final int rowStartIndex = (fullRows << 4) + offset;
	            appendHexDumpRowPrefix(dump, fullRows, rowStartIndex);
	
	            final int rowEndIndex = rowStartIndex + remainder;
	            for (int j = rowStartIndex; j < rowEndIndex; j++)
	            {
	                dump.append(BYTE2HEX[getUnsignedByte(buffer, j)]);
	            }
	
	            dump.append(HEX_PADDING[remainder]);
	            dump.append(" |");
	
	            for (int j = rowStartIndex; j < rowEndIndex; j++)
	            {
	                dump.append(BYTE2CHAR[getUnsignedByte(buffer, j)]);
	            }
	
	            dump.append(BYTE_PADDING[remainder]);
	            dump.append('|');
	        }
	
	        dump.append(NEWLINE)
	            .append("+--------+-------------------------------------------------+----------------+");
	    }

	    static void appendHexDumpRowPrefix(final StringBuilder dump, final int row, final int rowStartIndex)
	    {
	        if (row < HEXDUMP_ROW_PREFIXES.length)
	        {
	            dump.append(HEXDUMP_ROW_PREFIXES[row]);
	        }
	        else
	        {
	            dump.append(NEWLINE);
	            dump.append(Long.toHexString(rowStartIndex & 0xFFFFFFFFL | 0x100000000L));
	            dump.setCharAt(dump.length() - 9, '|');
	            dump.append('|');
	        }
	    }
	}
}
