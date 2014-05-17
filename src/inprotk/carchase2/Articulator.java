package inprotk.carchase2;

import inprotk.carchase2.CarChase;
import inpro.audio.DispatchStream;
import inprotk.carchase2.CarChaseTTS.TTSAction;

public abstract class Articulator {
	protected DispatchStream dispatcher;
	public Articulator(DispatchStream dispatcher) {
		this.dispatcher = dispatcher;
	}
	public abstract void say(TTSAction action);
	
	public void precompute(TTSAction action) {}

	public final static int getGlobalTime() {
		return CarChase.get().getTime();
	}

	public void printUpcoming() {}
	public abstract TTSAction getLastUpcoming();
	public abstract TTSAction getLast();
	public abstract boolean isSpeaking();
	public abstract void autoRemoveUpcoming();
}
