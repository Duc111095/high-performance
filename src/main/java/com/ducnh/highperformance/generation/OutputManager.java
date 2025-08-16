package com.ducnh.highperformance.generation;

import java.io.IOException;
import java.io.Writer;

import com.ducnh.highperformance.LangUtil;

public interface OutputManager {
	
	Writer createOutput(String name) throws IOException;
	
	default void withOutput(final String name, final ResourceConsumer<Writer> resourceConsumer) {
		try (Writer output = createOutput(name)) {
			resourceConsumer.accept(output);
		} catch (final IOException ex) {
			LangUtil.rethrowUnchecked(ex);
		}
	}
}
