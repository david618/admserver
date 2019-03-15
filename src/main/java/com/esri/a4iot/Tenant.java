package com.esri.a4iot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class Tenant
 */
public class Tenant extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private String scriptFolder;
	
	private String installAks;
	private String installDatastore;
	private String installGateway;
	private String installIngressContour;
	private String installIngressLetsEntrypt;
	private String installA4iot;
	
	private String deleteScript;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Tenant() {
		super();

		try {
			// Load main.properties
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			InputStream input = classLoader.getResourceAsStream("main.properties");

			Properties properties = new Properties();

			properties.load(input);
			scriptFolder = properties.getProperty("scriptFolder");

			// Load Class Specific Properties
			String className = getClass().getSimpleName();

			input = classLoader.getResourceAsStream(className + ".properties");
			properties.load(input);
			
			installAks=properties.getProperty("installAks");
			installDatastore=properties.getProperty("installDatastore");
			installGateway=properties.getProperty("installGateway");
			installIngressContour=properties.getProperty("installIngressContour");
			installIngressLetsEntrypt=properties.getProperty("installIngressLetsEntrypt");
			installA4iot=properties.getProperty("installA4iot");			
			
			deleteScript = properties.getProperty("deleteScript");


		} catch (IOException e) {
			// Add to logs instead of StackTrace
			e.printStackTrace();
		}

	}
	
	private boolean executeScript(String cmd, PrintWriter pw) throws InterruptedException, IOException {
		// Log the command
		System.out.println(cmd);

		ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
		pb.directory(new File(scriptFolder));
		Process process = pb.start();
		int exitCode = process.waitFor();

		// Output the exit Code and the Output from Bash Script
		System.out.println("Command exited with code: " + exitCode);
		String msg = Support.output(process.getInputStream());
		System.out.println("Command output: \n" + msg);

		
		pw.write(exitCode + "\n");
		pw.write(msg);

		if (exitCode == 0) {
			return true;
		}
		
		return false;
		
	}
	

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		/*
		 * Create a new Tenant Input json with parameters
		 */

		try {

			// Check content type
			String ct = request.getContentType();
			if (ct == null || !ct.equalsIgnoreCase("application/json")) {
				// return error
				System.out.println("You must post JSON. Content-Type was " + request.getContentType());
				return;
			}

			StringBuffer jb = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null)
					jb.append(line);
			} catch (IOException e) {
				// return error
				System.out.println("Failed to read posted data");
				return;
			}

			if (jb.toString().isEmpty()) {
				// return error
				System.out.println("You must post json");
				return;
			}

			JSONObject jp = new JSONObject();
			// Parse Json
			try {
				jp = new JSONObject(jb.toString());
			} catch (JSONException e) {
				// return error
				System.out.println("JSON posted is invalid");
			}

			// Read variables from JSON
			String tenantID = jp.getString("tenant-id");
			// Validate tenantID

			String a4iotBuildNum = jp.getString("a4iot-build-num");
			// Validate build number (must be valid build number)

			String azureLocation = "westus2";
			String key = "azure-location";
			if (jp.has(key)) {
				azureLocation = jp.getString(key);
			}
			// validate input (e.g. must be eastus, westus, eastus2, westus2)

			Integer azureCoresPerInstance = 16;
			key = "azure-cores-per-instance";
			if (jp.has(key)) {
				azureCoresPerInstance = jp.getInt(key);
			}
			// validate input (e.g. must be 8,16,32,or 64

			Integer azureNumInstance = 6;
			key = "azure-num-instances";
			if (jp.has(key)) {
				azureNumInstance = jp.getInt(key);
			}
			// validate number instance (e.g. must be less than 100)

			PrintWriter pw = response.getWriter();

			
			// Install AKS			
			String cmd = scriptFolder + "/" + installAks + " " + tenantID + " " + azureLocation + " " + azureCoresPerInstance + " " + azureNumInstance;
			if (!executeScript(cmd, pw)) return;
			
			// Install Datastore
			cmd = scriptFolder + "/" + installDatastore + " " + tenantID;
			if (!executeScript(cmd, pw)) return;

			// Install Gateway
			cmd = scriptFolder + "/" + installGateway + " " + tenantID;
			if (!executeScript(cmd, pw)) return;

			// Install Contour
			cmd = scriptFolder + "/" + installIngressContour + " " + tenantID;
			if (!executeScript(cmd, pw)) return;
			
			// Install LetsEncrypt
			cmd = scriptFolder + "/" + installIngressLetsEntrypt + " " + tenantID;
			if (!executeScript(cmd, pw)) return;
			
			// Install A4IOT
			cmd = scriptFolder + "/" + installA4iot + " " + tenantID + " " + a4iotBuildNum;
			if (!executeScript(cmd, pw)) return;
			
			
			
			
		} catch (

		InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @see HttpServlet#doPut(HttpServletRequest, HttpServletResponse)
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doDelete(HttpServletRequest, HttpServletResponse)
	 */
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		/*
		 * Delete a Tenant Require confirmation Input is parameter tenantID
		 */

		try {

			String tenantID = request.getParameter("t");

			if (tenantID == null) {
				tenantID = request.getParameter("tenant-id");
			}

			if (tenantID == null) {
				// return an error as JSON
				System.out.println("You must specify a tenant-id");
				return;
			}

			// TODO: Check if tenantId is valid

			// TODO: Check if tenantId exists

			String cmd = scriptFolder + "/" + deleteScript + " " + tenantID;
			System.out.println(cmd);

			ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
			pb.directory(new File(scriptFolder));
			Process process = pb.start();
			int exitCode = process.waitFor();

			System.out.println("Command exited with code: " + exitCode);
			System.out.println("Command output: " + Support.output(process.getInputStream()));

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
