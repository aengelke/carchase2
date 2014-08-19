package inprotk.carchase2;

import inprotk.carchase2.Configuration.CarState;
import inprotk.carchase2.World.Street;
import inprotk.carchase2.World.WorldPoint;

import java.util.LinkedList;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

public class CarChaseViewer extends PApplet {
	private static final long serialVersionUID = 1L;

	private static final float CAR_SCALE = 1f / 4.3f;
	private static final int CURVE_WIDTH = 50;
	
	private LinkedList<Segment> segments;
	
	private PImage map;
	private PImage car;

	private WorldPoint start;
	private WorldPoint end;
	
	private Street previousStreet;
	private Street currentStreet;
	private int speed;
	private int prevSpeed;
	private int direction;
	private int previousDirection;
	
	private float carAngle;
	private PVector carPosition;
	private int leftright;
	
	private int startMillis;
	private int duration;
	
	private boolean animating;
	
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
		carAngle = -10;
		segments = new LinkedList<>();
		synchronized(objectToNotifyOnSetup) {
			if (objectToNotifyOnSetup != null) objectToNotifyOnSetup.notify();
		}
		image(map, 0, 0);
	}
	
	// Called on update
	public void draw(){
		update();
		render();
	}
	
	public void update() {
		if (segments.size() == 0) return;
		Segment segment = segments.peek();
		
		if (segment.getPosition() >= 1 && animating) {
			segments.pop();
			if (segments.size() == 0) {
				CarChase.get().configuration().markDone();
				animating = false;
			}
		}

		if (!animating) return;
		
		segment.update();
		
		float position = segment.getPosition();
		
		//CarChase.get().configuration().checkSpeed(CarChase.get().getTime(), speed);
		
		if (segment instanceof CircleSegment) return;

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
		
		CarState state1 = new CarState(currentStreet.name, previousStreet.name, start.name, end.name, start.name, direction, previousDirection, prevDistance1, distance1, speed, prevSpeed, leftright); 
		CarState state2 = new CarState(currentStreet.name, previousStreet.name, start.name, end.name, end.name, direction, previousDirection, prevDistance2, distance2, speed, prevSpeed, leftright); 
		
		tts.matchAndTrigger(state1);
		tts.matchAndTrigger(state2);
		
		prevSpeed = speed;
		previousTimelinePosition = position;
	}
	
	public void render() {
		background(255);
		image(map, 0, 0);
		if (segments.size() == 0) return;
		
		// Render Car
		
		carAngle = segments.peek().getAngle();
		carPosition = segments.peek().getAbsCarPosition();
		
		pushMatrix();
		translate(carPosition.x, carPosition.y);
		rotate(carAngle + HALF_PI);
		translate(-car.width / 2 * CAR_SCALE, -car.height / 2 * CAR_SCALE);
		translate(20, 0);
		scale(CAR_SCALE, CAR_SCALE);
		image(car, 0, 0);
		stroke(0,255,0);
		popMatrix();
		
		if (CarChase.get().frameRate() < 6)
			saveFrame("../processing-recordings/v4/" + CarChase.get().getConfigName() + "/#####.png");
	}

	public int getTime() {
		return CarChase.get().getTime();
	}
	
	public void notifyOnSetup(Object o) {
		objectToNotifyOnSetup = o;
	}
	
	public void executeDriveAction(final DriveAction a) {
		if (a.percent > 0) segments.clear();
		animating = true;
		previousStreet = currentStreet == null ? a.street : currentStreet;
		currentStreet = a.street;
		start = a.start;
		end = a.end;
		speed = (int) (20 * a.speed);
		previousDirection = direction;
		direction = a.direction;
		
		final int millisToSkip = a.percent > 0 ? (int) (lineDuration(start, end, a.speed) * a.percent) : 0;
		previousTimelinePosition = a.percent > 0 ? a.percent : 0;
		
		float carStartAngle = carAngle;
		float carTargetAngle = atan2(end.y - start.y, end.x - start.x);
		if (carAngle == -10) carAngle = carStartAngle = carTargetAngle;

		carTargetAngle = (carTargetAngle + TWO_PI) % TWO_PI;
		carTargetAngle += (indexAbsMin(carTargetAngle - TWO_PI - carAngle, carTargetAngle - carAngle, carTargetAngle + TWO_PI - carAngle) - 1) * TWO_PI;
		
		int rotationDuration = 0;
		if (carStartAngle != carTargetAngle && a.percent == 0) {
			boolean inverse = carStartAngle < carTargetAngle;
			rotationDuration = (int) (2 * Math.abs(carAngle - carTargetAngle) * (20 / a.speed) * (!inverse ? 1.4 : 1.2));
			segments.add(new CircleSegment(start, rotationDuration, getTime() - millisToSkip, carStartAngle, carTargetAngle));
		}
		
		segments.add(new LineSegment(start, end, a.speed, getTime() - millisToSkip + rotationDuration));

		duration = rotationDuration + segments.peekLast().duration;
		startMillis = getTime() - millisToSkip;
		
		leftright = carStartAngle > carTargetAngle ? 1 : 2;
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
		float position = map(getTime() - startMillis, 0, duration, 0, 1);
		return position;
	}
	
	public static class DriveAction {
		public int /*duration, */direction;
		public float speed;
		public WorldPoint start, end;
		public Street street;
		public float percent;
		
		public DriveAction(WorldPoint start, WorldPoint end, Street street, int direction, int duration, float speed, float percent) {
			this.start = start;
			this.end = end;
			this.direction = direction;
			//this.duration = duration;
			this.speed = speed;
			this.street = street;
			this.percent = percent;
		}
	}
	
	private static PVector wp2vec(WorldPoint p) {
		return new PVector(p.x, p.y);
	}
	
	private abstract class Segment {
		protected PVector startPoint;
		protected PVector endPoint;
		protected PVector carPosition;
		protected int duration;
		protected int startTime;
		protected float carAngle;
		
		public Segment(WorldPoint start, WorldPoint end, int duration, int startTime) {
			startPoint = wp2vec(start);
			endPoint = wp2vec(end);
			this.duration = duration;
			this.startTime = startTime;
			carAngle = -100;
		}
		
		public abstract void update();
		
		public PVector getAbsCarPosition() {
			if (carPosition == null) update();
			return carPosition;
		}
		
		public float getAngle() {
			if (carAngle == -100) update();
			return carAngle;
		}
		
		public float getPosition() { 
			int millis = getTime() - startTime;
			return map(millis, 0, duration, 0, 1);
		}
	}
	
	private class LineSegment extends Segment {
		public LineSegment(WorldPoint start, WorldPoint end, float speed, int startTime) {
			super(start, end, -1, startTime);
			float theta = PVector.sub(endPoint, startPoint).heading();
			float x = -CURVE_WIDTH * cos(theta) + endPoint.x;
			float y = -CURVE_WIDTH * sin(theta) + endPoint.y;
			this.endPoint = new PVector(x, y);
			x = CURVE_WIDTH * cos(theta) + startPoint.x;
			y = CURVE_WIDTH * sin(theta) + startPoint.y;
			this.startPoint = new PVector(x, y);
			carAngle = theta;
			this.duration = lineDuration(start, end, speed);
		}

		@Override
		public void update() {
			this.carPosition = PVector.lerp(startPoint, endPoint, getPosition());
		}
	}
	
	private class CircleSegment extends Segment {
		private PVector mid;
		private float radius;
		private float angleStart, angleEnd;
		private boolean inverse;

		public CircleSegment(WorldPoint pt, int duration,
				int startTime, float anglePr, float angleNe) {
			super(pt, pt, duration, startTime);
			float x = CURVE_WIDTH * cos(angleNe) + pt.x;
			float y = CURVE_WIDTH * sin(angleNe) + pt.y;
			this.endPoint = new PVector(x, y);
			x = -CURVE_WIDTH * cos(anglePr) + pt.x;
			y = -CURVE_WIDTH * sin(anglePr) + pt.y;
			this.startPoint = new PVector(x, y);
			
			this.angleStart = anglePr;
			this.angleEnd = angleNe;
			
			inverse = angleStart < angleEnd;
			
			radius = cotan(angleDistance(angleStart, angleEnd)/ 2)*CURVE_WIDTH*2;
			mid = PVector.add(PVector.mult(PVector.fromAngle(angleStart + HALF_PI * (inverse ? 1 : -1)), radius/2), startPoint);
			
			if (inverse) {
				angleStart += PI;
				angleEnd += PI;
			}
		}

		@Override
		public void update() {
			carAngle = map(getPosition(), 0, 1, angleStart, angleEnd);
			this.carPosition = PVector.add(PVector.mult(PVector.fromAngle(carAngle+HALF_PI), radius / 2), mid);
			carAngle -=  inverse ? PI : 0;
		}
		
	}
	
	private float angleDistance(float angle1, float angle2) {
		float dist1 = angle1 - angle2;
		float dist2 = TWO_PI - angle2 + angle1;
		return min(abs(dist1), abs(dist2));
	}
	
	private float cotan(float f) {
		return 1/tan(f);
	}
	
	private int lineDuration(WorldPoint start, WorldPoint end, float speed) {
		PVector startPoint = wp2vec(start), endPoint = wp2vec(end);
		float theta = PVector.sub(endPoint, startPoint).heading();
		float x = -CURVE_WIDTH * cos(theta) + endPoint.x;
		float y = -CURVE_WIDTH * sin(theta) + endPoint.y;
		endPoint = new PVector(x, y);
		x = CURVE_WIDTH * cos(theta) + startPoint.x;
		y = CURVE_WIDTH * sin(theta) + startPoint.y;
		startPoint = new PVector(x, y);
		carAngle = theta;
		return (int) (startPoint.dist(endPoint) / speed);
	}
}
