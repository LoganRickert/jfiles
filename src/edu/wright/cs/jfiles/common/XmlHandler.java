/*
 * Copyright (C) 2016 - WSU CEG3120 Students
 * 
 * Brian Denlinger <brian.denlinger1@gmail.com>
 * 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package edu.wright.cs.jfiles.common;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This class encapsulates the XMLStruct class in an ArrayList and provides methods to read and 
 * write to data streams.
 * @author brian
 *
 */
public class XmlHandler {
	
	private String currentPath;
	private ArrayList<FileStruct> arrlist;
	private static transient XStream xstream = new XStream();
	
	//Static init block to configure XStream
	static {
		xstream.alias("fileObject", FileStruct.class);
		xstream.alias("root", XmlHandler.class);
		xstream.aliasAttribute(XmlHandler.class, "arrlist", "filesystem");
		xstream.aliasAttribute(FileStruct.class, "attrList", "attributeList");
		xstream.useAttributeFor(FileStruct.class, "type");
		xstream.omitField(XStream.class, "xstream");
	}
	
	/**
	 * Zero argument constructor.
	 */
	public XmlHandler() {
	}
	
	/**
	 * Constructor for sending XML.
	 * @param path path that you want to XMLify
	 * @throws IOException If Path object is inaccessible 
	 */
	public XmlHandler(String path) throws IOException {
		this.currentPath = path;
		arrlist = new ArrayList<FileStruct>();
		populateArray();	
	}
	
	/**
	 * Constructor for sending XML.
	 * @param path that you want to XMLify
	 * @param str stream to send XML to
	 * @throws IOException  if path object is inaccesible
	 */
	public XmlHandler(String path, OutputStreamWriter str) throws IOException {
		this.currentPath = path;
		arrlist = new ArrayList<FileStruct>();
		populateArray();
		sendXml(str);
	}
	
	/**
	 * Helper method to populate the ArrayList.
	 * @throws IOException If Path object is inaccessible
	 */
	private void populateArray() throws IOException {
		String ts = currentPath.substring(currentPath.length() - 1);
		
		//If passed a directory with a trailing slash in the path only list the directory
		//Else list the content of the directory
		if (Files.isDirectory(Paths.get(currentPath)) 
				&& ts.equals(System.getProperty("file.separator"))) {
			arrlist.add(new FileStruct(Paths.get(currentPath)));
		} else {
			//Tried this with an iterator and it blew up
			File[] temp = new File(currentPath).listFiles();
			
			if (temp == null) {
				return;
			}
			for (int i = 0; i < temp.length; i++) {
				arrlist.add(new FileStruct(temp[i].toPath()));	
			}
		}
	}
	
	/**
	 * Method to serialize an object and write XML to an output stream.
	 * @param osw OutputStreamWriter to write to
	 * @throws IOException If output stream can't be read from
	 */
	public void sendXml(OutputStreamWriter osw) throws IOException {
		xstream.toXML(this, osw);
	}
	
	/**
	 * Method to read XML and deserialize to an object.
	 * @param isr InputStreamReader to read from
	 * @return reconstructed object
	 */
	public ArrayList<FileStruct> readXml(InputStreamReader isr) {
		XmlHandler temp = (XmlHandler) xstream.fromXML(isr);
		return temp.arrlist;	
	}
}
