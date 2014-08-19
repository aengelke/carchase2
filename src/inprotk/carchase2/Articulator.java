package inprotk.carchase2;

import inprotk.carchase2.CarChase;
import inpro.audio.DispatchStream;
import inpro.incremental.processor.SynthesisModule;
import inprotk.carchase2.CarChaseTTS.TTSAction;

public abstract class Articulator {
	protected DispatchStream dispatcher;
	protected SynthesisModule synthesisModule;
	public Articulator(DispatchStream dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	public MyCurrentHypothesisViewer getHypothesisViewer() {
		MyCurrentHypothesisViewer v = new MyCurrentHypothesisViewer();
		synthesisModule.addListener(v);
		return v;
	}
	
	public abstract void say(Articulatable action);

	public final static int getGlobalTime() {
		return CarChase.get().getTime();
	}

	public void printUpcoming() {}
	public abstract Articulatable getLastUpcoming();
	public abstract Articulatable getLast();
	public abstract boolean isSpeaking();
	public abstract void reduceOffset();
	
	public static abstract class Articulatable {
		public abstract String getPreferredText();
		public abstract String getShorterText();
		public abstract boolean isOptional();
		public abstract boolean canFollowOnShorterText(Articulatable next);
		public abstract boolean canFollowOnPreferredText(Articulatable next);
	}
}
