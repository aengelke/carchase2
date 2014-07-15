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
	public ArrayList<DirectionAction> directionActions;
	public ArrayList<SpeedAction> speedActions;
	public int startDirection;
	public String startPointStr, startStreetStr;
	public int startDistance;
	
	public int currentDirAction;

	public WorldPoint startPoint, nextPoint;
	public Street currentStreet;
	public int travelDuration;
	public int direction;
	
	private ArrayList<ConfigurationUpdateListener> listeners;
	private int startSpeed;

	protected Configuration() {
		directionActions = new ArrayList<DirectionAction>();
		listeners = new ArrayList<ConfigurationUpdateListener>();
		speedActions = new ArrayList<SpeedAction>();
	}
	
	public Configuration(String filename) {
		try {
			String[] lines = CarChase.readLines(filename);
			directionActions = new ArrayList<DirectionAction>();
			speedActions = new ArrayList<SpeedAction>();
			startSpeed = 2;
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
						directionActions.add(new DirectionAction(Integer.parseInt(args[1])));
					}
					if (name.equals("speed")) {
						speedActions.add(new SpeedAction(Integer.parseInt(args[1]), Integer.parseInt(args[2])));
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
			Collections.sort(speedActions);
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
		ArrayList<WorldPointWrapper> possibilities = getPossibilitiesIntern(startPoint, nextPoint);
		
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
	
	protected ArrayList<WorldPointWrapper> getPossibilitiesIntern(WorldPoint startPoint, WorldPoint nextPoint) {
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
	
	public ArrayList<WorldPoint> getPossibilities(WorldPoint prev, WorldPoint current) {
		ArrayList<WorldPoint> possibilities = new ArrayList<WorldPoint>();
		for (WorldPointWrapper w : getPossibilitiesIntern(prev, current)) {
			possibilities.add(w.point);
		}
		return possibilities;
	}
	
	public ArrayList<WorldPoint> getPossibilities() {
		return getPossibilities(startPoint, nextPoint);
	}
	
	public ArrayList<WorldPoint> getComingPath() {
		ArrayList<WorldPoint> path = new ArrayList<WorldPoint>();
		WorldPoint sp = startPoint;
		WorldPoint np = nextPoint;
		int steppedOver = 0;
		path.add(sp);
		path.add(np);
		for (int i = currentDirAction; i <= directionActions.size() + steppedOver; i++) {
			ArrayList<WorldPointWrapper> nextPossibilities = getPossibilitiesIntern(sp, np);
			int index = nextPossibilities.size() - 1;
			if (i >= directionActions.size() + steppedOver && index == 0) { 
				steppedOver++; 
				i++; 
			}
			else if (i >= directionActions.size() + steppedOver) break;
			if (index > 0) {
				index = directionActions.get(i - steppedOver).data;
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
	
	public void pushDirection(int direction) {
		directionActions.add(new DirectionAction(direction));
		notifyListeners(ConfigurationUpdateListener.PATH_CHANGED);
	}
	
	public void popDirection() {
		if (directionActions.size() >= currentDirAction && directionActions.size() > 0) directionActions.remove(directionActions.size() - 1);
	}
	
	public void setNextDirection(int direction) {
		while (directionActions.size() <= currentDirAction) directionActions.add(null);
		directionActions.set(currentDirAction, new DirectionAction(direction));
		notifyListeners(ConfigurationUpdateListener.PATH_CHANGED);
	}
	
	public int getNextDirection(int millisFromStart) {
		if (currentDirAction >= directionActions.size()) return -1;
		return directionActions.get(currentDirAction).data;
	}
	
	public void directionUsed() {
		currentDirAction++;
	}
	
	public double getCurrentSpeed(int millisFromStart) {
		return getDiscreteSpeed(millisFromStart) / 20.; //0.1; // px/ms
	}
	
	public int getDiscreteSpeed(int millisFromStart) {
		int speed = 2;
		for (SpeedAction a : speedActions) {
			if (a.millis <= millisFromStart)
				speed = a.delta;
		}
		return speed;
	}
	
	public void checkSpeed(int millis, int currentSpeed) {
		if (getDiscreteSpeed(millis) != currentSpeed)
			notifyListeners(ConfigurationUpdateListener.SPEED_CHANGED);
	}

	public void markDone() {
		notifyListeners(ConfigurationUpdateListener.PATH_COMPLETED);
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
	
	public static class SpeedAction implements Comparable<SpeedAction> {
		public int millis, delta;
		
		public SpeedAction(int millis, int delta) {
			this.millis = millis;
			this.delta = delta;
		}
		
		public String toString() {
			return "SpeedAction[millis=" + millis + "ms,delta=" + delta + "]";
		}

		@Override
		public int compareTo(SpeedAction arg0) {
			return millis - arg0.millis;
		}
	}
	
	public static class CarState {
		public final String streetName;
		public final String prevStreetName;
		public final String nextPointName;
		public final String prevPointName;
		public final String pointName;
		public final int direction;
		public final int prevDirection;
		public final int previousDistance;
		public final int currentDistance;
		public final int speed;
		public final int prevSpeed;
		public final int lr;
		
		public CarState(String streetName, String prevStreetName,
				String prevPointName, String nextPointName, String pointName,
				int direction, int prevDirection, int previousDistance,
				int currentDistance, int speed, int prevSpeed, int lr) {
			this.streetName = streetName;
			this.prevStreetName = prevStreetName;
			this.nextPointName = nextPointName;
			this.prevPointName = prevPointName;
			this.pointName = pointName;
			this.direction = direction;
			this.prevDirection = prevDirection;
			this.previousDistance = previousDistance;
			this.currentDistance = currentDistance;
			this.speed = speed;
			this.prevSpeed = prevSpeed;
			this.lr = lr;
		}
	}
}
