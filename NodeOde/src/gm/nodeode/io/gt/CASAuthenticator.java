package gm.nodeode.io.gt;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.Connection.Response;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;

import gm.debug.Blog;

import java.io.IOException;
import java.util.Map;

/**
 * Code acquired from Nick Olive, modified
 * to work with GT's new system.
 * A class to login and obtain an authentication cookie from
 * GaTech CAS login system.
 */
public class CASAuthenticator {

	private static final String casURI = "https://login.gatech.edu/cas/";//"https://login.gatech.edu/cas/login";
	private static final String redirectURI = "";
	private String user, pass;

	public CASAuthenticator(String user, String pass) {
		this.user = user;
		this.pass = pass;
	}

	/**
	 * Connected to CAS authentication given a user name and password.
	 *
	 * @return A map of cookie name/value pairs needed to access CAS protected pages.
	 * 		   Returns null if authentication fails.
	 */
	public Map<String, String> connect() throws IOException {
		Map<String, String> cookies = null;

		Response loginPage = Jsoup.connect(getURI()).method(Method.GET).execute();
		Document doc = loginPage.parse();
		String randomKey = getRandomKey(doc);
		String jsession = jsessionID(doc);
		
		Blog.log("CAS", "jsessionID: \"" + String.valueOf(jsession) + "\"");
		
		//Make the final request to login and get the cookies
		Response submitPage = Jsoup
                .connect(getURI() + ";jsessionid=" + jsession)
                .data("username", user, "password", pass)
                .data("lt", randomKey) 
                .data("execution", getInput(doc, "execution"))
                .data("submit", getInput(doc, "submit"))
//                .data("reset", getInput(doc, "reset"))
                .cookie("JSESSIONID", jsession)
                .header("origin", "https://login.gatech.edu")
                .header("referer", "https://login.gatech.edu/cas/")
//                .data("warn", getInput(doc, "warn"))
                .data("_eventId", "submit")
                .cookies(loginPage.cookies())
                .method(Method.POST)
                .execute();
		cookies = submitPage.cookies();

		Blog.log("CAS", "status: " + submitPage.statusCode() + " - " + submitPage.statusMessage());
		Blog.log("CAS", "randkey: " + randomKey);

		Document response = submitPage.parse();
		Element error = response.getElementById("login_error");
		if (error != null) {
			String errorText = error.text();
			if (errorText.trim().length() > 0) {
				throw new RuntimeException(errorText.trim());
			}
		}
		
		Blog.log("CAS", "no. cookies = " + cookies.size());
		for (String s : cookies.keySet()) {
			Blog.log("CAS", "cookie[" + s + "] = " + cookies.get(s));
		}

		//Verify if a valid CAS cookie was received
		if ( !cookies.containsKey("CASTGC") ) {
			throw new IOException("Did not receive CASTGC cookie.");
		}

		return cookies;
	}


	private String getInput(Document doc, String name) {
		String inp = doc.select("input[name=" + name + "]").first().attr("value");
		Blog.log("CAS", "input[" + name + "] = " + inp);
		return inp;
	}
	
	private String jsessionID(Document doc) {
		String action = doc.select("form[id=fm1]").first().attr("action");
		int semi = action.indexOf("=");
		if (semi < 0) {
			return null;
		}
		return action.substring(semi+1);
	}
	
	/**
	 * Find the value of the hidden input "lt".
	 * This value is a random key and must be forwarded to POST
	 * when entering a user name and password.
	 * 
	 * @param doc The login page document
	 * @return The randomized key
	 */
	private String getRandomKey(Document doc) {
		return getInput(doc, "lt");
	}

	/**
	 * @return The full URI that will be accessed for authentication
	 */
	private String getURI() {
		String URI = casURI;

		if (redirectURI != null && redirectURI.length() > 0) {
			URI += "?service=" + redirectURI;
		}

		return URI;
	}

}