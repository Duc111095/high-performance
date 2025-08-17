package com.ducnh.highperformance.generation;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class StringWriterOutputManager {
	private String packageName;
	private String initialPackageName;
	private final HashMap<String, StringWriter> sourceFileByName = new HashMap<>();
	
	public StringWriterOutputManager() {
		
	}
	
	public Writer createOutput(final String name) {
		final StringWriter stringWriter = new StringWriter();
		sourceFileByName.put(packageName + "." + name, stringWriter);
		
		return new FilterWriter(stringWriter) {
			public void close() throws IOException {
				super.close();
				
				if (null != initialPackageName) {
					packageName = initialPackageName;
				}
			}
		};
	}
	
	public void setPackageName(final String packageName) {
		this.packageName = packageName;
		if (null == initialPackageName) {
			initialPackageName = packageName;
		}
	}
	
	public CharSequence getSource(final String name) {
		final StringWriter stringWriter = sourceFileByName.get(name);
		if (null == stringWriter) {
			throw new IllegalArgumentException("unknown source file name: " + name);
		}
		
		return stringWriter.toString();
	}
	
	public Map<String, CharSequence> getSources() {
		final HashMap<String, CharSequence> sources = new HashMap<>();
		for (final Map.Entry<String, StringWriter> entry : sourceFileByName.entrySet()) {
			sources.put(entry.getKey(), entry.getValue().toString());
		}
		
		return sources;
	}
	
	public void clear() {
		initialPackageName = null;
		packageName = "";
		sourceFileByName.clear();
	}
}
