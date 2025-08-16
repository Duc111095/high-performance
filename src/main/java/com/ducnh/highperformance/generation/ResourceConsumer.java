package com.ducnh.highperformance.generation;

import java.io.IOException;

public interface ResourceConsumer<T> {
	void accept(T resource) throws IOException;
}
