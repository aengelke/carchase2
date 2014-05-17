package inprotk.carchase2;

import inpro.util.TimeUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import inprotk.carchase2.CarChase;
import inprotk.carchase2.CarChaseTTS;
import inprotk.carchase2.Configuration;
import inprotk.carchase2.InteractiveConfiguration;
import inprotk.carchase2.World;

public class CarChase {
	public static final RuntimeException notImplemented = new RuntimeException("Not Implemented.");

	private static CarChase carchase;
	
	private String configSet;
	private World world;
	private Configuration config;
	private CarChaseTTS tts;
	private long startTime;
	
	public void setWorld(World w) {
		world = w;
	}
	public World world() {
		return world;
	}
	
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	public Configuration configuration() {
		return config;
	}
	
	public CarChaseTTS tts() {
		return tts;
	}
	public void setTTS(CarChaseTTS tts) {
		this.tts = tts;
	}
	
	public String getConfigSet() {
		return configSet;
	}
	
	public String getConfigFilename(String name) {
		return getFilename("configs2/" + configSet + "/" + name);
	}
	
	public void init(World w, CarChaseTTS tts, Configuration c) {
		world = w;
		this.tts = tts;
		config = c;
	}
	
	public void init(String name) {
		configSet = name;
		world = new World(getConfigFilename("world.txt"));
		config = new InteractiveConfiguration(getConfigFilename("config.txt"));
		tts = new CarChaseTTS(getConfigFilename("messages.txt"));
	}
	
	public boolean isInteractive() {
		return config instanceof InteractiveConfiguration;
	}
	 
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	public int getTime() {
		return (int) (System.currentTimeMillis() - startTime);
	}
	
	public double getInproTimeInSeconds() {
		return (System.currentTimeMillis() - TimeUtil.startupTime) / TimeUtil.SECOND_TO_MILLISECOND_FACTOR;
	}
	
	private CarChase() {}
	
	public static CarChase get() {
		if (carchase == null) carchase = new CarChase();
		return carchase;
	}
	
	// Helper methods.

	public static String getFilename(String s) {
		System.err.println(s);
		return CarChase.class.getResource(s).getPath().replaceAll("%20", " ");
	}
	
	public static void log(Object ... o) {
		for (Object ob : o) {
			if (ob == null) System.out.print("null");
			else System.out.print(ob.toString());
			System.out.print(" ");
		}
		System.out.println();
	}
	
	public static String[] readLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        ArrayList<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        return lines.toArray(new String[lines.size()]);
    }
}
