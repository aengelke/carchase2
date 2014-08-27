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

public class StandardArticulator extends Articulator {
	private final CarChaseIUSource ccIUSource;

	public StandardArticulator(DispatchStream dispatcher) {
		super(dispatcher);
		synthesisModule = new SynthesisModule(dispatcher);
		ccIUSource = new CarChaseIUSource();
		ccIUSource.addListener(synthesisModule);
	}

	public void say(Articulatable action) {
		ccIUSource.say(action);
	}

	public Articulatable getLast() {
		return null;
	}

	public boolean isSpeaking() {
		return false;
	}

	public void reduceOffset() {}
	
	private class CarChaseIUSource extends IUModule {
		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			throw CarChase.notImplemented;
		}
		public void say(Articulatable action) {
			if (action.isOptional() && dispatcher.isSpeaking())
				return;
			String text = action.getPreferredText();
			rightBuffer.addToBuffer(new ChunkIU(text));
			rightBuffer.notify(iulisteners);
		}
	}
}
