package inprotk.carchase2;

import java.util.ArrayList;
import java.util.List;

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
	private float speed;
	private int direction;
	private int previousDirection;
	
	private boolean animating;
	
	private float previousTimelinePosition;

	// TODO: Use setter methods
	public List<WorldPoint> carPath = new ArrayList<WorldPoint>();
	public List<WorldPoint> possibilities = new ArrayList<WorldPoint>();
	
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
		map = loadImage(CarChase.getFilename("data/mapWithStreetNames.png"));
		car = loadImage(CarChase.getFilename("data/car.png"));
		textFont(createFont("ArialMT-Bold", 15));
		frameRate(30);
	}
	
	// Called on update
	public void draw(){
		update();
		render();
	}
	
	public void update() {
		int millis = millis() - startMillis;
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
		int speed = CarChase.get().configuration().getDiscreteSpeed();
		tts.matchAndTrigger(currentStreet.name, previousStreet.name, start.name, prevDistance1, distance1, speed, direction, previousDirection);
		tts.matchAndTrigger(currentStreet.name, previousStreet.name, end.name, prevDistance2, distance2, speed, direction, previousDirection);
		
		previousTimelinePosition = position;
	}
	
	public void render() {
		background(255);
		image(map, 0, 0);
		if (carPosition == null) return;
		
		PVector decisionPoint = endPoint;//wp2vec(carPath.get(carPath.size() - 1));
		
		strokeWeight(5);
		stroke(255, 0, 0);
		line(carPosition.x, carPosition.y, endPoint.x, endPoint.y);
		for (int i = 0; i < possibilities.size(); i++) {
			PVector p = wp2vec(possibilities.get(i));
			float dist = min(p.dist(decisionPoint), 100);
			float theta = PVector.sub(p, decisionPoint).heading();
			float x = dist * cos(theta) + decisionPoint.x;
			float y = dist * sin(theta) + decisionPoint.y;
			stroke(255, 0, 0);
			line(decisionPoint.x, decisionPoint.y, x, y);
			fill(255);
			if (!possibilities.get(i).equals(carPath.get(carPath.size() - 1)) && possibilities.size() != 1)
				noStroke();
			rect(x - 12, y - 12, 24, 24, 4);
			fill(0);
			textAlign(CENTER, CENTER);
			text("" + i, x, y);
			stroke(255, 0, 0);
		}
		
		// Render Car
		pushMatrix();
		translate(carPosition.x, carPosition.y);
		rotate(carAngle + PI);//, 
		translate(-car.width / 2 * CAR_SCALE, -car.height / 2 * CAR_SCALE);
		translate(25, -10);
		scale(CAR_SCALE, CAR_SCALE);
		image(car, 0, 0);
		popMatrix();
	}

	
	public void executeDriveAction(final DriveAction a) {
		animating = true;
		previousStreet = currentStreet == null ? a.street : currentStreet;
		currentStreet = a.street;
		start = a.start;
		end = a.end;
		speed = a.speed;
		previousDirection = direction;
		direction = a.direction;
		
		startPoint = wp2vec(a.start); // the start position is the previous' target
		endPoint = wp2vec(a.end);
		
		final int millisToSkip = a.percent > 0 ? (int) (a.duration * a.percent) : 0;
		previousTimelinePosition = a.percent > 0 ? a.percent : 0;
		
		carStartAngle = carAngle;
		carAngle = carTargetAngle;
		carTargetAngle = atan2(startPoint.x - endPoint.x, endPoint.y - startPoint.y);
		
		if (Math.abs(carAngle - carTargetAngle) > Math.PI * 2 - Math.abs(carAngle - carTargetAngle))
			carTargetAngle = carTargetAngle - PI * 2;
		
		rotationDuration = a.percent > 0 ? 0 : (int) (2 * Math.abs(carAngle - carTargetAngle) * (20 / a.speed));
		transitionDuration = a.duration;
		startMillis = millis() - millisToSkip;
	}

	public float interrupt() {
		if (!animating) return 0;
		animating = false;
		float position = map(millis() - startMillis, 0, transitionDuration, 0, 1);
		return position;
	}
	
	public void initialize(double angle) {
		carStartAngle = carTargetAngle = (float) angle;
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
