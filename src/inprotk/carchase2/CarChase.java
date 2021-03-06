package inprotk.carchase2;

import inpro.util.TimeUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import inprotk.carchase2.CarChase;
import inprotk.carchase2.CarChaseTTS;
import inprotk.carchase2.Configuration;
import inprotk.carchase2.InteractiveConfiguration;
import inprotk.carchase2.World;

public class CarChase {
	public static final RuntimeException notImplemented = new RuntimeException("Not Implemented.");

	private static CarChase carchase;
	
	private String configSet;
	private String configName;
	private World world;
	private Configuration config;
	private CarChaseTTS tts;
	private long startTime;
	
	private static HashMap<String, String> superConfig; 
	
	static {
		if (superConfig == null) {
			superConfig = new HashMap<String, String>();
			try {
				String[] raw = readLines("inprotk/carchase2/configs2/superconfig.txt");
				int index = 0;
				for (String line : raw) {
					if (index++ == 0) continue;
					line = line.trim();
					if (line.startsWith("#")) continue;
					if (line.equals("")) continue;
					superConfig.put(line.split(":")[0], line.split(":")[1]);
				}
			} catch (Exception e) {
				log("Error reading or parsing Super-Configuration. Full stop.");
				log(e);
				System.exit(-1);
			}
		}
	}
	
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
	
	public String getConfigName() {
		return configName;
	}
	
	public String getConfigFilename(String name) {
		return getFilename("configs2/" + configSet + "/" + name);
	}
	
	public void init(World w, CarChaseTTS tts, Configuration c) {
		world = w;
		this.tts = tts;
		config = c;
	}
	
	private Configuration makeConfig() {
		this.configName = getSuperConfig("config");
		if (getSuperConfig("interactive").equals("true"))
			return new InteractiveConfiguration();
		return new Configuration(getConfigFilename(configName + ".txt"));
	}
	
	public void init(String name) {
		configSet = name;
		world = new World(getConfigFilename("world.txt"));
		config = makeConfig();
		String patternFile = "patterns" + (getSuperConfig("baseline").equals("true") ? "-baseline" : "");
		tts = new CarChaseTTS(getConfigFilename(patternFile + ".txt"));
	}
	
	public boolean isInteractive() {
		return config instanceof InteractiveConfiguration;
	}
	 
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	public float frameRate() {
		if (getSuperConfig("recording").equals("papplet"))
			return 5f;
		return 30f;
	}
	
	public int getTime() {
		return (int) ((System.currentTimeMillis() - startTime) / (30.0 / frameRate()));
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
        InputStreamReader fileReader = new InputStreamReader(new FileInputStream(filename), "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        ArrayList<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        return lines.toArray(new String[lines.size()]);
    }
	
	public static String getSuperConfig(String key) {
		return superConfig.get(key);
	}
}
