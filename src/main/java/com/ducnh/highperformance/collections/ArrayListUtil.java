package com.ducnh.highperformance.collections;

import java.util.ArrayList;

public class ArrayListUtil {
	private ArrayListUtil() {}
	
	public static <T> void fastUnorderedRemove(final ArrayList<T> list, final int index) {
		final int lastIndex = list.size() - 1;
		if (index != lastIndex) {
			list.set(index, list.remove(lastIndex));
		} else {
			list.remove(index);
		}
	}
	
	public static <T> void fastUnorderedRemove(final ArrayList<T> list, final int index, final int lastIndex) {
		if (lastIndex != index) {
			list.set(index, list.remove(lastIndex));
		} else {
			list.remove(index);
		}
	}
	
	public static <T> boolean fastUnorderedRemove(final ArrayList<T> list, final T e) {
		for (int i = 0, size = list.size(); i < size; i++) {
			if (e == list.get(i)) {
				fastUnorderedRemove(list, i, size - 1);
				return true;
			}
		}
		return false;
	}
}
