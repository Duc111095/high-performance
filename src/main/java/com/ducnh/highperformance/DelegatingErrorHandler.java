package com.ducnh.highperformance;

public interface DelegatingErrorHandler extends ErrorHandler{
	void next(ErrorHandler errorHandler);
}
