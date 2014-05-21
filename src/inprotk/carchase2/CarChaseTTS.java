package inprotk.carchase2;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import processing.data.StringDict;
import inprotk.carchase2.Articulator;
import inprotk.carchase2.CarChase;
import inprotk.carchase2.IncrementalArticulator;

public class CarChaseTTS {
	private ArrayList<Situation> situations;
	private ArrayList<Pattern> patterns;
	private HashMap<String, StreetReplacement> streetNames;
	private Articulator articulator;
	private DispatchStream dispatcher;
	private DispatcherThread dispatchThread;
	
	private String flexForm1, flexForm2;
	
	public CarChaseTTS(String messagesFilename, String patternsFilename) {
		try {
			dispatcher = SimpleMonitor.setupDispatcher();
			//articulator = new StandardArticulator(dispatcher);
			articulator = new IncrementalArticulator(dispatcher);
			parseMessages(messagesFilename);
			parsePatterns(patternsFilename);
			dispatchThread = new DispatcherThread();
			dispatchThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void parsePatterns(String filename) throws Exception {
		patterns = new ArrayList<Pattern>();
		streetNames = new HashMap<String, StreetReplacement>();
		Pattern currentPattern = null;
		String[] lines = CarChase.readLines(filename);
		int index = 0;
		for (String line : lines) {
			if (index++ == 0) continue;
			if (line.startsWith("#")) continue;
			if (line.startsWith("--msg")) {
				String[] args = line.substring(6).split("=");
				String[] meta = args[0].split("#");
				MessageInformationLevel type = MessageInformationLevel.valueOf(meta[0]);
				MessageType typeStart = MessageType.valueOf(meta[1]);
				MessageType typeEnd = MessageType.valueOf(meta[2]);
				String key = meta[3];
				currentPattern.addTemplate(key, args[1], typeStart, typeEnd, type);
			}
			else if (line.startsWith("--cond")) {
				currentPattern.addCondition(line.substring(7));
			}
			else if (line.startsWith("++")) {
				patterns.add(currentPattern);
				currentPattern = null;
			}
			else if (line.startsWith("pt")) {
				String[] args = line.substring(3).split(",");
				for (int i = 0; i < args.length; i++) 
					while (args[i].startsWith(" ")) args[i] = args[i].substring(1);
				boolean optional = args.length > 1 && args[1].equals("y");
				currentPattern = new Pattern(optional);
			}
			else if (line.startsWith("street")) {
				String[] args = line.substring(7).split(",");
				for (int i = 0; i < args.length; i++) 
					while (args[i].startsWith(" ")) args[i] = args[i].substring(1);
				String key = args[0];
				String name = args[1];
				if (name.equals("%")) name = key;
				String flex1 = args[2];
				String flex2 = args[3];
				flex1 = flex1.replace("%", key);
				flex2 = flex2.replace("%", key);
				streetNames.put(key, new StreetReplacement(name, flex1, flex2));
			}
			else if (line.startsWith("flex")) {
				if (line.startsWith("flex1")) flexForm1 = line.substring(6);
				if (line.startsWith("flex2")) flexForm2 = line.substring(6);
			}
			else if (line.equals("")) continue;
			else throw new RuntimeException("Illegal line: " + line + " in file " + filename);
		}
	}
	
	private void parseMessages(String filename) throws Exception {
		situations = new ArrayList<Situation>();
		Situation currentMessage = null;
		String[] lines = CarChase.readLines(filename);
		int index = 0;
		for (String line : lines) {
			if (index++ == 0) continue;
			if (line.startsWith("#")) continue;
			if (line.startsWith("--")) {
				String[] args = line.substring(2).split("=");
				String[] meta = args[0].split("#");
				MessageInformationLevel type = MessageInformationLevel.valueOf(meta[0]);
				MessageType typeStart = MessageType.valueOf(meta[1]);
				MessageType typeEnd = MessageType.valueOf(meta[2]);
				String key = meta[3];
				currentMessage.addMessage(key, args[1], typeStart, typeEnd, type);
			}
			else if (line.startsWith("++")) {
				situations.add(currentMessage);
				currentMessage = null;
			}
			else if (line.startsWith("p1")) {
				String[] args = line.substring(3).split(",");
				for (int i = 0; i < args.length; i++) 
					while (args[i].startsWith(" ")) args[i] = args[i].substring(1);
				currentMessage = new DrivingSituation(args[6].equals("y"), args[0], args[1], args[3], Integer.parseInt(args[2]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
			}
			else if (line.equals("")) continue;
			else throw new RuntimeException("Illegal line: " + line + " in file " + filename);
		}
	}
	
	public void matchAndTrigger(String streetName, String prevStreet, String pointName, int previousDistance, int currentDistance, int speed, int direction, int previousDirection) {
		dispatchThread.addDispatchTask(streetName, prevStreet, pointName, previousDistance, currentDistance, speed, direction, previousDirection);
	}
	
	private class DispatcherThread extends Thread {
		private Vector<DispatchAction> actions;
		public DispatcherThread() {
			super("CarChaseTTS Dispatcher");
			actions = new Vector<DispatchAction>();
		}
		public void run() {
			while (true) {
				Vector<DispatchAction> toDispatch = new Vector<DispatchAction>();
				synchronized (this) {
					toDispatch.addAll(actions);
					actions.clear();
				}

				for (DispatchAction action : toDispatch) {
					dispatch(action);
				}
				
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
		private void dispatch(DispatchAction a) {
			TTSAction startAction = null;
			TTSAction continuationAction = null;
			TTSAction lastIU = articulator.getLast();
			boolean continuationPossible = articulator.isSpeaking();
			for (Situation m : situations) {
				if (startAction == null)	
					startAction = m.match(a.streetName, a.prevStreet, a.pointName, a.previousDistance, a.currentDistance, a.speed, a.direction, a.previousDirection, null);
				if (continuationAction == null && continuationPossible)	
					continuationAction = m.match(a.streetName, a.prevStreet, a.pointName, a.previousDistance, a.currentDistance, a.speed, a.direction, a.previousDirection, lastIU);
				if (startAction != null && (!continuationPossible || continuationAction != null)) break;
			}
			
			if (startAction == null) { // Try to find a matching pattern! 
				for (Pattern p : patterns) {
					if (startAction == null)	
						startAction = p.match(a.streetName, a.prevStreet, a.pointName, a.previousDistance, a.currentDistance, a.speed, a.direction, a.previousDirection, null);
					if (continuationAction == null && continuationPossible)	
						continuationAction = p.match(a.streetName, a.prevStreet, a.pointName, a.previousDistance, a.currentDistance, a.speed, a.direction, a.previousDirection, lastIU);
					if (startAction != null && (!continuationPossible || continuationAction != null)) break;
				}
				if (startAction == null)
					return;
			}
			
			CarChase.log(continuationPossible, startAction.text, continuationAction == null ? null : continuationAction.text);
			
			TTSAction finalAction = startAction;
			if (continuationPossible && continuationAction != null)
				finalAction = continuationAction;
			
			CarChase.log("Articulator Say", finalAction.text);

			articulator.printUpcoming();
			// TODO: Implement this (less) important feature.
			articulator.autoRemoveUpcoming();
			articulator.say(finalAction);
		}
		private void addDispatchTask(String streetName, String prevStreet,
				String pointName, int previousDistance, int currentDistance,
				int speed, int direction, int previousDirection) {
			synchronized (this) {
				actions.add(new DispatchAction(streetName, prevStreet, pointName, previousDistance, currentDistance, speed, direction, previousDirection));
				notify();
			}
		}
		
		private class DispatchAction {
			public int previousDirection;
			public int direction;
			private String streetName;
			private String prevStreet;
			private String pointName;
			private int previousDistance;
			private int currentDistance;
			private int speed;
			public DispatchAction(String streetName, String prevStreet,
					String pointName, int previousDistance, int currentDistance,
					int speed, int direction, int previousDirection) {
				super();
				this.streetName = streetName;
				this.prevStreet = prevStreet;
				this.pointName = pointName;
				this.previousDistance = previousDistance;
				this.currentDistance = currentDistance;
				this.speed = speed;
				this.direction = direction;
				this.previousDirection = previousDirection;
			}
		}
	}
	
	public static class TTSAction {
		public MessageType typeStart, typeEnd;
		public MessageInformationLevel type;
		public String text;
		public boolean optional;
		
		private TTSAction(MessageType typeStart, MessageType typeEnd, MessageInformationLevel type, String text, boolean optional) {
			this.typeStart = typeStart;
			this.typeEnd = typeEnd;
			this.type = type;
			this.text = text;
			this.optional = optional;
		}
	}
	
	public static enum MessageInformationLevel {
		T1, // Information Level 1, high speed
		T2, // Information Level 2, normal speed
		T3; // Information Level 3, low speed
		public static MessageInformationLevel fromInteger(int level) {
			int intInformationLevel = Math.min(3, Math.max(1, level));
			return MessageInformationLevel.valueOf("T" + intInformationLevel);
		}
	}
	
	
	// Example: Das Auto faehrt in den Kreisel. (Begin: S2, End: S1)
	// Example: und faehrt in den Kreisel. (Begin: R1, End: S1)
	// Example: Das Auto faehrt auf die Kreuzung zu und (Begin: S1, End: R2)
	public static enum MessageType {
		F1(false),
		R1(true),
		R2(true);
		
		// Moeglich ist: [ F1 F1 ] [ R1 F2 ] [ R2 F1 ] [ F1 F1 ]
		// R benoetigt einen Satz davor, der gerade gesprochen wird;
		// F geht immer, wenn der vorige Satz keinen nachfolgenden braucht.
		
		private boolean requiresSentence;
		private int type;
		private MessageType(boolean requiresSentence) {
			this.requiresSentence = requiresSentence;
			this.type = Integer.parseInt(toString().substring(1));
		}
		
		/**
		 * Is a sentence of the same type required at the beginning / ending? If no, it is a valid beginning / ending of a sentence.
		 * @return
		 */
		public boolean requiresSentence() {
			return requiresSentence;
		}
		
		public int getType() {
			return type;
		}
	}
	
	public class Pattern {
		protected boolean optional;
		public HashMap<String, TTSAction> templates;
		public ArrayList<Condition> conditions;
		public Pattern(boolean optional) {
			templates = new HashMap<String, TTSAction>();
			conditions = new ArrayList<Condition>();
			this.optional = optional;
		}
		
		// Requires: XY#OP#XY
		public void addCondition(String conditionLine) {
			String[] condParts = conditionLine.split("#");
			conditions.add(new Condition(condParts[0], condParts[2], condParts[1]));
		}
		
		public void addTemplate(String key, String value, MessageType sortStart, MessageType sortEnd, MessageInformationLevel type) {
			templates.put(key, new TTSAction(sortStart, sortEnd, type, value, optional));
		}
		
		public TTSAction match(String streetName, String prevStreet, String pointName, int previousDistance, int currentDistance, int speed, int direction, int prevDir, TTSAction last) {
			StringDict replace = new StringDict();
			replace.set("*INTSTREET", streetName);
			replace.set("*INTPREVSTREET", prevStreet);
			StreetReplacement streetRpl = streetNames.get(streetName);
			if (streetRpl == null) streetRpl = new StreetReplacement(streetName);
			StreetReplacement prevStreetRpl = streetNames.get(prevStreet);
			if (prevStreetRpl == null) prevStreetRpl = new StreetReplacement(prevStreet);
			replace.set("*STREET", streetRpl.name);
			replace.set("*PREVSTREET", prevStreetRpl.name);
			replace.set("*FLEX1STREET", streetRpl.flex1);
			replace.set("*FLEX1PREVSTREET", prevStreetRpl.flex1);
			replace.set("*FLEX2STREET", streetRpl.flex2);
			replace.set("*FLEX2PREVSTREET", prevStreetRpl.flex2);
			replace.set("*DIRECTION", direction + "");
			replace.set("*PREVDIRECTION", prevDir + "");
			replace.set("*POINTNAME", pointName);
			replace.set("*NUMSTREETS", "" + CarChase.get().world().points.get(pointName).streets.size());
			replace.set("*SPEED", "" + speed);
			for (Condition cond : conditions) {
				String instancedLeftSide = instanciate(cond.leftSide, replace);
				String instancedRightSide = instanciate(cond.rightSide, replace);
				if (cond.isDistance) {
					int distance = Integer.parseInt(instancedRightSide);
					if (previousDistance > currentDistance) {
						if (distance >= previousDistance || distance < currentDistance) return null;
					}
					else if (distance < previousDistance || distance >= currentDistance) return null;
				} else {
					if (cond.operator == '=' && !instancedLeftSide.equals(instancedRightSide)) return null;
					else if (cond.operator == '!' && instancedLeftSide.equals(instancedRightSide)) return null;
					else if (cond.operator == '<' || cond.operator == '>') {
						int left = Integer.parseInt(instancedLeftSide);
						int right = Integer.parseInt(instancedRightSide);
						if (cond.operator == '<' && left >= right) return null;
						if (cond.operator == '>' && left <= right) return null;
					}
				}
			}
			PatternSituation s = new PatternSituation(optional);
			for (Map.Entry<String, TTSAction> entry : templates.entrySet()) 
				s.addMessage(entry.getKey(), entry.getValue(), instanciate(entry.getValue().text, replace));
			return s.match(streetName, prevStreet, pointName, previousDistance, currentDistance, speed, direction, prevDir, last);
		}
		
		private class Condition {
			public String leftSide;
			public String rightSide;
			public char operator;
			public boolean isDistance;
			
			public Condition(String left, String right, String op) {
				this.leftSide = left;
				this.rightSide = right;
				this.operator = op.charAt(0);
				this.isDistance = leftSide.equals("*DISTANCE");
			}
		}
		
		private String instanciate(String original, StringDict replace) {
			String newString = original;
			for (String key : replace.keyArray())
				newString = newString.replace((CharSequence) key, replace.get(key));
			return newString;
		}
	}
	
	public static abstract class Situation {
		public HashMap<String, TTSAction> messages;
		protected boolean optional;
		
		public Situation(boolean optional) {
			this.messages = new HashMap<String, TTSAction>();
			this.optional = optional;
		}
		
		public void addMessage(String key, String value, MessageType sortStart, MessageType sortEnd, MessageInformationLevel type) {
			messages.put(key, new TTSAction(sortStart, sortEnd, type, value, optional));
		}

		public TTSAction match(String streetName, String prevStreet, String pointName, int previousDistance, int currentDistance, int speed, int direction, int prevDir, TTSAction last) {
			if (!isSituationMatching(streetName, prevStreet, pointName, previousDistance, currentDistance, speed, direction, prevDir)) 
				return null;
			TTSAction[] matches = matches();
			if (matches == null) return null;
			MessageInformationLevel informationLevel = MessageInformationLevel.fromInteger(4 - speed);
			HashMap<MessageInformationLevel,TTSAction> actions = new HashMap<MessageInformationLevel,TTSAction>();
			for (TTSAction action : matches) {
				if (actions.containsKey(action.type)) continue;
				// If last is null, we assume that we currently say nothing. 
				// If last is not null, we (have to) should try to append a continuation,
				// as the dispatcher always asks for both, last = null and last != null.
				//  XXX: Can this be solved better? [ F1 R1 ]  pause  [ F3 R1 ]
				if (last == null)
				{
					if (!action.typeStart.requiresSentence())
						actions.put(action.type, action);
				}
				else if (last.typeEnd.requiresSentence()) {
					if (action.typeStart.getType() == last.typeEnd.getType())
						actions.put(action.type, action);
				}
				else if (action.typeStart.requiresSentence()) {
					if (action.typeStart.getType() == last.typeEnd.getType())
						actions.put(action.type, action);
				}
			}
			if (actions.containsKey(informationLevel)) {
				return actions.get(informationLevel);
			} else {
				for (int distance = 1; distance < MessageInformationLevel.values().length; distance++) {
					MessageInformationLevel lowerLevel = MessageInformationLevel.fromInteger(4 - speed - distance);
					MessageInformationLevel higherLevel = MessageInformationLevel.fromInteger(4 - speed + distance);
					if (actions.containsKey(lowerLevel)) return actions.get(lowerLevel);
					if (actions.containsKey(higherLevel)) return actions.get(higherLevel);
				}
			}
			return null;
		}
		
		public TTSAction[] matches() {
			return messages.values().toArray(new TTSAction[0]);
		}
		
		public abstract boolean isSituationMatching(String streetName, String prevStreet, String pointName, int previousDistance, int currentDistance, int speed, int dir, int prevDir);
	}
	
	private static class PatternSituation extends Situation {

		public PatternSituation(boolean optional) {
			super(optional);
		}
		
		public void addMessage(String key, TTSAction a, String newText) {
			addMessage(key, newText, a.typeStart, a.typeEnd, a.type);
		}

		@Override
		public boolean isSituationMatching(String streetName,
				String prevStreet, String pointName, int previousDistance,
				int currentDistance, int speed, int dir, int prevDir) {
			return true;
		}
		
	}
	
	public static class DrivingSituation extends Situation {
		public String streetName, pointName, prevStreet;
		public int distance, direction, prevDirection;
		public int speed;
		
		public DrivingSituation(boolean optional, String streetName, String pointName,
				String prevStreet, int distance, int direction, int prevDirection) {
			super(optional);
			this.streetName = streetName;
			this.pointName = pointName;
			this.prevStreet = prevStreet;
			this.distance = distance;
			this.direction = direction;
			this.prevDirection = prevDirection;
		}
		
		public boolean isSituationMatching(String streetName, String prevStreet, String pointName, int previousDistance, int currentDistance, int speed, int dir, int prevDir) {
			if (speed == 0) return false;
			if (!this.streetName.equals(streetName)) return false;
			if (!this.prevStreet.equals(prevStreet) && !this.prevStreet.equals("%")) return false;
			if (!this.pointName.equals(pointName)) return false;
			if (previousDistance > currentDistance) {
				if (distance >= previousDistance || distance < currentDistance) return false;
			}
			else if (distance < previousDistance || distance >= currentDistance) return false;
			if (dir != direction) return false;
			if (prevDirection != 0 && prevDir != 0 && prevDir != prevDirection) return false;
			return true;
		}
	}
	
	private class StreetReplacement {
		public String name, flex1, flex2;

		public StreetReplacement(String name, String flex1, String flex2) {
			this.name = name;
			this.flex1 = flex1;
			this.flex2 = flex2;
		}
		
		public StreetReplacement(String name) {
			this(name, flexForm1 + " " + name, flexForm2 + " " + name);
		}
	}
}
