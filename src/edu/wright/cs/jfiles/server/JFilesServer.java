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

import edu.wright.cs.jfiles.core.CommandExecutor;
import edu.wright.cs.jfiles.core.CommandParser;
import edu.wright.cs.jfiles.core.Environment;
import edu.wright.cs.jfiles.core.ExecutablePath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * The main class of the JFiles server application.
 * 
 * @author Roberto C. Sánchez &lt;roberto.sanchez@wright.edu&gt;
 *
 */
public class JFilesServer implements Runnable {

	static final Logger logger = LogManager.getLogger(JFilesServer.class);
	private static int PORT;
	private static int MAXTHREADS;
	private final ServerSocket serverSocket;
	private static final String UTF_8 = "UTF-8";

	/**
	 * Handles allocating resources needed for the server.
	 * 
	 * @throws IOException
	 *             If there is a problem binding to the socket
	 */

	private static void init() throws IOException {
		Properties prop = new Properties();
		FileInputStream fis = null;
		File config = null;

		// Array of strings containing possible paths to check for config files
		String[] configPaths = { "$HOME/.jfiles/serverConfig.xml",
				"/usr/local/etc/jfiles/serverConfig.xml", "/opt/etc/jfiles/serverConfig.xml",
				"/etc/jfiles/serverConfig.xml", "%PROGRAMFILES%/jFiles/etc/serverConfig.xml",
				"%APPDATA%/jFiles/etc/serverConfig.xml" };

		// Checking location(s) for the config file);
		for (int i = 0; i < configPaths.length; i++) {
			if (new File(configPaths[i]).exists()) {
				config = new File(configPaths[i]);
				break;
			}
		}

		// Output location where the config file was found. Otherwise warn and
		// use defaults.
		if (config == null) {
			logger.info("No config file found. Using default values.");
		} else {
			logger.info("Config file found in " + config.getPath());
			// Read file
			try {
				// Reads xmlfile into prop object as key value pairs
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

		// Add setters here. First value is the key name and second is the
		// default value.
		// Default values are require as they are used if the config file cannot
		// be found OR if
		// the config file doesn't contain the key.
		PORT = Integer.parseInt(prop.getProperty("Port", "9786"));
		logger.info("Config set to port " + PORT);

		MAXTHREADS = Integer.parseInt(prop.getProperty("maxThreads", "10"));
		logger.info("Config set max threads to " + MAXTHREADS);
	}

	/**
	 * This is a Javadoc comment to statisfy Checkstyle.
	 * 
	 * @throws IOException
	 *             When bad things happen
	 */
	public JFilesServer() throws IOException {
		serverSocket = new ServerSocket(PORT);
	}

	/**
	 * Creates an XML file.
	 * 
	 * @throws TransformerFactoryConfigurationError
	 *             error in configuration
	 * @throws TransformerException
	 *             error in configuration
	 */
	private void createXml() throws TransformerFactoryConfigurationError, TransformerException {
		Document doc = null;
		try {
			// Create new XML document
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			doc = builder.newDocument();

			// Add elements to new document
			Element root = doc.createElement("fileSystem");
			doc.appendChild(root);
			Node dir = createNode(doc, "directory");
			dir.appendChild(createNode(doc, "file"));
			root.appendChild(dir);

			// Output XML to console
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult console = new StreamResult(System.out);
			transformer.transform(source, console);

		} catch (ParserConfigurationException e) {
			logger.error("An error occurred while configuring the parser", e);
		} catch (TransformerConfigurationException e) {
			logger.error("An error occurred while configuring the transformer", e);
		} catch (TransformerFactoryConfigurationError e) {
			logger.error("An error occurred while configuring the transformer factory", e);
		}
	}

	/**
	 * Create an xml node.
	 * 
	 * @param doc
	 *            document to create node for
	 * @param name
	 *            name of node that should be created
	 * @return returns a Node element
	 */
	private Node createNode(Document doc, String name) {
		Element node = doc.createElement(name);
		return node;
	}

	@Override
	public void run() {
		String dir = System.getProperty("user.dir");
		File history = new File("SearchHistory.txt");

		Locale.setDefault(new Locale("English"));
		try (Socket server = serverSocket.accept()) {
			logger.info("Received connection from" + server.getRemoteSocketAddress());
			InputStreamReader isr = new InputStreamReader(server.getInputStream(), UTF_8);
			BufferedReader in = new BufferedReader(isr);
			FileWriter hstWrt = new FileWriter(history); // history writer

			String cmd;
			OutputStreamWriter osw = new OutputStreamWriter(server.getOutputStream(), UTF_8);

			// ExecutablePath executablePath = new ExecutablePath();
			// Environment environment = new Environment();

			// CommandParser parser = new CommandParser(environment);
			// CommandExecutor executor = new CommandExecutor(executablePath,
			// environment);

			BufferedWriter out = new BufferedWriter(osw);

			while (null != (cmd = in.readLine()) && true) {
				if ("".equals(cmd)) {
					break;
				}
				out.write("Prompt :> ");
				// command example
				// ========================================================================
				// out.write("Prompt :> ");
				// CommandLine commandLine = parser.parse(cmd);
				//
				// try {
				// ExecutionResult result = executor.executeCommand(commandLine,
				// out);
				// if (result.isExitShell()) {
				// break;
				// }
				// } catch (CommandNotFoundException e) {
				// out.write(" " + e.getMessage() + ": command not found\n");
				// }
				// ========================================================================

				String[] baseCommand = cmd.split(" ");

				switch (baseCommand[0].toUpperCase(Locale.ENGLISH)) {
				case "LIST":
					listCmd(dir, out);
					break;
				case "FIND":
					while (!(isValid(baseCommand))) { //loop used to make sure command is used right
						out.write("Invalid Entry, Please re-enter what you want to find" + "\n");
						cmd = in.readLine();
						//baseCommand = cmd.split(" ");  this is commented until a bug is fixed #57
					}
					findCmd(dir, out, baseCommand[1].toLowerCase(Locale.ENGLISH));
					break;
				case "FINDR":
					while (!(isValid(baseCommand))) {
						out.write("Invalid Entry, Please re-enter what you want to find" + "\n");
						cmd = in.readLine();
						//baseCommand = cmd.split(" ");
					}
					recursiveFindCmd(dir, out, baseCommand[1].toLowerCase(Locale.ENGLISH));
					break;
				case "EXIT":
					try {
						out.close();
						in.close();
						serverSocket.close();
					} catch (IOException ex) {
						System.out.println("Error closing the socket and streams");

					}
					break;
				default:
					logger.info("Hit default switch." + System.lineSeparator());
					break;
				}
				if (history.exists()) {
					hstWrt.append(baseCommand[1] + "\n");
				} else {
					hstWrt.write(baseCommand[1] + "\n");
				}
			}
			out.flush();
			hstWrt.close();

		} catch (IOException e) {
			// TODO AUto-generated catch block
			// e.printStackTrace();
			logger.error("Some error occured", e);
		}
	}
	
	/**
	 * Check to see if the command that requires input is valid.
	 * 
	 * @command is the command entered from user to decide what to do.
	 * 
	 */
	boolean isValid(String[] command) {
		if (command.length <= 1) { //used for handling invalid error
			logger.error("Invalid Input, nothing to find");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * List Command function. Method for the list command.
	 * 
	 * @throws IOException
	 *             If there is a problem binding to the socket
	 */
	private void listCmd(String dir, BufferedWriter out) {
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(dir))) {
			for (Path path : directoryStream) {
				out.write(path.toString() + System.getProperty("line.separator"));
			}
		} catch (IOException e) {
			// TODO AUto-generated catch block
			// e.printStackTrace();
			logger.error("Some error occured", e);
		}
	}

	/**
	 * Find Command function. Method for the find command. Writes results found
	 * within current directory. Search supports glob patterns
	 * 
	 * @throws IOException
	 *             If there is a problem binding to the socket
	 */
	private void findCmd(String dir, BufferedWriter out, String searchTerm) {
		int findCount = 0;
		try (DirectoryStream<Path> directoryStream =
				Files.newDirectoryStream(Paths.get(dir), searchTerm)) {
			for (Path path : directoryStream) {
				// if
				// (path.toString().toLowerCase(Locale.ENGLISH).contains(searchTerm))
				// {
				out.write(path.toString() + "\n");
				findCount++;
				// }
			}
			System.out.println("Found " + findCount + " file(s) in " + dir + " that contains \""
					+ searchTerm + "\"\n");
		} catch (IOException e) {
			// TODO AUto-generated catch block
			// e.printStackTrace();
			logger.error("Some error occured", e);
		}
	}

	/**
	 * Recursive find Command function. Method for the recursive option of the
	 * find command. Calls itself if a child directory is found, otherwise calls
	 * findCmd to get results from current directory.
	 * 
	 * @throws IOException
	 *             If there is a problem binding to the socket
	 */
	private void recursiveFindCmd(String dir, BufferedWriter out, String searchTerm) {
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(dir))) {
			for (Path path : directoryStream) {
				if (path.toFile().isDirectory()) {
					recursiveFindCmd(path.toString(), out, searchTerm);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error("Some error occured", e);
		}
		findCmd(dir, out, searchTerm);
	}

	/**
	 * The main entry point to the program.
	 * 
	 * @throws IOException
	 *             If there is a problem binding to the socket
	 */
	public static void main(String[] args) {
		try {
			init();
			logger.info("Starting the server");
			JFilesServer jf = new JFilesServer();
			try {
				jf.createXml();
			} catch (TransformerFactoryConfigurationError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Thread thread = new Thread(jf);
			// thread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
