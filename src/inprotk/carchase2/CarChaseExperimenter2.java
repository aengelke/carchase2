package inprotk.carchase2;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import inprotk.carchase2.CarChaseViewer.DriveAction;
import inprotk.carchase2.World.Street;
import inprotk.carchase2.World.WorldPoint;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import processing.core.PApplet;
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
					
					if (!CarChase.getSuperConfig("recording").equals("awt")) return;
					
					final String name = CarChase.getSuperConfig("baseline").equals("true") ? "base" : "incr";
					new Thread(new Runnable() {

						@Override
						public void run() {
							int lastTime = CarChase.get().getTime();
							for (int i = 0; i < 20*50; i++){
							try {
	                            Robot robot = new Robot();
	                            final BufferedImage snapShot = robot.createScreenCapture(new Rectangle(0, 0, 1024, 768+20));
                            	final File f = new File("../../preval2/" + name + "/" + PApplet.nf(i, 5) + ".png");
								new Thread(new Runnable() {
									public void run() {
										try {
											ImageIO.write(snapShot, "png", f);
										} catch (IOException e) {}
									}
								}).start();
	                            int tm = CarChase.get().getTime();
	                            if (tm < lastTime + 50)
	                            	Thread.sleep(50 - tm + lastTime);
	                        } catch (Exception ex) {
	                            ex.printStackTrace();
	                        }}
						}
						
					}, "Screenshooting").start();
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
			Thread.sleep(14000);
		} catch (InterruptedException e) {
		}
		System.exit(0);
	}
	
	public static void main(String[] args) {
		CarChase.get().init("default");
		
		CarChaseExperimenter2 exp = new CarChaseExperimenter2();
		CarChase.get().start();
		exp.execute();
	}
}
