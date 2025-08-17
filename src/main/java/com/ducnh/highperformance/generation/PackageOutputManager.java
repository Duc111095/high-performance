package com.ducnh.highperformance.generation;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

import static java.io.File.separatorChar;

public class PackageOutputManager implements OutputManager{
	private final File outputDir;
	
	public PackageOutputManager(final String baseDirName, final String packageName) {
		Objects.requireNonNull(baseDirName, "baseDirName");
		Objects.requireNonNull(packageName, "packageName");
		
		final char lastChar = baseDirName.charAt(baseDirName.length() - 1);
		final String dirName = lastChar == separatorChar ? baseDirName : baseDirName + separatorChar;
		final String dirNamePlusPackage = dirName + packageName.replace('.', separatorChar);
		
		outputDir = new File(dirNamePlusPackage);
		if (!outputDir.exists()) {
			if (!outputDir.mkdirs()) {
				throw new IllegalStateException("Unable to create directory: " + dirNamePlusPackage);
			}
		}
	}
	
	public Writer createOutput(final String name) throws IOException {
		final File targetFile = new File(outputDir, name + ".java");
		return Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8);
	}
}
