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

package edu.wright.cs.jfiles.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * The main class of the JFiles server application.
 * 
 * @author Roberto C. Sánchez &lt;roberto.sanchez@wright.edu&gt;
 *
 */
public class JFilesServer implements Runnable {

	static final Logger logger = LogManager.getLogger(JFilesServer.class);
	private static final int PORT = 9786;
	private final ServerSocket serverSocket;
	private static final String UTF_8 = "UTF-8";
	
	/**
	 * The main entry point to the program.
	 * 
<<<<<<< HEAD
	 * @throws IOException
	 *             If there is a problem binding to the socket
=======
	 * @param args
	 *            The command-line arguments
>>>>>>> refs/remotes/origin/team2
	 */
<<<<<<< HEAD
	public JFilesServer() throws IOException {
		serverSocket = new ServerSocket(PORT);
	}

	@Override
	public void run() {
		String dir = System.getProperty("user.dir");
		try (Socket server = serverSocket.accept()) {
			System.out.println("Received connection from" + server.getRemoteSocketAddress());
			InputStreamReader isr = new InputStreamReader(server.getInputStream(), UTF_8);
			BufferedReader in = new BufferedReader(isr);
			String cmd;
			while (null != (cmd = in.readLine())) {
				if ("".equals(cmd)) {
					break;
				}
				OutputStreamWriter osw = new OutputStreamWriter(server.getOutputStream(), UTF_8);

				BufferedWriter out = new BufferedWriter(osw);
				String[] baseCommand = cmd.split(" ");
				if ("LIST".equalsIgnoreCase(baseCommand[0])) {
					try (DirectoryStream<Path> directoryStream = Files
							.newDirectoryStream(Paths.get(dir))) {
						for (Path path : directoryStream) {
							out.write(path.toString() + "\n");
						}
					}

				}
				// start Search block
				if ("FIND".equalsIgnoreCase(baseCommand[0])) {

					try (DirectoryStream<Path> directoryStream = Files
							.newDirectoryStream(Paths.get(dir))) {
						for (Path path : directoryStream) {
							// out.write(path.toString() + "\n");
							if (path.toString().contains(baseCommand[1])) {
								out.write(path.toString() + "\n");
							}

						}
					}

				} else { // End search block
					out.write("ERROR: Unknown command!\n");
				}
				out.flush();
			}

=======
	public static void main(String[] args) {
		try {
			init();
			logger.info("Starting the server");
			JFilesServer jf = new JFilesServer();
			Thread thread = new Thread(jf);
			thread.start();
>>>>>>> refs/remotes/origin/team2
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Handles allocating resources needed for the server.
	 * 
<<<<<<< HEAD
	 * @param args
	 *            The command-line arguments
=======
	 * @throws IOException
	 *             If there is a problem binding to the socket
>>>>>>> refs/remotes/origin/team2
	 */
	public JFilesServer() throws IOException {
		serverSocket = new ServerSocket(PORT);
	}
	
	/**
	 * Handles any/all configuration work needed for the server to run.
	 */
	private static void init(){
		
	}

	@Override
	public void run() {
		String dir = System.getProperty("user.dir");
		try (Socket server = serverSocket.accept()) {
			// System.out.println("Received connection from"
			// + server.getRemoteSocketAddress());
			logger.info("Received connection from " + server.getRemoteSocketAddress());
			InputStreamReader isr = new InputStreamReader(server.getInputStream(), UTF_8);
			BufferedReader in = new BufferedReader(isr);
			String cmd = in.readLine();
			OutputStreamWriter osw = new OutputStreamWriter(server.getOutputStream(), UTF_8);
			BufferedWriter out = new BufferedWriter(osw);
			if ("LIST".equalsIgnoreCase(cmd)) {
				try (DirectoryStream<Path> directoryStream
						= Files.newDirectoryStream(Paths.get(dir))) {
					for (Path path : directoryStream) {
						out.write(path.toString() + "\n");
					}
				}
			} else {
				// out.write("ERROR: Unknown command!\n");
				logger.error("Unknown command");
			}
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error("Some error occured", e);
		}
	}

}
