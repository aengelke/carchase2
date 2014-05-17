package inprotk.carchase2;

import inprotk.carchase2.World.Street;
import inprotk.carchase2.World.WorldPoint;
import inprotk.carchase2.World.WorldPointWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import inprotk.carchase2.CarChase;
import inprotk.carchase2.ConfigurationUpdateListener;
import inprotk.carchase2.World;

public class Configuration {
	public ArrayList<DirectionAction> actions;
	public int startDirection;
	public String startPointStr, startStreetStr;
	public int startDistance;
	
	public int currentDirAction;

	public WorldPoint startPoint, nextPoint;
	public Street currentStreet;
	public int travelDuration;
	public int direction;
	
	private ArrayList<ConfigurationUpdateListener> listeners;
	
	public Configuration(String filename) {
		try {
			String[] lines = CarChase.readLines(filename);
			actions = new ArrayList<DirectionAction>();
			for (String line : lines) {
				if (line.startsWith("#")) continue;
				if (line.startsWith("Start:")) {
					String[] args = line.substring(line.indexOf(":")).split(",");
					args[0] = args[0].substring(1); // Remove : at the beginning
					for (int i = 0; i < args.length; i++) 
						while (args[i].startsWith(" ")) args[i] = args[i].substring(1);
					startPointStr = args[0];
					startStreetStr = args[1];
					startDirection = Integer.parseInt(args[2] + "1");
					startDistance = Integer.parseInt(args[3]);
				}
				else if (line.startsWith("---")) {
					String[] args = line.substring(line.indexOf("---") + 3).split(",");
					args[0] = args[0].substring(1); // Remove : at the beginning
					for (int i = 0; i < args.length; i++) 
						while (args[i].startsWith(" ")) args[i] = args[i].substring(1);
					String name = args[0];
					if (name.equals("direction")) {
						actions.add(new DirectionAction(Integer.parseInt(args[1])));
					}
				}
				else if (line.equals("")) continue;
				else throw new RuntimeException("Illegal line: " + line + " in file " + filename);
			}
			
			World w = CarChase.get().world();
			startPoint = w.points.get(startPointStr);
			currentStreet = w.streets.get(startStreetStr);
			nextPoint = currentStreet.fetchNextPoint(startPoint, startDirection);
			direction = startDirection;
			listeners = new ArrayList<ConfigurationUpdateListener>();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addListener(ConfigurationUpdateListener l) {
		if (l != null)
			listeners.add(l);
	}
	
	protected void notifyListeners(int type) {
		for (ConfigurationUpdateListener l : listeners) 
			l.configurationUpdated(type);
	}
	
	public boolean update(int time) {
		ArrayList<WorldPointWrapper> possibilities = getPossibilities(startPoint, nextPoint);
		
		int wrapperIndex = possibilities.size() - 1;
		if (possibilities.size() > 1) {
			wrapperIndex = getNextDirection(time);
			directionUsed();
		}
		if (wrapperIndex < 0) return false;
		WorldPointWrapper wrapper = possibilities.get(wrapperIndex);
		startPoint = nextPoint;
		nextPoint = wrapper.point;
		currentStreet = wrapper.street;
		direction = wrapper.direction;
		return true;
	}
	
	protected ArrayList<WorldPointWrapper> getPossibilities(WorldPoint startPoint, WorldPoint nextPoint) {
		ArrayList<String> streets = nextPoint.streets;
		ArrayList<WorldPointWrapper> possibilities = new ArrayList<WorldPointWrapper>();
		for (String streetName : streets)  {
			Street street = CarChase.get().world().streets.get(streetName);
			WorldPoint point1 = street.bidirectional ? street.fetchNextPoint(nextPoint, -1) : null;
			if (point1 != null && !point1.equals(startPoint)) {
				double theta1 = Math.atan2(nextPoint.y - point1.y, nextPoint.x - point1.x);
				WorldPointWrapper wrapper1 = new WorldPointWrapper(point1, street, theta1, -1);
				if (!possibilities.contains(wrapper1)) {
					possibilities.add(wrapper1);
				}
			}
			WorldPoint point2 = street.fetchNextPoint(nextPoint, 1);
			if (point2 != null && !point2.equals(startPoint)) {
				double theta2 = Math.atan2(nextPoint.y - point2.y, nextPoint.x - point2.x);
				WorldPointWrapper wrapper2 = new WorldPointWrapper(point2, street, theta2, 1);
				if (!possibilities.contains(wrapper2)) {
					possibilities.add(wrapper2);
				}
			}
		}
		
		Collections.sort(possibilities, new Comparator<WorldPointWrapper>() {
			@Override
			public int compare(WorldPointWrapper arg0, WorldPointWrapper arg1) {
				return (int) (100 * arg0.theta - 100 * arg1.theta);
			}
		});
		return possibilities;
	}
	
	public ArrayList<WorldPoint> getPossibilities() {
		ArrayList<WorldPoint> possibilities = new ArrayList<WorldPoint>();
		for (WorldPointWrapper w : getPossibilities(startPoint, nextPoint)) {
			possibilities.add(w.point);
		}
		/*WorldPoint point1 = nextPoint;
		while (possibilities.size() == 1) {
			WorldPoint pStart = possibilities.get(0);
			possibilities = new ArrayList<WorldPoint>();
			for (WorldPointWrapper w : getPossibilities(point1, pStart)) {
				possibilities.add(w.point);
			}
			point1 = pStart;
		}*/
		return possibilities;
	}
	
	public ArrayList<WorldPoint> getComingPath() {
		ArrayList<WorldPoint> path = new ArrayList<WorldPoint>();
		WorldPoint sp = startPoint;
		WorldPoint np = nextPoint;
		int steppedOver = 0;
		path.add(sp);
		path.add(np);
		for (int i = currentDirAction; i < actions.size() + steppedOver; i++) {
			ArrayList<WorldPointWrapper> nextPossibilities = getPossibilities(sp, np);
			int index = nextPossibilities.size() - 1;
			if (index > 0) {
				index = actions.get(i - steppedOver).data;
			} else if (index == 0) {
				steppedOver++;
			} else {
				break;
			}
			path.add(nextPossibilities.get(index).point);
			sp = np;
			np = nextPossibilities.get(index).point;
		}
		return path;
	}
	
	public void setNextDirection(int direction) {
		while (actions.size() <= currentDirAction) actions.add(null);
		actions.set(currentDirAction, new DirectionAction(direction));
		notifyListeners(ConfigurationUpdateListener.PATH_CHANGED);
	}
	
	public int getNextDirection(int millisFromStart) {
		if (currentDirAction >= actions.size()) return -1;
		return actions.get(currentDirAction).data;
	}
	
	public void directionUsed() {
		currentDirAction++;
	}
	
	public double getCurrentSpeed(int millisFromStart) {
		return 0.1; // px/ms
	}
	
	public int getDiscreteSpeed() {
		return (int) (getCurrentSpeed(0) * 20.0);
	}
	
	public static class DirectionAction {
		public int data;
		
		public DirectionAction(int data) {
			this.data = data;
		}
		
		public String toString() {
			return "DirectionAction[data=" + data + "]";
		}
	}

	public void markDone() {
		notifyListeners(ConfigurationUpdateListener.PATH_COMPLETED);
	}
}
