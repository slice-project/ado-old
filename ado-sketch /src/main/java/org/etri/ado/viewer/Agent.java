package org.etri.ado.viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.javatuples.Pair;

public class Agent {
	
	private static final float Scale = 350f;
	public static final int WIDTH = 1024;
	public static final int HEIGHT = 1024; 
	
	private final String m_id;
	private final Color m_color;
	private Point m_loc;
		
	private static Random s_rand = new Random();
	private static final Color[] s_colors = {Color.BLACK, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY, 
			Color.MAGENTA, Color.ORANGE, Color.PINK, Color.WHITE};
	private static Map<String, Color> s_colorMap = new HashMap<String, Color>();
	private static int Index = s_rand.nextInt(8);
	
	static {
		s_colorMap.put("red", Color.RED);
		s_colorMap.put("green", Color.GREEN);
		s_colorMap.put("blue", Color.BLUE);
	}
	
	public Agent(String id) {
		m_id = id;
		if ( s_colorMap.containsKey(id) ) {
			m_color = s_colorMap.get(id);
		}
		else {
			m_color = s_colors[Index++ % 9];
		}
	}
	
	public String getId() {
		return m_id;
	}
	
	public void paint(Graphics g) {
		g.setColor(m_color);
		g.fillOval(m_loc.x + WIDTH, HEIGHT - m_loc.y, 80, 80);
	}
		
	public void setLocation(Pair<Float,Float> loc) {
		int x = (int) (loc.getValue0().floatValue() * Scale);
		int y = (int) (loc.getValue1().floatValue() * Scale);
		m_loc = new Point(x, y);
	}	
}
