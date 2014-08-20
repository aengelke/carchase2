package inprotk.carchase2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import inpro.audio.DispatchStream;
import inpro.incremental.IUModule;
import inpro.incremental.processor.SynthesisModule;
import inpro.incremental.sink.CurrentHypothesisViewer;
import inpro.incremental.unit.ChunkIU;
import inpro.incremental.unit.EditMessage;
import inpro.incremental.unit.EditType;
import inpro.incremental.unit.HesitationIU;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.IU.Progress;
import inprotk.carchase2.CarChase;
import inprotk.carchase2.StandardArticulator;

public class IncrementalArticulator extends StandardArticulator {
	private final CarChaseIUSource ccIUSource;
	
	private ArrayList<Articulatable> articulates;
	
	public IncrementalArticulator(DispatchStream dispatcher) {
		super(dispatcher);
		this.dispatcher = dispatcher;
		synthesisModule = new SynthesisModule(dispatcher);
		ccIUSource = new CarChaseIUSource();
		ccIUSource.addListener(synthesisModule);
		//ccIUSource.addListener(new MyCurrentHypothesisViewer().show());
		synthesisModule.addListener(new MyCurrentHypothesisViewer().show());
		articulates = new ArrayList<Articulatable>();
	}
	
	@Override
	public void say(Articulatable action) {
		// I now assume that the action is chosen intelligent.
		// Therefore, here the continuation WILL NOT be appended,
		// but rather the caller has to check whether he can.
		
		if (action == null) return;
		articulates.add(action);
		ccIUSource.say(action, false, false);
	}
	
	public Articulatable getLast() {
		ChunkIU iu = ccIUSource.getLast();
		if (iu == null) return null;
		return (Articulatable) iu.getUserData("articulatable");
	}
	
	public boolean isSpeaking() {
		//spokeInLastSeconds(2);
		return dispatcher.isSpeaking();
	}
	
	public boolean spokeInLastSeconds(double seconds) {
		ChunkIU iu = ccIUSource.getLast();
		if (iu == null) return dispatcher.isSpeaking();
		double timeDelta = iu.endTime() - CarChase.get().getInproTimeInSeconds() + seconds;
		CarChase.log(iu.startTime(), iu.endTime(), timeDelta);
		if (true) throw CarChase.notImplemented;
		return dispatcher.isSpeaking() || timeDelta >= 0;
	}
	
	public void reduceOffset() {
		ccIUSource.beginChanges();
		ArrayList<IU> ius = ccIUSource.revokeUpcoming();
		for (IU iu : ius) {
			if (!(iu instanceof ChunkIU)) continue;
			Articulatable articulatable = (Articulatable) iu.getUserData("articulatable");
			if (articulatable.isOptional()) {
				articulates.remove(articulatable);
			} else if (articulatable.getShorterText() != null) {
				articulatable.setUseOfShorterText(true);
				ccIUSource.say(articulatable, true, true);
			} else {
				ccIUSource.say(articulatable, false, true);
			}
		}
		ccIUSource.doneChanges();
	}
	
	private class CarChaseIUSource extends IUModule {
		private boolean changing;

		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			throw CarChase.notImplemented;
		}
		
		public ChunkIU getLast() {
			for (int i = rightBuffer.getBuffer().size() - 1; i >= 0; i--) {
				IU lastIU = rightBuffer.getBuffer().get(i);
				if (!(lastIU instanceof ChunkIU)) continue;
				return (ChunkIU) lastIU;
			}
			return null;
		}
		
		public ArrayList<IU> revokeUpcoming() {
			ArrayList<IU> upcoming = new ArrayList<IU>();
			for (IU lastIU : rightBuffer.getBuffer())  {
				if (lastIU.getProgress() != Progress.UPCOMING) continue;
				if (lastIU.isCommitted()) continue;
				upcoming.add((ChunkIU) lastIU);
			}
			for (IU iu : upcoming)
				rightBuffer.editBuffer(new EditMessage<IU>(EditType.REVOKE, iu));
			if (!changing)
				rightBuffer.notify(iulisteners);
			return upcoming;
		}
		
		public void say(Articulatable action, boolean shorter, boolean commit) {
			String text = action.getPreferredText();
			boolean addHesitation = false;
			if (text.matches(".*<hes>$")) {
				text = text.replaceAll(" <hes>$", "");
				addHesitation = true;
			}
			ChunkIU iu = new ChunkIU(text);
			iu.setUserData("articulatable", action);
			rightBuffer.addToBuffer(iu);
			if (addHesitation)
				rightBuffer.addToBuffer(new HesitationIU());
			if (commit)
				rightBuffer.editBuffer(new EditMessage<IU>(EditType.COMMIT, iu));
			if (!changing)
				rightBuffer.notify(iulisteners);
		}
		
		public void beginChanges() {
			changing = true;
		}
		
		public void doneChanges() {
			changing = false;
			rightBuffer.notify(iulisteners);
		}
	}
}