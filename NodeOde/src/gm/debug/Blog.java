package gm.debug;
import java.awt.Font;
import java.awt.Point;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Un-cluttering our debugs
 * @author Garrett
 *
 */
public class Blog {
	public static boolean GUI_LOGGER = true;
	
	private static final Object swimming = new Object();
	
	private static volatile boolean anyChange = true;
	
	private HashMap<String, String> logs;
	private HashMap<String, Long> stamps;
	private LinkedList<String> dumps;
	
	private Blog() {
		logs = new HashMap<String, String>();
		stamps = new HashMap<String, Long>();
		dumps = new LinkedList<String>();
		
		GUI_LOGGER |= System.getProperty("user.name").toUpperCase().startsWith("G");
		if (GUI_LOGGER)
			initGUI();
	}

	private volatile static Blog me;
	private static Blog inst() {
		synchronized (swimming) {
			if (me == null) me = new Blog();
			return me;
		}
	}
	
	public static void log(Object o) {
		synchronized (swimming) {
			inst().dumps.add(String.valueOf(o));
		}
	}
	
	public static void unlog(Object labelO) {
		synchronized (swimming) {
			inst();
			
			anyChange = true;
			
			String label = String.valueOf(labelO);
			if (labelO != null && labelO instanceof Class)
				label = ((Class<?>)labelO).getSimpleName();
			if (me.logs.containsKey(label)) {
				me.logs.remove(label);
				if (GUI_LOGGER)
					me.updateGUI();
			}
		}
	}
	
	public static void log(Object labelO, Object o) {
		synchronized (swimming) {
			inst();
			
			anyChange = true;
			
			String label = String.valueOf(labelO);
			if (labelO != null && labelO instanceof Class)
				label = ((Class<?>)labelO).getSimpleName();
			
			String s = String.valueOf(o);
			
			if (!me.logs.containsKey(label) || !me.logs.get(label).equals(s)) {
				me.logs.put(label, s);
				
				if (GUI_LOGGER)
					me.updateGUI();
				else
					System.out.println(s);
			}
	
			long ctime = System.currentTimeMillis();
			me.stamps.put(label, ctime);
		}
	}
	
	private void updateGUI() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				List<String> keys = new LinkedList<String>();
				synchronized (swimming) {
					keys.addAll(inst().logs.keySet());
				}
				Collections.sort(keys, new Comparator<String>() {
					public int compare(String a, String b) {
						if (a.length() > b.length()) return 1;
						if (a.length() < b.length()) return -1;
						return a.compareTo(b);
					}
				});
				StringBuffer sb = new StringBuffer();
				int maxk = 0;
				for (String s : keys)
					if (s.length() > maxk)
						maxk = s.length();
				long ctime = System.currentTimeMillis();
				synchronized (swimming) {
					for (String s : keys) {
						double sago = (ctime - stamps.get(s))/1000.0;
						sago = (int)(100 * sago) / 100.0;
						
						if (sb.length() > 0)
							sb.append("\n");
						sb.append("[");
						sb.append(s);
						sb.append("] ");
						for (int i = 0; i < maxk - s.length(); i++)
							sb.append(" ");
						String msg = logs.get(s) + "   (" + sago + "s ago)";
						int mlen =  80 - (maxk+3);
						while (msg.length() > mlen) {
							sb.append(msg.substring(0, mlen).trim());
							msg = msg.substring(mlen).trim();
							sb.append("\n");
							for (int i = 0; i < maxk+3; i++)
								sb.append(" ");
						}
						sb.append(msg);
					}
				}
				logArea.setText(sb.toString());
				
				anyChange = false;
			}
		});
	}
	
	private void updateConsole() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized (swimming) {
					while (!dumps.isEmpty()) {
						if (logDump.getText().length() > 0)
							logDump.append("\n");
						logDump.append(dumps.removeFirst());
					}
				}
			}
		});
	}

	private JFrame logFrame;
	private JTextArea logArea;
	private JTextArea logDump;
	private void initGUI() {
		logFrame = new JFrame("Logging");
		logFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Font font = new Font("monospaced", Font.PLAIN, 12);
		
		logArea = new JTextArea(20, 80);
		JTextArea area = logArea;
		area.setEditable(false);
		area.setWrapStyleWord(true);
		area.setLineWrap(true);
		
		area.setFont(font);
		
		logDump = new JTextArea(20, 80);
		logDump.setEditable(false);
		logDump.setWrapStyleWord(true);
		logDump.setLineWrap(true);
		logDump.setFont(font);
		
		JTabbedPane tabs = new JTabbedPane();
		
		tabs.addTab("Fields", new JScrollPane(logArea, 
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		
		tabs.addTab("Console", new JScrollPane(logDump, 
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		
		logFrame.add(tabs);
		
		logFrame.pack();
		
		logFrame.setLocation(new Point(0,0));
		logFrame.setVisible(true);

		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try { Thread.sleep(500); } catch (InterruptedException e) { }
					if (anyChange)
						updateGUI();
					if (!dumps.isEmpty())
						updateConsole();
				}
			}
		}).start();
	}
}
