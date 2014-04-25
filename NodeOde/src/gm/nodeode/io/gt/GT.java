package gm.nodeode.io.gt;

import gm.debug.Blog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class GT {
	private static Map<String, String> cookies;
	
	public static void main(String[] args) throws Exception {
		File file = new File("creds.txt");
		if (file.exists()) {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String user = in.readLine();
			String pass = in.readLine();
			in.close();
			
			System.out.println("Attempting login...");
			System.out.println(login(user, pass));
			System.out.println(requestCourses("201408", "CS"));
		}
	}
	
	public static boolean login(String username, String password) {
		try {
			cookies = new CASAuthenticator(username, password).connect();
		} catch (Exception e) {
			Blog.log("Login Error: " + e);
			return false;
		}
		
		return true;
	}
	
	public static String requestPOST(String url, String ... parameters) {
		try {
			StringBuffer paramsBuffer = new StringBuffer();
			for (int i = 0; i < parameters.length; i+=2) {
				if (paramsBuffer.length() > 0)
					paramsBuffer.append("&");
				paramsBuffer.append(URLEncoder.encode(parameters[i+0], "UTF-8"));
				paramsBuffer.append("=");
				paramsBuffer.append(URLEncoder.encode(parameters[i+1], "UTF-8"));
			}
			String params = paramsBuffer.toString();
			
			URL u = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Type", "text/plain");
			conn.setRequestProperty("Content-Length", String.valueOf(params.getBytes().length));
			for (String key : cookies.keySet()) {
				conn.setRequestProperty(
						URLEncoder.encode(key, "UTF-8"), 
						URLEncoder.encode(cookies.get(key), "UTF-8"));
			}
			
			
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes(params);
			out.flush();
			out.close();
//			
//			conn.disconnect();
			
			int code = conn.getResponseCode();
			
			StringBuffer resp = new StringBuffer();
			
			InputStream sin = conn.getInputStream();
			BufferedReader in = new BufferedReader(
					new InputStreamReader(sin));
			String line = null;
			while (true) {
				try { line = in.readLine(); }
				catch (Exception e) { break; }
				
				if (line == null) break;
				
				if (resp.length() > 0) {
					resp.append("\n");
				}
				resp.append(line);
			}
			in.close();
			
			conn.disconnect();
			
			return resp.toString();
		} catch (Exception e) {
			Blog.log("GT error: " + e);
			return null;
		}
	}


	public static String requestCourses(String year, String subject) {
		return requestPOST("https://oscar.gatech.edu/pls/bprod/bwckctlg.p_display_courses",
				"term_in", year, 
				"sel_subj", subject,
				"call_proc_in", "bwckctlg.p_disp_dyn_ctlg");
	}
	
}
