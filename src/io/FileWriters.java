package io;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 
 * @author Magnus Fransson
 * @version 1.0
 */
public class FileWriters {
	
	FileWriter w;
	
	public FileWriters(String path, String fileName) throws IOException {
		
		this.w = new FileWriter(path + fileName);
		
	}
	
	/**
	 * Write an already separated string to the open file. Appends new line automatically.
	 * @param row
	 * @throws IOException
	 */
	public void FileWritersAppendRow(String row) throws IOException {
		
		this.w.append(row);
		this.w.append(System.lineSeparator());
				
	}
	
	/**
	 * Flushes and closes the file stream.
	 */
	public void destroy() {
		try {
			this.w.flush();
			this.w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}