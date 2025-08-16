package com.ducnh.highperformance.generation;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class JavaClassObject extends SimpleJavaFileObject{
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

	public JavaClassObject(final String className, final Kind kind) {
		super(URI.create("string:///" + className.replace('.', '/') + kind.extension), kind);
	}
	
	public byte[] getBytes() {
		return baos.toByteArray();
	}
	
	public OutputStream openOutputStream() {
		return baos;
	}
	
	public Kind getKind() {
		return Kind.CLASS;
	}
}
