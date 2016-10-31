/*
 * Copyright (C) 2016 - WSU CEG3120 Students
 * 
 * Roberto C. Sánchez <roberto.sanchez@wright.edu>
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

package edu.wright.cs.jfiles.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * The main class of the JFiles client application.
 * 
 * @author Roberto C. Sánchez &lt;roberto.sanchez@wright.edu&gt;
 *
 */
public class JFilesClient implements Runnable {

	static final Logger logger = LogManager.getLogger(JFilesClient.class);
	private static String host;
	private static int port;
	private static final String UTF_8 = "UTF-8";

	/**
	 * Handles allocating resources needed for the client.
	 * 
	 * @throws IOException
	 *             If there is a problem binding to the socket
	 */
	public JFilesClient() {
	}

	/**
	 * Handles allocating resources needed for the server.
	 * 
	 * @throws IOException
	 *             If there is a problem binding to the socket
	 */
	private static void init() throws IOException {
		//Array of strings containing possible paths to check for config files
		ArrayList<String> configPaths = new ArrayList<String>();
		configPaths.add("/usr/local/etc/jfiles/serverConfig.xml");
		configPaths.add("/usr/local/etc/jfiles/serverConfig.xml");
		configPaths.add("/opt/etc/jfiles/serverConfig.xml");
		configPaths.add("/etc/jfiles/serverConfig.xml");
		configPaths.add(System.getProperty("user.home") + "/jfiles/serverConfig.xml");
		configPaths.add(System.getProperty("user.home") + "/.jfiles/serverConfig.xml");
		
		FileInputStream fis = null;
		File config = null;
				
		//Checking location(s) for the config file);
		for (int i = 0; i < configPaths.size(); i++) {
			if (new File(configPaths.get(i)).exists()) {
				config = new File(configPaths.get(i));
				break;
			}
		}
		
		Properties prop = new Properties();
		
		//Output location where the config file was found. Otherwise warn and use defaults.
		if (config == null) {		
			logger.info("No config file found. Using default values.");
		} else {
			logger.info("Config file found in " + config.getPath());
			//Read file
			try {
				//Reads xmlfile into prop object as key value pairs
				fis = new FileInputStream(config);
				prop.loadFromXML(fis);			
			} catch (IOException e) {
				logger.error("IOException occured when trying to access the server config", e);
			} finally {
				if (fis != null) {
					fis.close();
				}
			}
		}
	
		//Add setters here. First value is the key name and second is the default value.
		//Default values are require as they are used if the config file cannot be found OR if
		// the config file doesn't contain the key.
		port = Integer.parseInt(prop.getProperty("port","9786"));
		logger.info("Config set to port " + port);
		
		host = prop.getProperty("host","localhost");
		logger.info("Config set max threads to " + host);	
		
		// Create a Derby database in memory called jFiles
		String jfilesCachedb = "jdbc:derby:memory:jFiles;create=true";
		

		try {
			Connection conn = DriverManager.getConnection(jfilesCachedb);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        if (jfilesCachedb != null) {
        	logger.info("Database connection successful");
        }

	}
	
	
	@Override
	public void run() {
		try (Socket socket = new Socket(host, port)) {
			OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream(), UTF_8);
			BufferedWriter out = new BufferedWriter(osw);
			out.write("LIST\n");
			out.flush();
			InputStreamReader isr = new InputStreamReader(socket.getInputStream(), UTF_8);
			BufferedReader in = new BufferedReader(isr);
			String line;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * The main entry point to the program.
	 * 
	 * @param args
	 *            The command-line arguments
	 */
	public static void main(String[] args) {
		logger.info("Starting the client");
		try {
			init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JFilesClient jf = new JFilesClient();
		Thread thread = new Thread(jf);
		thread.start();
	}

}

