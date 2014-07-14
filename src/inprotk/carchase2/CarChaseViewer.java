package inprotk.carchase2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import inprotk.carchase2.Configuration.CarState;
import inprotk.carchase2.World.Street;
import inprotk.carchase2.World.WorldPoint;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

public class CarChaseViewer extends PApplet {
	private static final long serialVersionUID = 1L;

	private static final float CAR_SCALE = 1f / 4.3f;
	
	private PImage map;
	private PImage car;
	private PVector carPosition;
	private ArrayDeque<PVector> carPositions;
	private PVector startPoint;
	private PVector endPoint;
	private float carAngle;
	private float carStartAngle;
	private float carTargetAngle;
	private int rotationDuration;
	private int transitionDuration;
	private int startMillis;
	
	private Street previousStreet;
	private Street currentStreet;
	private WorldPoint start;
	private WorldPoint end;
	private int speed;
	private int prevSpeed;
	private int direction;
	private int previousDirection;
	
	private boolean animating;
	private boolean setup;
	
	private float previousTimelinePosition;

	private Object objectToNotifyOnSetup;
	
	public int sketchWidth() {
		return 1024;
	}
	public int sketchHeight() {
		return 768;
	}
	public String sketchRenderer() {
		return JAVA2D;
	}
	
	public void setup() {
		map = loadImage(CarChase.get().getConfigFilename("mapWithStreetNames.png"));
		car = loadImage(CarChase.getFilename("data/car.png"));
		textFont(createFont("ArialMT-Bold", 15));
		frameRate(CarChase.get().frameRate());
		prevSpeed = -1;
		carAngle = carStartAngle = carTargetAngle = -10;
		setup = true;
		carPositions = new ArrayDeque<>();
		for (int i = 0; i < 15; i++)
			carPositions.addLast(new PVector(0, 0));
		synchronized(objectToNotifyOnSetup) {
			if (objectToNotifyOnSetup != null) objectToNotifyOnSetup.notify();
		}
	}
	
	// Called on update
	public void draw(){
		update();
		render();
	}
	
	public void update() {
		int millis = getTime() - startMillis;
		float rotationPercent = map(millis, 0, rotationDuration, 0, 1);
		rotationPercent = min(1, rotationPercent);
		float position = map(millis, 0, transitionDuration, 0, 1);
		if (position >= 1 && animating) {
			CarChase.get().configuration().markDone();
			animating = false;
			position = 1;
		}
		
		carAngle = lerp(carStartAngle, carTargetAngle, rotationPercent);
		carPosition = PVector.lerp(startPoint, endPoint, position);
		
		if (!animating) return;
		
		CarChase.get().configuration().checkSpeed(CarChase.get().getTime(), speed);

		if (previousTimelinePosition > position) previousTimelinePosition = position;
		if (previousTimelinePosition == position) return;

		int prevDistance1 = direction * (int) (previousTimelinePosition * end.distanceTo(start));
		int prevDistance2 = -direction * (int) ((1 - previousTimelinePosition) * end.distanceTo(start));
		int distance1 = direction * (int) (position * end.distanceTo(start));
		int distance2 = -direction * (int) ((1 - position) * end.distanceTo(start));
		if (prevDistance1 > distance1) {
			int dist1 = distance1;
			distance1 = prevDistance1;
			prevDistance1 = dist1;
		}
		if (prevDistance2 > distance2) {
			int dist2 = distance2;
			distance2 = prevDistance2;
			prevDistance2 = dist2;
		}
		
		CarChaseTTS tts = CarChase.get().tts();
		
		CarState state1 = new CarState(currentStreet.name, previousStreet.name, start.name, end.name, start.name, direction, previousDirection, prevDistance1, distance1, speed, prevSpeed); 
		CarState state2 = new CarState(currentStreet.name, previousStreet.name, start.name, end.name, end.name, direction, previousDirection, prevDistance2, distance2, speed, prevSpeed); 
		
		tts.matchAndTrigger(state1);
		tts.matchAndTrigger(state2);
		
		prevSpeed = speed;
		previousTimelinePosition = position;
	}
	
	public void render() {
		background(255);
		image(map, 0, 0);
		if (carPosition == null) return;
		
		// Render Car
		pushMatrix();
		translate(carPositions.peekLast().x, carPositions.peekLast().y);//carPosition.x, carPosition.y);
		rotate(carAngle + HALF_PI);
		translate(-car.width / 2 * CAR_SCALE, -car.height / 2 * CAR_SCALE);
		carPositions.addLast(new PVector(screenX(8+25,13+14),screenY(8+25,13+14)));
		popMatrix();
		
		float theta = PVector.sub(carPositions.peekLast(), carPositions.peekFirst()).heading()*.8f+(carAngle-HALF_PI-PI)*.2f;
		println(PVector.sub(carPositions.peekLast(), carPositions.peekFirst()).heading(), carAngle-HALF_PI);
		pushMatrix();
		translate(carPosition.x, carPosition.y);
		rotate(theta + 0);
		translate(-car.width / 2 * CAR_SCALE, -car.height / 2 * CAR_SCALE);
		translate(25, 30);
		scale(CAR_SCALE, CAR_SCALE);
		image(car, 0, 0);
		stroke(0,255,0);
		popMatrix();

		//stroke(0,255,0);
		//line(carPositions.peekFirst().x, carPositions.peekFirst().y, carPositions.peekLast().x, carPositions.peekLast().y);
		carPositions.removeFirst();
		
		//println(oldP, carPositionP);
		//saveFrame("../processing-recordings/v2/" + CarChase.get().getConfigName() + "/#####.png");
	}

	public int getTime() {
		return CarChase.get().getTime();
	}
	
	public void notifyOnSetup(Object o) {
		objectToNotifyOnSetup = o;
	}
	
	public void executeDriveAction(final DriveAction a) {
		animating = true;
		previousStreet = currentStreet == null ? a.street : currentStreet;
		currentStreet = a.street;
		start = a.start;
		end = a.end;
		speed = (int) (20 * a.speed);
		previousDirection = direction;
		direction = a.direction;
		
		startPoint = wp2vec(a.start); // the start position is the previous' target
		endPoint = wp2vec(a.end);
		float theta = PVector.sub(startPoint, endPoint).heading();
		float x = 15 * cos(theta) + endPoint.x;
		float y = 15 * sin(theta) + endPoint.y;
		//endPoint = new PVector(x, y);
		
		final int millisToSkip = a.percent > 0 ? (int) (a.duration * a.percent) : 0;
		previousTimelinePosition = a.percent > 0 ? a.percent : 0;
		
		carStartAngle = carAngle;
		carAngle = carTargetAngle;
		carTargetAngle = atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x);
		if (carAngle == -10) carAngle = carStartAngle = carTargetAngle;

		carTargetAngle = (carTargetAngle + TWO_PI) % TWO_PI;
		carTargetAngle += (indexAbsMin(carTargetAngle - TWO_PI - carAngle, carTargetAngle - carAngle, carTargetAngle + TWO_PI - carAngle) - 1) * TWO_PI;
		
		rotationDuration = a.percent > 0 ? 0 : (int) (2 * Math.abs(carAngle - carTargetAngle) * (20 / a.speed));
		transitionDuration = a.duration;
		startMillis = getTime() - millisToSkip;
	}
	
	private static int indexAbsMin(float a, float ... b) {
		float min = abs(a);
		int index = 0;
		for (int i = 0; i < b.length; i++) {
			if (abs(b[i]) < min) {
				min = abs(b[i]);
				index = i + 1;
			}
		}
		return index;
	}

	public float interrupt() {
		if (!animating) return 0;
		animating = false;
		float position = map(getTime() - startMillis, 0, transitionDuration, 0, 1);
		return position;
	}
	
	public static class DriveAction {
		public int duration, direction;
		public float speed;
		public WorldPoint start, end;
		public Street street;
		public float percent;
		
		public DriveAction(WorldPoint start, WorldPoint end, Street street, int direction, int duration, float speed, float percent) {
			this.start = start;
			this.end = end;
			this.direction = direction;
			this.duration = duration;
			this.speed = speed;
			this.street = street;
			this.percent = percent;
		}
	}
	
	private static PVector wp2vec(WorldPoint p) {
		return new PVector(p.x, p.y);
	}
}
