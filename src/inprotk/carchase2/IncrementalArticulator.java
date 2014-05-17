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
import inprotk.carchase2.CarChaseTTS.TTSAction;
import inprotk.carchase2.CarChase;
import inprotk.carchase2.StandardArticulator;

public class IncrementalArticulator extends StandardArticulator {
	
	private final SynthesisModule synthesisModule;
	private final DispatchStream dispatcher;
	private final MyIUSource myIUSource;
	
	public IncrementalArticulator(DispatchStream dispatcher) {
		super(dispatcher);
		this.dispatcher = dispatcher;
		synthesisModule = new SynthesisModule(dispatcher);
		myIUSource = new MyIUSource();
		myIUSource.addListener(synthesisModule);
		myIUSource.addListener(new CurrentHypothesisViewer().show());
		synthesisModule.addListener(new CurrentHypothesisViewer().show());
	}
	
	@Override
	public void say(TTSAction action) {
		// I now assume that the action is chosen intelligent.
		// Therefore, here the continuation WILL NOT be appended,
		// but rather the caller has to check whether he can.
		
		if (action == null) return;
		myIUSource.say(action);
	}
	
	public TTSAction getLastUpcoming() {
		IUextended iue = myIUSource.getLastUpcoming();
		if (iue == null) return null;
		return iue.action;
	}
	
	public TTSAction getLast() {
		IUextended iue = myIUSource.getLast();
		if (iue == null) return null;
		return iue.action;
	}
	
	public boolean isSpeaking() {
		//spokeInLastSeconds(2);
		return dispatcher.isSpeaking();
	}
	
	public boolean spokeInLastSeconds(double seconds) {
		IUextended iue = myIUSource.getLast();
		if (iue == null) return dispatcher.isSpeaking();
		double timeDelta = iue.inner.endTime() - CarChase.get().getInproTimeInSeconds() + seconds;
		CarChase.log(iue.inner.startTime(), iue.inner.endTime(), timeDelta);
		if (true) throw CarChase.notImplemented;
		return dispatcher.isSpeaking() || timeDelta >= 0;
	}
	
	public void printUpcoming() {
		ArrayList<IUextended> iues = myIUSource.getAllUpcoming();
		CarChase.log("\n", "Upcoming:");
		for (IUextended iue : iues) {
			CarChase.log(" - Upcoming IU", iue.message, iue.duration, iue.hasHesitation);
		}
	}
	
	public void autoRemoveUpcoming() {
		ArrayList<IUextended> iues = myIUSource.getAllUpcoming();
		double durationOverAll = 0;
		double durationCanRevoked = 0;
		for (IUextended iue : iues) {
			durationOverAll += iue.inner.duration();
			if (iue.action.optional)
				durationCanRevoked += iue.inner.duration();
		}
		if (durationOverAll > 2)
			for (IUextended iue : iues)
				if (iue.action.optional)
					myIUSource.revoke(iue);
		CarChase.log("Reduced distance from", durationOverAll, "to", durationOverAll - durationCanRevoked);
		//throw CarChase.notImplemented;
	}
	
	private class IUextended {
		private TTSActionIU inner;
		private HesitationIU hesitationIU;
		private String message;
		private double duration;
		private boolean hasHesitation;
		private TTSAction action;
		
		private IUextended(TTSActionIU iu, HesitationIU hesitation) {
			this.inner = iu;
			this.hasHesitation = hesitation != null;
			this.hesitationIU = hesitation;
			this.action = inner.action;
			this.message = iu.getWord();
			this.duration = iu.duration();
		}
		
		public String toString() {
			return message + "," + hasHesitation;// + "," + inner.toString();
		}
	}
	
	private class MyIUSource extends IUModule {

		protected void leftBufferUpdate(Collection<? extends IU> ius,
				List<? extends EditMessage<? extends IU>> edits) {
			throw CarChase.notImplemented;
		}
		
		public IUextended getLastUpcoming() {
			IUextended lastIU = getLast();
			if (lastIU == null) return null;
			if (lastIU.inner.getProgress() != Progress.UPCOMING) return null;
			return lastIU;
		}
		
		public IUextended getLast() {
			if (rightBuffer.getBuffer().size() == 0) return null;
			IU lastIU = rightBuffer.getBuffer().get(rightBuffer.getBuffer().size() - 1);
			
			boolean hesitation = lastIU instanceof HesitationIU;
			HesitationIU hesIU = (HesitationIU) (hesitation ? lastIU : null);
			if (hesitation)
				lastIU = rightBuffer.getBuffer().get(rightBuffer.getBuffer().size() - 2);
			
			TTSActionIU chunk = (TTSActionIU) lastIU;
			
			return new IUextended(chunk, hesIU);
		}
		
		public void revokeAllUpcoming() {
			ArrayList<IUextended> ius = getAllUpcoming();
			for (IUextended iue : ius) {
				if (iue.hasHesitation)
					rightBuffer.editBuffer(new EditMessage<IU>(EditType.REVOKE, iue.hesitationIU));
				rightBuffer.editBuffer(new EditMessage<IU>(EditType.REVOKE, iue.inner));
			}
			rightBuffer.notify(iulisteners);
		}
		
		public void revoke(IUextended iue) {
			if (iue.hasHesitation)
				rightBuffer.editBuffer(new EditMessage<IU>(EditType.REVOKE, iue.hesitationIU));
			rightBuffer.editBuffer(new EditMessage<IU>(EditType.REVOKE, iue.inner));
		}
		
		public ArrayList<IUextended> getAllUpcoming() {
			ArrayList<IUextended> upcoming = new ArrayList<IUextended>();
			for (int i = 0; i < rightBuffer.getBuffer().size(); i++)  {
				IU lastIU = rightBuffer.getBuffer().get(i); // Its a ChunkIU
				
				if (lastIU.getProgress() != Progress.UPCOMING) continue;
				boolean hesitation = lastIU instanceof HesitationIU;
				HesitationIU hesIU = (HesitationIU) (hesitation ? lastIU : null);
				if (hesitation) {
					lastIU = rightBuffer.getBuffer().get(rightBuffer.getBuffer().size() - 2);
					if (lastIU.getProgress() != Progress.UPCOMING) continue;
				}
				
				TTSActionIU chunk = (TTSActionIU) lastIU;
				
				upcoming.add(new IUextended(chunk, hesIU));
			}
			
			return upcoming;
		}
		
		public void say(TTSAction action) {
			String text = action.text;
			boolean addHesitation = false;
			if (text.matches(".*<hes>$")) {
				text = text.replaceAll(" <hes>$", "");
				addHesitation = true;
			}
			rightBuffer.addToBuffer(new TTSActionIU(text, action));
			if (addHesitation) {
				rightBuffer.addToBuffer(new HesitationIU());
			}
			rightBuffer.notify(iulisteners);
		}
		
	}
	
	/**
	 * A ChunkIU, which contains a TTSAction. Used for getting 
	 * the TTSAction and its type, etc.
	 */
	private static class TTSActionIU extends ChunkIU {
		private TTSAction action;
		public TTSActionIU(String chunkText, TTSAction action) {
			super(chunkText);
			this.action = action;
		}
	}

}