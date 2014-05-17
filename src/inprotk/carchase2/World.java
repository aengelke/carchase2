package inprotk.carchase2;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import inprotk.carchase2.CarChase;


/**
 * Eine Darstellung der Welt, bestehend aus Straﬂen und Knotenpunkten.
 * Eine Strasse wird durch eine Liste von Punkten beschrieben, 
 * @author Alexis Engelke
 *
 */
public class World {
	public HashMap<String, Street> streets;
	public HashMap<String, WorldPoint> points;
	public World(String filename) {
		try {
			String[] lines = CarChase.readLines(filename);
			points = new HashMap<String, WorldPoint>();
			streets = new HashMap<String, Street>();
			for (String line : lines) {
				if (line.startsWith("#")) continue;
				else if (line.startsWith("Point:")) {
					String[] args = line.substring(line.indexOf(":")).split(",");
					args[0] = args[0].substring(1); // Remove : at the beginning
					for (int i = 0; i < args.length; i++) 
						while (args[i].startsWith(" ")) args[i] = args[i].substring(1);
					String name = args[0];
					int x = Integer.parseInt(args[1]);
					int y = Integer.parseInt(args[2]);
					points.put(name, new WorldPoint(name, x, y));
				}
				else if (line.startsWith("Street:")) {
					String[] args = line.substring(line.indexOf(":")).split(",");
					args[0] = args[0].substring(1); // Remove : at the beginning
					for (int i = 0; i < args.length; i++) 
						while (args[i].startsWith(" ")) args[i] = args[i].substring(1);
					String name = args[0];
					boolean bidirectional = Integer.parseInt(args[1]) > 0;
					ArrayList<String> streetPoints = new ArrayList<String>();
					for (int i = 2; i < args.length; i++) {
						streetPoints.add(args[i]);
					}
					streets.put(name, new Street(name, bidirectional, streetPoints));
				}
				else if (line.equals("")) continue;
				else throw new RuntimeException("Illegal line: " + line + " in file " + filename);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public class Street {
		public ArrayList<String> streetPoints;
		public String name;
		public boolean bidirectional;
		
		public Street(String name, boolean bidir, ArrayList<String> streetPoints) {
			assert points != null;
			assert name != null;
			this.name = name;
			this.bidirectional = bidir;
			this.streetPoints = new ArrayList<String>();
			this.streetPoints.addAll(streetPoints);
			for (String p : streetPoints) 
				points.get(p).addStreet(name);
		}
		
		public String toString() {
			String s = "Street[name=" + name + (bidirectional?",bidirectional":",onedirectional") + ",points=" + streetPoints.toString() + "]";
			return s;
		}
		
		public WorldPoint fetchNextPoint(WorldPoint wp, int step) {
			if (streetPoints.indexOf(wp.name) + step < 0 || streetPoints.indexOf(wp.name) + step >= streetPoints.size())
				return null;
			return points.get(streetPoints.get(streetPoints.indexOf(wp.name) + step));
		}
	}
	public class WorldPoint {
		public ArrayList<String> streets;
		public String name;
		public int x, y;
		
		public WorldPoint(String name, int x, int y) {
			assert name != null;
			streets = new ArrayList<String>();
			this.name = name;
			this.x = x;
			this.y = y;
		}
		
		public void addStreet(String s) {
			streets.add(s);
		}
		
		public String toString() {
			String s = "Point[name=" + name + ",x" + x + ",y=" + y + ",streets=" + streets.toString() + "]";
			return s;
		}
		
		public int distanceTo(WorldPoint other) {
			return (int) Math.sqrt(	Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
		}
		
		public Point asPoint() {
			return new Point(x, y);
		}
	}
	
	public static class WorldPointWrapper {
		public WorldPoint point;
		public Street street;
		public double theta;
		public int direction;
		public WorldPointWrapper(WorldPoint p, Street s, double alpha, int dir) {
			point = p;
			street = s;
			theta = alpha;
			direction = dir;
		}
		
		public String toString() {
			return point.name + "," + street.name + "," + direction + "," + theta;
		}
		
		public boolean equals(Object another) {
			if (!(another instanceof WorldPointWrapper)) return false;
			WorldPointWrapper w = (WorldPointWrapper) another;
			return point.name.equals(w.point.name) && street.name.equals(w.street.name) &&
				    theta == w.theta && direction == w.direction;
		}
	}
}
