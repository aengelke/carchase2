package inprotk.carchase2;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import inprotk.carchase2.Configuration;
import inprotk.carchase2.ConfigurationUpdateListener;
import inprotk.carchase2.World.WorldPoint;

public class InteractiveConfiguration extends Configuration implements KeyListener {
	
	private int currentSpeed, backupSpeed;
	private boolean waitForDirection;

	public InteractiveConfiguration() {
		super();
		currentSpeed = 2;
		waitForDirection = false;
		World w = CarChase.get().world();
		
		String[] streetNames = w.streets.keySet().toArray(new String[0]);
		String startStreetStr = (String) JOptionPane.showInputDialog(null, "Choose start street: ", "CarChase 2", JOptionPane.QUESTION_MESSAGE, null, streetNames, streetNames[0]);
		String[] pointNames = w.streets.get(startStreetStr).streetPoints.toArray(new String[0]);
		String startPointStr = (String) JOptionPane.showInputDialog(null, "Choose start point: ", "CarChase 2", JOptionPane.QUESTION_MESSAGE, null, pointNames, pointNames[0]);
		
		boolean ask = false;
		Object next = w.streets.get(startStreetStr).fetchNextPoint(w.points.get(startPointStr), 1);
		if (next == null) startDirection = -1;
		else {
			next = w.streets.get(startStreetStr).fetchNextPoint(w.points.get(startPointStr), -1);
			if (next == null) startDirection = 1;
			else ask = true;
		}
		if (ask) {
			startDirection = Integer.parseInt((String) JOptionPane.showInputDialog(null, "Choose start direction: ", "CarChase 2", JOptionPane.QUESTION_MESSAGE, null, new String[] {"-1", "1"}, "1"));
		}
		
		startPoint = w.points.get(startPointStr);
		currentStreet = w.streets.get(startStreetStr);
		nextPoint = currentStreet.fetchNextPoint(startPoint, startDirection);
		direction = startDirection;
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		int number = e.getKeyChar() - 48;
		if (number >= 0 && number < 10) {
			ArrayList<WorldPoint> path = getComingPath();
			WorldPoint last = null;
			if (path.size() < 2) last = path.size() == 1 ? nextPoint : startPoint;
			else last = path.get(path.size() - 2);
			WorldPoint current = null;
			if (path.size() == 0) current = nextPoint;
			else current = path.get(path.size() - 1);
			int possibilityCount = getPossibilities(last, current).size();
			if (possibilityCount > number) {
				pushDirection(number);
				if (waitForDirection) {
					synchronized (this) {
						notify();
					}
				}
			}
		}
		if (e.getKeyChar() == '/') popDirection();
		if (e.getKeyChar() == '+' && currentSpeed < 3) {
			currentSpeed++; 
			notifyListeners(ConfigurationUpdateListener.SPEED_CHANGED);
		}
		
		if (e.getKeyChar() == '-' && currentSpeed > 0) {
			currentSpeed--;
			notifyListeners(ConfigurationUpdateListener.SPEED_CHANGED);
		}
	}
	
	public double getCurrentSpeed(int millisFromStart) {
		return currentSpeed / 20.0; // px/ms
	}
	
	public int getDiscreteSpeed() {
		return currentSpeed;
	}

	public void markDone() {
		int possibilityCount = getPossibilities(startPoint, nextPoint).size();
		if (possibilityCount > 1 && getNextDirection(-1) == -1) {
			waitForDirection = true;
			backupSpeed = currentSpeed;
			currentSpeed = 0;
			synchronized(this) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			waitForDirection = false;
			currentSpeed = backupSpeed;
		}
		notifyListeners(ConfigurationUpdateListener.PATH_COMPLETED);
	}
}

