package com.ducnh.highperformance.concurrent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

public final class SigInt {

	private static final Class<?> SIGNAL_HANDLER_CLASS;
	private static final Constructor<?> SIGNAL_CONSTRUCTOR;
	private static final Method HANDLE_METHOD;
	private static final Method RAISE_METHOD;
	
	static {
		try {
			final Class<?> signalClass = Class.forName("jdk.internal.misc.Signal");
			SIGNAL_HANDLER_CLASS = Class.forName("jdk.internal.misc.Signal$Handler");
			SIGNAL_CONSTRUCTOR = signalClass.getConstructor(String.class);
			HANDLE_METHOD = signalClass.getMethod("handle", signalClass, SIGNAL_HANDLER_CLASS);
			RAISE_METHOD = signalClass.getMethod("raise", signalClass);
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void register(final Runnable task) {
		register("INT", task);
	}
	
	static Object register(final String name, final Runnable task) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(task, "task");
		try {
			final Object signal = SIGNAL_CONSTRUCTOR.newInstance();
			final Object handler = Proxy.newProxyInstance(
					SIGNAL_HANDLER_CLASS.getClassLoader(), 
					new Class<?>[]{ SIGNAL_HANDLER_CLASS }, 
					(proxy, method, args) -> {
						if (SIGNAL_HANDLER_CLASS == method.getDeclaringClass()) {
							task.run();
						} else if (Object.class == method.getDeclaringClass()) {
							if (method.getName().equals("toString")) {
								return args[0].toString();
							}
						}
						return null;
					});
			
			return HANDLE_METHOD.invoke(null, signal, handler);
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	static Object raiseSignal(final String name) throws ReflectiveOperationException {
		return RAISE_METHOD.invoke(null, SIGNAL_CONSTRUCTOR.newInstance(name));
	}
	
	private SigInt() {
		
	}
}
