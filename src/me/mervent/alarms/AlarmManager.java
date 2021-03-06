package me.mervent.alarms;

import haven.Audio;
import haven.Gob;
import haven.GobTag;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class AlarmManager {
    private static HashMap<String, Alarm> alarms = new HashMap<>();
    private static HashSet<Long> played = new HashSet<Long>();
    
    public static void init() {
	load();
    }
    
    // Play an alarm for gob with resname, if it has one
    public static boolean play(String resname, Gob gob) {
	Alarm al = alarms.get(resname);

	// prevents playing alarms for friendly players
	if (Objects.equals(resname, "gfx/borka/body" ) && !gob.is(GobTag.FOE)) {
	    return false;
	}
	if(al != null && !played.contains(gob.id) && (!al.knocked || !gob.anyOf(GobTag.DEAD, GobTag.KO))) {
	    al.play();
	    played.add(gob.id);
	    return true;
	} else {
	    return false;
	}
    }
    
    // Load settings from file or use defaults if file does not exist
    public static void load() {
	alarms.clear();
	File config = new File("alarms-user.cfg");
	if(!config.exists()) {
	    defaultSettings();
	} else {
	    loadFromFile(config);
	}
    }
    
    // Load config from the given file
    private static void loadFromFile(File config) {
	try {
	    for(String s : Files.readAllLines(Paths.get(config.toURI()), StandardCharsets.UTF_8)) {
		String[] split = s.split("(;)");
		if(!alarms.containsKey(split[0]))
		    alarms.put(split[0], new Alarm(split[1], Integer.parseInt(split[2]), Boolean.parseBoolean(split[3])));
	    }
	} catch(IOException e) {
	    e.printStackTrace();
	}
    }
    
    // Loads settings from the list
    public static void load(AlarmWindow.AlarmList list) {
	alarms.clear();
	for(AlarmWindow.AlarmItem ai : list.items) {
	    alarms.put(ai.getGobResname(), new Alarm(ai.getAlarmFilename(), ai.getVolume(), ai.getKnocked()));
	}
    }
    
    // Save current settings to file
    public static void save() {
	try {
	    BufferedWriter bw = Files.newBufferedWriter(Paths.get(new File("alarms-user.cfg").toURI()), StandardCharsets.UTF_8);
	    for(Map.Entry<String, Alarm> e : alarms.entrySet()) {
		bw.write(e.getKey() + ";" + e.getValue().filePath + ";" + e.getValue().volume+";"+e.getValue().knocked+"\n");
	    }
	    bw.flush();
	    bw.close();
	} catch(IOException e) {
	    e.printStackTrace();
	}
    }
    
    public static AlarmWindow.AlarmItem[] getAlarmItems() {
	AlarmWindow.AlarmItem[] alarmItems = new AlarmWindow.AlarmItem[alarms.size()];
	Iterator<Map.Entry<String, Alarm>> it = alarms.entrySet().iterator();
	for(int i=0; i<alarmItems.length; i++) {
	    Map.Entry<String, Alarm> e = it.next();
	    alarmItems[i] = new AlarmWindow.AlarmItem(e.getKey(), e.getValue().filePath, e.getValue().volume, e.getValue().knocked);
	}
	return alarmItems;
    }
    
    // Loads the default settings
    public static void defaultSettings() {
	alarms.clear();
	loadFromFile(new File("alarms-default.cfg"));
    }
    
    public static class Alarm {
	public String filePath;
	public int volume;
	public boolean knocked;
	
	public Alarm(String filePath, int volume, boolean knocked) {
	    this.filePath = filePath;
	    this.volume = volume;
	    this.knocked = knocked;
	}
	
	public void play() {
	    File file = new File("alarms/" + filePath);
	    if(!file.exists()) {
		System.out.println("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
		return;
	    }
	    try {
		AudioInputStream in = AudioSystem.getAudioInputStream(file);
		AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
		AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
		Audio.CS klippi = new Audio.PCMClip(pcmStream, 2);
		((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, volume/50.0));
	    } catch(UnsupportedAudioFileException e) {
		e.printStackTrace();
	    } catch(IOException e) {
		e.printStackTrace();
	    }
	}
    }
}
