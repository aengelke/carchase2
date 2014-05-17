package inprotk.carchase2;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import inprotk.carchase2.Configuration;
import inprotk.carchase2.ConfigurationUpdateListener;

public class InteractiveConfiguration extends Configuration implements KeyListener {
	
	private int currentSpeed, backupSpeed;
	private boolean waitForDirection;

	public InteractiveConfiguration(String filename) {
		super(filename);
		currentSpeed = 2;
		waitForDirection = false;
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
			int possibilityCount = getPossibilities(startPoint, nextPoint).size();
			if (possibilityCount > number) {
				setNextDirection(number);
				if (waitForDirection) {
					synchronized (this) {
						notify();
					}
				}
			}
		}
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

