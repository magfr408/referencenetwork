package io;

import java.io.IOException;

public class Logger extends FileWriters {
	public Logger(String path, String fileName) throws IOException {
		super(path, fileName);
	}

	private void log(String message) {
		try {
			this.FileWritersAppendRow(message);
		} catch (IOException e) {
			/* Nothing */ }
	}

	public void log(String[] messages) {
		for (String message : messages) {
			this.log(message);
		}
	}
}