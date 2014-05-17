package inprotk.carchase2;

public interface ConfigurationUpdateListener {
	public void configurationUpdated(int type);
	
	public static final int PATH_CHANGED = 0;
	public static final int SPEED_CHANGED = 1;
	public static final int PATH_COMPLETED = 2;
}
