package org.untab;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

/**
 * ActivatedSpawnerDetector
 *
 * @author untab
 */
public class Main extends Plugin {
	
	@Override
	public void onLoad() {
		
		//logger
		this.getLogger().info("ActivatedSpawnerDetector loaded");
		
		//creating and registering a new module
		final ActivatedSpawnerDetector ActivatedSpawnerDetector = new ActivatedSpawnerDetector();
		RusherHackAPI.getModuleManager().registerFeature(ActivatedSpawnerDetector);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("ActivatedSpawnerDetector unloaded");
	}
	
}