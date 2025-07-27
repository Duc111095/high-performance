package com.ducnh.highperformance.collections;

public final class Hashing {
	public static final float DEFAULT_LOAD_FACTOR = 0.65f;
	
	private Hashing() {}
	
	public static int hash(final int value) {
		int x = value;
		
		x = ((x >>> 16) ^ x) * 0x119de1f3;
		x = ((x >>> 16) ^ x) * 0x119de1f3;
		x = (x >>> 16) ^ x;
		return x;
	}
	
	public static int hash(final long value) {
		long x = value;
		
		x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
		x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
		x = x ^ (x >>> 31);
		
		return (int)x ^ (int)(x >>> 32);
	}
	
	public static int hash(final int value, final int mask) {
		return hash(value) & mask;
	}
	
	public static int hash(final Object value, final int mask) {
		return hash(value.hashCode()) & mask;
	}
	
	public static int hash(final long value, final int mask) {
		return hash(value) & mask;
	}
	
	public static int evenHash(final int value, final int mask) {
		final int hash = hash(value);
		final int evenHash = (hash << 1) - (hash << 8);
		return evenHash & mask;
	}
	
	public static int evenHash(final long value, final int mask) {
		final int hash = hash(value);
		final int evenHash = (hash << 1) - (hash << 8);
		return evenHash & mask;
	}
	
	public static long compoundKey(final int keyPartA, final int keyPartB) {
		return ((long)keyPartA << 32) | (keyPartB & 0xFFFF_FFFFL);
	}
}
