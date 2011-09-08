package com.sleaker.logparser;

import java.io.File;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class LogParser extends JavaPlugin {

	private String plugName;
	public static Logger log = Logger.getLogger("Minecraft");
	private Queue<String> logQueue = new ConcurrentLinkedQueue<String>();
	protected static final String logDir = "log-archive" + File.separator;
	LogThread logThread = new LogThread(logQueue);
	MoveWrongLogHandler logHandler;

	@Override
	public void onDisable() {	    
		//Shutdown our logging thread
		synchronized(logThread) {
			try {
				logThread.writeQueue();
				logThread.run = false;
				logThread.notify();
				logThread.join();
			} catch (InterruptedException e) {
				//don't care if we can't stop
			}
		}
		//Cleanup our handler
		log.removeHandler(logHandler);
		logHandler.close();

		log.info(plugName + " disabled!");
	}

	@Override
	public void onEnable() {
		plugName = "[" + this.getDescription().getName() + "]";

		File baseDir = new File(logDir);
		if (!baseDir.exists()) {
			log.info(plugName + " - Logging directory not found, creating at <server_dir>/log-archive");
			baseDir.mkdir();
		}
		//Register our log handler to intercept the moved wrongly messages.
		logHandler = new MoveWrongLogHandler();
		log.addHandler(logHandler);

		logThread.start();
		log.info(plugName + " by Sleaker v" + this.getDescription().getVersion() + " enabled!");
	}	

	private class MoveWrongLogHandler extends Handler {

		@Override
		public void publish(LogRecord record) {
			String s = record.getMessage();
			if (s.contains("moved wrongly") || s.contains("logged in with")  || s.contains("lost connection")
					|| s.contains("Got position") || s.contains("Expected ") || s.contains("used command")) {
				
				logQueue.add(getDateTime() + "  " + s);
			}
		}

		@Override
		public void flush() {

		}

		@Override
		public void close() throws SecurityException {

		}

		private String getDateTime() {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss| ");
			return dateFormat.format(new Date());
		}
	}
}
