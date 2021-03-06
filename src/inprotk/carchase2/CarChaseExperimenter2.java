package inprotk.carchase2;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.KeyListener;

import inprotk.carchase2.CarChaseViewer.DriveAction;
import inprotk.carchase2.World.Street;
import inprotk.carchase2.World.WorldPoint;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import inprotk.carchase2.CarChase;
import inprotk.carchase2.CarChaseExperimenter2;
import inprotk.carchase2.CarChaseViewer;
import inprotk.carchase2.Configuration;
import inprotk.carchase2.ConfigurationUpdateListener;

public class CarChaseExperimenter2 {
	private CarChaseViewer viewer;
	private JFrame frame;
	
	private int lastEvent;

	private CarChaseExperimenter2() {
		setupGUI();
		lastEvent = -1;
	}
	
	private void setupGUI() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					frame = new JFrame("CarApp");
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					frame.setUndecorated(true);
					frame.setSize(1024, 768);
					viewer = new CarChaseViewer();
					viewer.init();
					frame.setLayout(new BorderLayout());
					frame.add(CarChase.get().tts().getHypothesisViewer().getTextField(), BorderLayout.SOUTH);
					frame.add(viewer, BorderLayout.CENTER);
			        frame.pack();
					frame.setVisible(true);
					viewer.requestFocusInWindow();
					if (CarChase.get().isInteractive()) {
						viewer.addKeyListener((KeyListener) CarChase.get().configuration());
					}
					viewer.start();
					
					if (CarChase.getSuperConfig("recording").equals("awt"))
						RecorderAWT.startRecording(new Rectangle(0, 0, 1024, 768));
					
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		final CarChaseExperimenter2 self = this;

		CarChase.get().configuration().addListener(new ConfigurationUpdateListener() {
			
			@Override
			public void configurationUpdated(int type) {
				if (type != ConfigurationUpdateListener.PATH_CHANGED) {
					lastEvent = type;
					synchronized(self) {
						self.notify();
					}
				}
			}
		});
	}
	
	public void execute() {
		Configuration config = CarChase.get().configuration();
		
		WorldPoint startPoint = config.startPoint;
		WorldPoint nextPoint = config.nextPoint;
		Street currentStreet = config.currentStreet;
		int direction = config.direction;
		int travelDuration;
		float percent = 0;
		
		viewer.notifyOnSetup(this);
		
		synchronized(this) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		
		do {
			
			int time = CarChase.get().getTime();
			travelDuration = (int) ((1 - percent) * (nextPoint.distanceTo(startPoint) / config.getCurrentSpeed(time) - 10));
			
			viewer.executeDriveAction(new DriveAction(startPoint, nextPoint, currentStreet, direction, travelDuration, (float) config.getCurrentSpeed(time), percent));
			
			synchronized(this) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			
			if (lastEvent == ConfigurationUpdateListener.PATH_COMPLETED) {
				boolean keepRunning = config.update(time);
				if (!keepRunning) break;
				
				startPoint = config.startPoint;
				currentStreet = config.currentStreet;
				nextPoint = config.nextPoint;
				direction = config.direction;
				percent = 0;
				lastEvent = -1;
			}
			else if (lastEvent == ConfigurationUpdateListener.SPEED_CHANGED) {
				float interruptionPercent = viewer.interrupt();
				percent = interruptionPercent;
				lastEvent = -1;
			}
		} while (true);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		if (CarChase.getSuperConfig("recording").equals("awt")) {
			// This will handle the shutdown.
			RecorderAWT.stopRecording();
		} else {
			System.exit(0);
		}
	}
	
	public static void main(String[] args) {
		CarChase.get().init("default");
		
		CarChaseExperimenter2 exp = new CarChaseExperimenter2();
		CarChase.get().start();
		exp.execute();
	}
}
