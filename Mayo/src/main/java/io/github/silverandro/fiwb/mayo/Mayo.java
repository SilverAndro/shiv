package io.github.silverandro.fiwb.mayo;

import java.lang.instrument.Instrumentation;

public class Mayo {
	public static Instrumentation instrument;

	public static void agentmain(String agentArgs, Instrumentation instrumentation) {
		System.out.println("Mayo is in agentmain! " + instrumentation);
		instrument = instrumentation;
	}

	static {
		System.out.println("Init mayo! " + Mayo.class.getClassLoader() + " " + StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getName());
	}
}
