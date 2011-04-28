package com.ibdknox.socket_io_netty;

public class ShutdownHook extends java.lang.Thread {
	
	private INSIOHandler handler;

	public ShutdownHook(INSIOHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void run() {
		System.out.println("**Shutting down the NSIO server");
		handler.OnShutdown();
		System.out.println("**SHUTDOWN**");
	}
	

}
