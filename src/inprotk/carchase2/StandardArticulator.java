package inprotk.carchase2;

import java.util.Collection;
import java.util.List;

import inprotk.carchase2.Articulator;
import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.processor.SynthesisModule;
import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.IU;
import inprotk.carchase2.CarChaseTTS.TTSAction;

public class StandardArticulator extends Articulator {
	private final SynthesisModule synthesisModule;
	private final MyIUSource myIUSource;

	public StandardArticulator(DispatchStream dispatcher) {
		super(dispatcher);
		synthesisModule = new SynthesisModule(dispatcher);
		myIUSource = new MyIUSource();
		myIUSource.addListener(synthesisModule);
	}

	public void say(TTSAction action) {
		myIUSource.say(action);
	}

	public TTSAction getLastUpcoming() {
		return null;
	}

	public TTSAction getLast() {
		return null;
	}

	public boolean isSpeaking() {
		return false;
	}

	public void autoRemoveUpcoming() {}
	
	private class MyIUSource extends IUModule {
		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			throw new org.apache.commons.lang.NotImplementedException("StandardArticulator.MyIUSource is an IU source, it hence ignores its left buffer.");
		}
		public void say(TTSAction action) {
			String text = action.text;
			rightBuffer.addToBuffer(new ChunkIU(text));
			rightBuffer.notify(iulisteners);
		}
	}
}
