package inprotk.carchase2;

import inprotk.carchase2.CarChase;
import inpro.audio.DispatchStream;
import inprotk.carchase2.CarChaseTTS.TTSAction;

public abstract class Articulator {
	protected DispatchStream dispatcher;
	public Articulator(DispatchStream dispatcher) {
		this.dispatcher = dispatcher;
	}
	public abstract void say(Articulatable action);

	public final static int getGlobalTime() {
		return CarChase.get().getTime();
	}

	public void printUpcoming() {}
	public abstract Articulatable getLastUpcoming();
	public abstract Articulatable getLast();
	public abstract boolean isSpeaking();
	public abstract void autoRemoveUpcoming();
	
	public static interface Articulatable {
		public String getPreferredText();
		public String getShorterText();
		public String getLongerText();
		public boolean isOptional();
	}
}
