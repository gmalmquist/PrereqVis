package gm.nodeode.io;

import java.io.InputStream;

public class Data {
	public static InputStream getStream(String name) {
		return Data.class.getResourceAsStream("/gm/nodeode/data/" + name);
	}
}
