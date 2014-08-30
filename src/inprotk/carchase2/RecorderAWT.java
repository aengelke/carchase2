package inprotk.carchase2;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import processing.core.PApplet;

public class RecorderAWT {
	private static boolean record = true;
	public static void startRecording(final Rectangle r) {

		final String name = CarChase.getSuperConfig("baseline").equals("true") ? "base" : "incr";
		new Thread(new Runnable() {

			@Override
			public void run() {
				int lastTime = CarChase.get().getTime();
				ExecutorService execService = Executors.newFixedThreadPool(1);
				for (int i = 0; record; i++) {
					try {
                        Robot robot = new Robot();
                        final BufferedImage snapShot = robot.createScreenCapture(r);
                    	final File f = new File("../../preval2/" + name + "/" + PApplet.nf(i, 5) + ".png");
                    	execService.submit(new Runnable() {
							public void run() {
								try {
									ImageIO.write(snapShot, "png", f);
								} catch (IOException e) {}
							}
						});
                        int tm = CarChase.get().getTime();
                        if (tm < lastTime + 80)
                        	Thread.sleep(80 - tm + lastTime);
                        lastTime = CarChase.get().getTime();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
				}
            	execService.submit(new Runnable() {
					public void run() {
						System.exit(0);
					}
				});
			}
			
		}, "Screenshooting").start();
	}
	public static void stopRecording() {
		record = false;
	}
}
