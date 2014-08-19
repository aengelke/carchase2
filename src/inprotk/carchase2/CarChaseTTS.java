package inprotk.carchase2;

import inpro.apps.SimpleMonitor;
import inpro.audio.DispatchStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import processing.data.StringDict;
import inprotk.carchase2.Articulator;
import inprotk.carchase2.CarChase;
import inprotk.carchase2.Articulator.Articulatable;
import inprotk.carchase2.Configuration.CarState;
import inprotk.carchase2.IncrementalArticulator;
import inprotk.carchase2.World.Street;
import inprotk.carchase2.World.WorldPoint;

public class CarChaseTTS {
	private ArrayList<Pattern> patterns;
	private HashMap<String, StreetReplacement> streetNames;
	private Articulator articulator;
	private DispatchStream dispatcher;
	private DispatcherThread dispatchThread;

	private String flexForm1, flexForm2, left, right;

	public CarChaseTTS(String patternsFilename) {
		try {
			dispatcher = SimpleMonitor.setupDispatcher();
			//articulator = new StandardArticulator(dispatcher);
			articulator = new IncrementalArticulator(dispatcher);
			parsePatterns(patternsFilename);
			dispatchThread = new DispatcherThread();
			dispatchThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MyCurrentHypothesisViewer getHypothesisViewer() {
		return articulator.getHypothesisViewer();
	}

	private void parsePatterns(String filename) throws Exception {
		patterns = new ArrayList<Pattern>();
		streetNames = new HashMap<String, StreetReplacement>();
		Pattern currentPattern = null;
		String[] lines = CarChase.readLines(filename);
		int index = 0;
		for (String line : lines) {
			if (index++ == 0) continue;
			line = line.trim();
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
			else if (line.startsWith("leftright")) {
				String[] values = line.split(" ");
				left = values[1];
				right = values[2];
				CarChase.log("LR", left, right);
			}
			else if (line.equals("")) continue;
			else throw new RuntimeException("Illegal line: " + line + " in file " + filename);
		}
	}

	public void matchAndTrigger(CarState state) {
		dispatchThread.addDispatchTask(state);
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
			CarChaseArticulatable startAction = null;
			CarChaseArticulatable continuationAction = null;
			CarChaseArticulatable lastIU = (CarChaseArticulatable) articulator.getLast();
			boolean continuationPossible = articulator.isSpeaking();

			for (Pattern p : patterns) {
				if (startAction == null)
					startAction = p.match(a.state, null);
				if (continuationAction == null && continuationPossible)
					continuationAction = p.match(a.state, lastIU);
				if (startAction != null && (!continuationPossible || continuationAction != null)) break;
			}
			if (startAction == null)
				return;

			//CarChase.log(continuationPossible, startAction.text, continuationAction == null ? null : continuationAction.text);

			CarChaseArticulatable finalAction = startAction;
			if (continuationPossible && continuationAction != null)
				finalAction = continuationAction;

			CarChase.log("Articulator Say", finalAction);

			articulator.printUpcoming();
			// TODO: Implement this (less) important feature.
			articulator.reduceOffset();
			articulator.say(finalAction);
		}
		private void addDispatchTask(CarState state) {
			synchronized (this) {
				actions.add(new DispatchAction(state));
				notify();
			}
		}

		private class DispatchAction {
			private CarState state;
			public DispatchAction(CarState state) {
				this.state = state;
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

		public String toString() {
			return "[TTSAction text=" + text + ",level=" + type.toString() + "]";
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
		F2(false),
		F3(false),
		R1(true),
		R2(true),
		R3(true),
		R4(true),
		R5(true),
		R6(true);

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
			if (templates.containsKey(key)) CarChase.log("WARNING: Duplicate key", key);
			templates.put(key, new TTSAction(sortStart, sortEnd, type, value, optional));
		}

		private StringDict instantiateVariables(CarState s) {
			World w = CarChase.get().world();
			Street currentStreet = w.streets.get(s.streetName);
			Street prevStreet = w.streets.get(s.prevStreetName);
			WorldPoint nextPoint = w.points.get(s.nextPointName);
			WorldPoint prevPoint = w.points.get(s.prevPointName);
			StringDict replace = new StringDict();
			replace.set("*INTSTREET", s.streetName);
			replace.set("*INTPREVSTREET", s.prevStreetName);
			StreetReplacement streetRpl = streetNames.get(s.streetName);
			if (streetRpl == null) streetRpl = new StreetReplacement(s.streetName);
			StreetReplacement prevStreetRpl = streetNames.get(s.prevStreetName);
			if (prevStreetRpl == null) prevStreetRpl = new StreetReplacement(s.prevStreetName);
			replace.set("*STREET", streetRpl.name);
			replace.set("*PREVSTREET", prevStreetRpl.name);
			replace.set("*FLEX1STREET", streetRpl.flex1);
			replace.set("*FLEX1PREVSTREET", prevStreetRpl.flex1);
			replace.set("*FLEX2STREET", streetRpl.flex2);
			replace.set("*FLEX2PREVSTREET", prevStreetRpl.flex2);
			replace.set("*DIRECTION", s.direction + "");
			replace.set("*PREVDIRECTION", s.prevDirection + "");
			replace.set("*POINTNAME", s.nextPointName);
			replace.set("*PREVPOINTNAME", s.prevPointName);
			replace.set("*SPEED", "" + s.speed);
			replace.set("*PREVSPEED", "" + s.prevSpeed);
			replace.set("*BIDIRECTIONAL", "" + (currentStreet.bidirectional ? 1 : 0));
			replace.set("*NUMSTREETS", "" + nextPoint.streets.size());
			replace.set("*LEFTRIGHT", "" + (s.lr == 1 ? left : right));
			// Junctions
			applyJunction(nextPoint, currentStreet, replace, s.direction, nextPoint.streets, false);
			applyJunction(prevPoint, prevStreet, replace, s.prevDirection, prevPoint.streets, true);

			return replace;
		}

		public CarChaseArticulatable match(CarState s, CarChaseArticulatable lastArticulatable) {
			TTSAction last = lastArticulatable != null ? lastArticulatable.preferred : null;
			StringDict replace = instantiateVariables(s);
			for (Condition cond : conditions) {
				String instancedLeftSide = instanciate(cond.leftSide, replace);
				String instancedRightSide = instanciate(cond.rightSide, replace);
				if (cond.isDistance) {
					int distance = Integer.parseInt(instancedRightSide);
					if (cond.operator == '=') {
						if (s.previousDistance > s.currentDistance) {
							if (distance >= s.previousDistance || distance < s.currentDistance) return null;
						}
						else if (distance < s.previousDistance || distance >= s.currentDistance) return null;
					} else if (cond.operator == '>') {
						if (s.currentDistance <= distance) return null;
					}
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
			ArrayList<TTSAction> messages = new ArrayList<TTSAction>();
			for (Map.Entry<String, TTSAction> entry : templates.entrySet())
				messages.add(new TTSAction(entry.getValue().typeStart, entry.getValue().typeEnd, entry.getValue().type, instanciate(entry.getValue().text, replace), entry.getValue().optional));
			TTSAction[] matches = messages.toArray(new TTSAction[0]);
			if (matches == null) return null;
			MessageInformationLevel informationLevel = MessageInformationLevel.fromInteger(4 - s.speed);
			HashMap<MessageInformationLevel,ArrayList<TTSAction>> actions = new HashMap<MessageInformationLevel,ArrayList<TTSAction>>();
			for (MessageInformationLevel level : MessageInformationLevel.values())
				actions.put(level, new ArrayList<TTSAction>());
			for (TTSAction action : matches) {
				// If last is null, we assume that we currently say nothing.
				// If last is not null, we (have to) should try to append a continuation,
				// as the dispatcher always asks for both, last = null and last != null.
				//  XXX: Can this be solved better? [ F1 R1 ]  pause  [ F3 R1 ]
				if (last == null)
				{
					if (!action.typeStart.requiresSentence())
						actions.get(action.type).add(action);
				}
				else if (last.typeEnd.requiresSentence()) {
					if (action.typeStart.getType() == last.typeEnd.getType())
						actions.get(action.type).add(action);
				}
				else if (action.typeStart.requiresSentence()) {
					if (action.typeStart.getType() == last.typeEnd.getType())
						actions.get(action.type).add(action);
				}
			}

			Random random = new Random();

			ArrayList<TTSAction> posPreferred = null, posShorter = null;
			if (actions.get(informationLevel).size() > 0) {
				posPreferred = actions.get(informationLevel);
			} else {
				for (int distance = 1; distance < MessageInformationLevel.values().length; distance++) {
					MessageInformationLevel lowerLevel = MessageInformationLevel.fromInteger(4 - s.speed - distance);
					MessageInformationLevel higherLevel = MessageInformationLevel.fromInteger(4 - s.speed + distance);
					if (actions.get(lowerLevel).size() > 0) {
						posPreferred = actions.get(lowerLevel);
					}
					else if (actions.get(higherLevel).size() > 0) {
						posPreferred = actions.get(higherLevel);
					}
				}
			}

			for (int i = 1; i < MessageInformationLevel.values().length; i++) {
				if (actions.get(MessageInformationLevel.fromInteger(i)).size() > 0) {
					posShorter = actions.get(MessageInformationLevel.fromInteger(i));
				}
			}
			
			if (posPreferred == null) return null;
			if (posShorter == null) posShorter = new ArrayList<TTSAction>();
			
			// now we find a pair of preferred and shorter message with the same start and end types.
			HashMap<TTSAction, TTSAction> mapping = new HashMap<TTSAction, TTSAction>();
			for (TTSAction pref : posPreferred) {
				boolean put = false;
				for (TTSAction m : posShorter) {
					if (pref.typeStart != m.typeStart || pref.typeEnd != m.typeEnd)
						continue;
					mapping.put(pref, m);
					put = true;
					break;
				}
				if (!put) mapping.put(pref, null);
			}
			
			if (mapping.size() == 0) return null;
			
			int chosen = random.nextInt(mapping.size());
			Map.Entry<TTSAction, TTSAction> result = (Map.Entry<TTSAction, TTSAction>) mapping.entrySet().toArray()[chosen];
			
			CarChase.log("CHOSE", result.getKey().text, result.getValue().text);

			return new CarChaseArticulatable(result.getKey(), result.getValue(), optional);
		}

		private void applyJunction(WorldPoint point, Street street, StringDict replace, int direction, ArrayList<String> streetNamesCrossNextPoint, boolean was) {
			String prefix = was ? "WAS" : "IS";
			String prevPrefix = was ? "PREV" : "";
			boolean isEndOfStreet = street.fetchNextPoint(point, direction) == null;

			int isJunction = 0;
			if (streetNamesCrossNextPoint.size() == 2 && streetNamesCrossNextPoint.indexOf(street.name) >= 0){
				isJunction = isEndOfStreet ? 2 : 1;

				int indexOfOther = 1 - streetNamesCrossNextPoint.indexOf(street.name);
				assert streetNamesCrossNextPoint.indexOf(street.name) >= 0 : "Something somewhere went terribly wrong: " + streetNamesCrossNextPoint.toString() + street.name;
				String streetName = streetNamesCrossNextPoint.get(indexOfOther);
				Street crossStreet = CarChase.get().world().streets.get(streetName);
				int indexInCross = crossStreet.streetPoints.indexOf(point.name);
				if (indexInCross <= 0 || indexInCross >= crossStreet.streetPoints.size() - 1)
					isJunction = 0;
				else {
					StreetReplacement streetRpl = streetNames.get(streetName);
					if (streetRpl == null) streetRpl = new StreetReplacement(streetName);
					replace.set("*INT" + prevPrefix + "JUNCTIONSTREET", streetName);
					replace.set("*" + prevPrefix + "JUNCTIONSTREET", streetRpl.name);
					replace.set("*FLEX1" + prevPrefix + "JUNCTIONSTREET", streetRpl.flex1);
					replace.set("*FLEX2" + prevPrefix + "JUNCTIONSTREET", streetRpl.flex2);
				}
			}
			replace.set("*" + prefix + "JUNCTION", "" + isJunction);
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

	public static class CarChaseArticulatable extends Articulatable {
		private TTSAction preferred, shorter;
		private boolean optional;

		public CarChaseArticulatable(TTSAction preferred, TTSAction shorter, boolean optional) {
			this.preferred = preferred;
			this.shorter = shorter;
			this.optional = optional;
		}

		public String getPreferredText() {
			return preferred.text;
		}

		public String getShorterText() {
			if (shorter == null) return null;
			return shorter.text;
		}

		public boolean isOptional() {
			return optional;
		}

		public boolean canFollowOnShorterText(Articulatable next) {
			if (!(next instanceof CarChaseArticulatable))
				return false;
			if (shorter == null)
				return false;
			return true; // We only use shorter versions, if they are compatible.
		}

		public String toString() {
			return "----\n--pr-" + preferred.text + "\n--sh-" + (shorter == null ? "null" : shorter.text);
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
