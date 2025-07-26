package com.ducnh.highperformance;

import java.util.Collection;

public final class CloseHelper {
	private CloseHelper() {}
	
	public static void quietClose(final AutoCloseable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final Exception ignore) {}
		}
	}
	
	public static void quietCloseAll(final Collection<? extends AutoCloseable> closeables) {
		if (closeables != null) {
			for (final AutoCloseable closeable : closeables) {
				quietClose(closeable);
			}
		}
	}
	
	public static void close(final AutoCloseable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final Exception ex) {
				LangUtil.rethrowUnchecked(ex);
			}
		}
	}
	
	public static void closeAll(final Collection<? extends AutoCloseable> closeables) {
		if (closeables != null) {
			Exception error = null;
			for (final AutoCloseable closeable : closeables) {
				if (closeable != null) {
					try {
						closeable.close();
					} catch (final Exception ex) {
						if (error == null) {
							error = ex;
						} else {
							error.addSuppressed(ex);
						}
					}
				}
			}
			if (error != null) {
				LangUtil.rethrowUnchecked(error);
			}
		}
	}
	
	public static void closeAll(final AutoCloseable... closeables) {
		if (closeables != null) {
			Exception error = null;
			for (final AutoCloseable closeable : closeables) {
				try {
					closeable.close();
				} catch (final Exception ex) {
					if (error == null) {
						error = ex;
					} else {
						error.addSuppressed(ex);
					}
				}
			}
			if (error != null) {
				LangUtil.rethrowUnchecked(error);
			}
		}
	}
	
	public static void close(final ErrorHandler errorHandler, final AutoCloseable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final Exception ex) {
				if (errorHandler == null) {
					final NullPointerException error = new NullPointerException("errorHandler is null");
					error.addSuppressed(ex);
					throw error;
				}
				errorHandler.onError(ex);
			}
		}
	}
	
	public static void closeAll(final ErrorHandler errorHandler, final Collection<? extends AutoCloseable> closeables) {
		if (closeables != null) {
			NullPointerException error = null;
			for (final AutoCloseable closeable : closeables) {
				if (closeable != null) {
					try {
						closeable.close();
					} catch (final Exception ex) {
						if (errorHandler == null) {
							if (error == null) {
								error = new NullPointerException("errorHandler is null");
							}
							error.addSuppressed(ex);
						} else {
							errorHandler.onError(ex);
						}
					}
				}
			}
			if (error != null) {
				LangUtil.rethrowUnchecked(error);
			}
		}
	}
	
	public static void closeAll(final ErrorHandler errorHandler, final AutoCloseable... closeables) {
		if (closeables != null) {
			NullPointerException error = null;
			for (final AutoCloseable closeable : closeables) {
				if (closeable != null) {
					try {
						closeable.close();
					} catch (final Exception ex) {
						if (errorHandler == null) {
							if (error == null) {
								error = new NullPointerException("errorHandler is null");
							}
							error.addSuppressed(ex);
						} else {
							errorHandler.onError(ex);
						}
					}
				}
			}
			if (error != null) {
				LangUtil.rethrowUnchecked(error);
			}
		}
	}
}
