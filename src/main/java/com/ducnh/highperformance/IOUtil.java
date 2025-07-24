package com.ducnh.highperformance;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.BiConsumer;


import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.nio.file.StandardOpenOption.*;

@SuppressWarnings({"deprecation", "removal"})
public class IOUtil {
	public static final int BLOCK_SIZE = 4 * 1024;
	
	private static final byte[] FILLER = new byte[BLOCK_SIZE];
	private static final int MAP_READ_ONLY = 0;
	private static final int MAP_READ_WRITE = 1;
	private static final int MAP_PRIVATE = 2;
	private IOUtil() {}
	
	
	public static void fill(final FileChannel fileChannel, final long position, final long length, final byte value) {
		try {
			final byte[] filler;
			if (value != 0) {
				filler = new byte[BLOCK_SIZE];
				Arrays.fill(filler, value);
			}
			else {
				filler = FILLER;
			}
			final ByteBuffer byteBuffer = ByteBuffer.wrap(filler);
			fileChannel.position(position);
			
			final int blocks = (int) (length / BLOCK_SIZE);
			final int blockRemainder = (int) (length % BLOCK_SIZE);
			
			for (int i = 0; i < blocks; i++) {
				byteBuffer.position(0);
				fileChannel.write(byteBuffer);
			} 
			if (blockRemainder > 0) {
				byteBuffer.position(0);
				byteBuffer.limit(blockRemainder);
				fileChannel.write(byteBuffer);
			}
		} catch (final IOException ex) {
			LangUtil.rethrowUnchecked(ex);
		}
	}
	
	public static void delete(final File file, final boolean ignoreFailures) {
		if (file.exists()) {
			if (file.isDirectory()) {
				final File[] files = file.listFiles();
				if (files != null) {
					for (final File f : files) {
						delete(f, ignoreFailures);
					}
				}
			}
			
			if (!file.delete() && !ignoreFailures) {
				try {
					Files.delete(file.toPath());
				} catch (final Exception ex) {
					LangUtil.rethrowUnchecked(ex);
				}
			}
		}
	}
	
	public static void delete(final File file, final ErrorHandler errorHandler) {
		try {
			if (file.exists()) {
				if (file.isDirectory()) {
					final File[] files = file.listFiles();
					if (files != null) {
						for (final File f : files) {
							delete(f, errorHandler);
						}
					}
				}
				if (!file.delete()) {
					Files.delete(file.toPath());
				}
			}
		} catch (final Exception ex) {
			errorHandler.onError(ex);;
		}
	}
	
	public static void ensureDirectoryExists(final File directory, final String descriptionLabel) {
		if (!directory.exists()) {
			if((!directory.mkdir())) {
				throw new IllegalArgumentException("could not create " + descriptionLabel + " directory: " + directory);
			}
		}
	}
	
	public static void ensureDirectoryRecreated(final File directory, final String descriptionLabel, final BiConsumer<String, String> callback) {
		if (directory.exists()) {
			delete(directory, false);
			callback.accept(directory.getAbsolutePath(), descriptionLabel);
		}
		if (!directory.mkdir()) {
			throw new IllegalArgumentException("could not create " + descriptionLabel + " directory: " + directory);
		}
	}
	public static void deleteIfExists(final File file) {
		try {
			Files.deleteIfExists(file.toPath());
		} catch (final IOException ex) {
			LangUtil.rethrowUnchecked(ex);
		}
	}
	
	public static void deleteIfExists(final File file, final ErrorHandler errorHandler) {
		try {
			Files.deleteIfExists(file.toPath());
		} catch (final Exception ex) {
			errorHandler.onError(ex);
		}
	}
	
	public static FileChannel createEmptyFile(final File file, final long length) {
		return createEmptyFile(file, length, true);
	}
	
	public static FileChannel createEmptyFile(final File file, final long length, final boolean fillWithZeros) {
		ensureDirectoryExists(file.getParentFile(), file.getParent());
		
		FileChannel templateFile = null;
		try {
			final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
			randomAccessFile.setLength(length);
			templateFile = randomAccessFile.getChannel();
			
			if (fillWithZeros) {
				fill(templateFile, 0, length, (byte) 0);
			}
		}  catch (final IOException ex) {
			LangUtil.rethrowUnchecked(ex);
		}
		return templateFile;
	}
	
	public static MappedByteBuffer mapExistingFile(final File location, final String descriptionLabel) {
		return mapExistingFile(location, READ_WRITE, descriptionLabel);
	}
	
	public static MappedByteBuffer mapExistingFile(final File location, final String descriptionLabel, final long offset, final long length) {
		return mapExistingFile(location, READ_WRITE, descriptionLabel, offset, length);
	}
	
	public static MappedByteBuffer mapExistingFile(
			final File location, final FileChannel.MapMode mapMode, final String descriptionLabel) {
		checkFileExists(location, descriptionLabel);
		
		MappedByteBuffer mappedByteBuffer = null;
		try (RandomAccessFile file = new RandomAccessFile(location, getFileMode(mapMode));
				FileChannel channel = file.getChannel()) {
			mappedByteBuffer = channel.map(mapMode, 0, channel.size());
		} catch (final Exception ex) {
			LangUtil.rethrowUnchecked(ex);
		}
		return mappedByteBuffer;
	}
	
	public static MappedByteBuffer mapExistingFile(
			final File location,
			final FileChannel.MapMode mapMode,
			final String descriptionLabel,
			final long offset,
			final long length) {
		checkFileExists(location, descriptionLabel);
		
		MappedByteBuffer mappedByteBuffer = null;
		try (RandomAccessFile file = new RandomAccessFile(location, getFileMode(mapMode));
				FileChannel channel = file.getChannel()) {
			mappedByteBuffer = channel.map(mapMode, offset, length);
		} catch (final IOException ex) {
			LangUtil.rethrowUnchecked(ex);
		}
		return mappedByteBuffer;
	}
	
	public static MappedByteBuffer mapNewFile(final File location, final long length) {
		return mapNewFile(location, length, true);
	}
	
	public static MappedByteBuffer mapNewFile(final File location, final long length, final boolean fillWithZeros) {
		MappedByteBuffer mappedByteBuffer = null;
		try (FileChannel channel = FileChannel.open(location.toPath(), CREATE_NEW, READ, WRITE)) {
			mappedByteBuffer = channel.map(READ_WRITE, 0, length);
		} catch (final IOException ex) {
			LangUtil.rethrowUnchecked(ex);
		}
		if (fillWithZeros) {
			int pos = 0;
			final int capacity = mappedByteBuffer.capacity();
			while (pos < capacity) {
				mappedByteBuffer.put(pos, (byte)0);
				pos += BLOCK_SIZE;
			}
		}
		return mappedByteBuffer;
	}
	
	public static void checkFileExists(final File file, final String name) {
		if (!file.exists()) {
			final String msg = "missing file for " + name + ": " + file.getAbsolutePath();
			throw new IllegalStateException(msg);
		}
	}
	
	public static void unmap(final MappedByteBuffer buffer) {
		BufferUtil.free(buffer);
	}
	
	public static void unmap(final ByteBuffer buffer) {
		if (buffer instanceof MappedByteBuffer) {
			unmap((MappedByteBuffer)buffer);
		}
	}
	
	public static String tmpDirName() {
		String tmpDirName = System.getProperty("java.io.tmp");
		if (!tmpDirName.endsWith(File.separator)) {
			tmpDirName += File.separator;
		}
		return tmpDirName;
	}
	
	public static void removeTrailingSlashes(final StringBuilder builder) {
		while (builder.length() > 1) {
			final int lastCharIndex = builder.length() - 1;
			final char c = builder.charAt(lastCharIndex);
			if ('/' == c || '\\' == c) {
				builder.setLength(lastCharIndex);
			} else {
				break;
			}
		}
	}
	
	private static String getFileMode(final FileChannel.MapMode mode) {
		return mode == READ_ONLY ? "r" : "rw";
	}
}
