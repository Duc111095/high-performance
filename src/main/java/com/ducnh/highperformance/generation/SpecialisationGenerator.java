package com.ducnh.highperformance.generation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class SpecialisationGenerator {
	private static final String COLLECTIONS_PACKAGE = "com/ducnh/highperformance/collections";
	private static final String SRC_DIR = "src/main/java";
	private static final String DST_DIR = "build/generated-src";
	private static final String SUFFIX = ".java";
	
	private static final List<Substitution> SUBSTITUTIONS = Collections.singletonList(
			new Substitution("long", "Long", "Long"));
	
	public SpecialisationGenerator() {
		
	}
	
	public static void main(String[] args) throws IOException {
		specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntIntConsumer", SRC_DIR, DST_DIR);
		specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntIntFunction", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntIntPredicate", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntObjConsumer", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntObjPredicate", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntObjectToObjectFunction", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "ObjectIntToIntFunction", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "ObjIntConsumer", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "ObjIntPredicate", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntArrayList", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntArrayQueue", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2IntHashMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2IntCounterMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntHashSet", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "IntLruCache", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2ObjectCache", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2ObjectHashMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Int2NullableObjectHashMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Object2IntHashMap", SRC_DIR, DST_DIR);
        specialise(SUBSTITUTIONS, COLLECTIONS_PACKAGE, "Object2IntCounterMap", SRC_DIR, DST_DIR);
	}
	
	public static void specialise(
		final List<Substitution> substitutions,
		final String packageName,
		final String srcClassName,
		final String srcDirName,
		final String dstDirName) 
		throws IOException 
	{
		final Path inputPath = Paths.get(srcDirName, packageName, srcClassName + SUFFIX);
		final Path outputDirectory = Paths.get(dstDirName, packageName);
		Files.createDirectories(outputDirectory);
		
		final List<String> contents = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
		for (final Substitution substitution : substitutions) {
			final String substitutedFileName = substitution.substitute(srcClassName);
			final List<String> substitutedContents = contents
					.stream()
					.map(substitution::conditionalSubstitute)
					.collect(Collectors.toList());
			
			final Path outputPath = Paths.get(dstDirName, packageName, substitutedFileName + SUFFIX);
			
			Files.write(outputPath, substitutedContents, StandardCharsets.UTF_8);
		}
	}
	
	public static final class Substitution {
		private final String primitiveType;
		private final String boxedType;
		private final String className;
		
		private Substitution(final String primitiveType, final String boxedType, final String className) {
			this.primitiveType = primitiveType;
			this.boxedType = boxedType;
			this.className = className;
		}
		
		public String substitute(final String contents) {
			return contents
				.replace("int", primitiveType)
				.replace("Integer", boxedType)
				.replace("Int", className);
		}
		
		public String conditionalSubstitute(final String contents) {
			return 
				(contents.contains("interface") || contents.contains("Interface")) ? contents : substitute(contents);
		}
	}
}
