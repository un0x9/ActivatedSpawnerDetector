package org.untab;

import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import net.minecraft.world.phys.Vec3;
import org.rusherhack.client.api.utils.WorldUtils;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.client.api.events.render.EventRender3D;
import org.rusherhack.client.api.utils.EntityUtils;
import org.rusherhack.client.api.render.IRenderer3D;
import org.rusherhack.client.api.setting.ColorSetting;
import org.rusherhack.core.utils.ColorUtils;

import org.joml.Vector3f;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;


/**
 * ActivatedSpawnerDetector
 *
 * @author untab
 * @credits etianl
 */



public class ActivatedSpawnerDetector extends ToggleableModule {
	private final BooleanSetting chestsOnly = new BooleanSetting("Log Chests Only", "Only sends a message if a chest is found within a 16 block radius", false);
	private final BooleanSetting chestTracers = new BooleanSetting("Chest Tracers", "Tracers for Chests", false);
	private final BooleanSetting spawnerTracers = new BooleanSetting("Spawner Tracers", "Tracers for Spawners", false);
	private final BooleanSetting soundAlert = new BooleanSetting("Sound alert", "Make alert noise", true);
	private final BooleanSetting chatNotify = new BooleanSetting("ChatNotify", "Notifies the chat", true);
	private final BooleanSetting blockRender = new BooleanSetting("Block Render", "Renders blocks", true);
	private final ColorSetting chestColor = new ColorSetting("Chest Color", Color.PINK)
			//set whether alpha is enabled in the color picker
			.setAlphaAllowed(false)
			//sync the color with the theme color
			.setThemeSync(true);
	private final ColorSetting spawnerColor = new ColorSetting("Spawner Color", Color.RED)
			.setAlphaAllowed(false)
			//sync the color with the theme color
			.setThemeSync(true);

	public class ChunkDetectorGroup {
		public final Set<BlockPos> spawnerPositions = Collections.synchronizedSet(new HashSet<>());
		public final Set<BlockPos> chestPositions = Collections.synchronizedSet(new HashSet<>());
    }

	Map<LevelChunk, ChunkDetectorGroup> processedChunks = Collections.synchronizedMap(new HashMap<>());
    /**
	 * Constructor
	 */
	public ActivatedSpawnerDetector() {
		super("ActivatedSpawnerDetector", "", ModuleCategory.CLIENT);
		this.registerSettings(this.blockRender, this.spawnerColor, this.chestColor, this.chestsOnly, this.chestTracers, this.spawnerTracers, this.chatNotify, this.soundAlert);
	}

	@Subscribe
	public void onUpdate(EventUpdate event) {
		// check enabled
		if (!this.isToggled()) {
			return;
		}
        assert mc.player != null;
		Map<LevelChunk, ChunkDetectorGroup> currentChunks = Collections.synchronizedMap(new HashMap<>());
		List<LevelChunk> chunks = WorldUtils.getChunks();
		for (LevelChunk chunk : chunks) {
			ChunkDetectorGroup newCollection = new ChunkDetectorGroup();
			currentChunks.put(chunk, newCollection);
			if (processedChunks.containsKey(chunk)) continue;
			chunk.getBlockEntities().values().stream()
					.filter(be -> be.getBlockState().getBlock() == Blocks.SPAWNER)
					.forEach(blockEntity -> {

					SpawnerBlockEntity spawner = (SpawnerBlockEntity) blockEntity;
					int spawnDelay = getSpawnerDelay(spawner);
					BlockPos pos = spawner.getBlockPos();

					if (spawnDelay != 20) {
                        assert mc.level != null;
                        if (mc.level.dimension().registryKey() == Level.NETHER.registryKey() && spawnDelay == 0)
							return;
						if (!chestsOnly.getValue()) {

							if (chatNotify.getValue()) {
								synchronized (ChatUtils.class) {
									ChatUtils.print(String.format(
											"Detected Activated Spawner! Block Position: x:%d, y:%d, z:%d",
											(int) pos.getCenter().x, (int) pos.getCenter().y, (int) pos.getCenter().z
									));
								}
							}

							if (soundAlert.getValue()) {
								mc.execute(() -> mc.level.playLocalSound(
										mc.player.getX(),
										mc.player.getY(),
										mc.player.getZ(),
										SoundEvents.EXPERIENCE_ORB_PICKUP,
										SoundSource.PLAYERS,
										1.0F,
										1.0F,
										false
								));
							}
						}

						newCollection.spawnerPositions.add(pos);
						List<BlockPos> chestPos = getChestPos(pos);
						if (!chestPos.isEmpty()) {
							if (soundAlert.getValue()) {
								mc.execute(() -> mc.level.playLocalSound(
										mc.player.getX(),
										mc.player.getY(),
										mc.player.getZ(),
										SoundEvents.EXPERIENCE_ORB_PICKUP,
										SoundSource.PLAYERS,
										1.0F,
										1.0F,
										false
								));
							}
							for (BlockPos chest : chestPos) {
								newCollection.chestPositions.add(chest);

								if (chatNotify.getValue()) {
									synchronized (ChatUtils.class) {
										ChatUtils.print(String.format(
												"There is a chest nearby an activated spawner! Block Position: x:%d, y:%d, z:%d",
												(int) chest.getCenter().x, (int) chest.getCenter().y, (int) chest.getCenter().z
										));
									}
								}
							}
						}
					}
			});
			processedChunks.put(chunk, newCollection);
		}
		processedChunks.entrySet().removeIf(entry -> !currentChunks.containsKey(entry.getKey()));
	}

	private int getSpawnerDelay(SpawnerBlockEntity spawner) {
		try {
			Object spawnerObject = spawner.getSpawner();
			Field privateField = spawnerObject.getClass().getSuperclass().getDeclaredField("field_9154"); // spawnDelay field
			privateField.setAccessible(true);
			return (int) privateField.get(spawnerObject);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			ChatUtils.print("Error when getting SpawnDelay!");
			return 20;
		}
	}

	public List<BlockPos> getChestPos(BlockPos pos) {
		List<BlockPos> chests = new ArrayList<BlockPos>();
		for (int x = -16; x < 17; x++) {
			for (int y = -16; y < 17; y++) {
				for (int z = -16; z < 17; z++) {
					BlockPos bpos = new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    assert mc.level != null;
                    if (mc.level.getBlockState(bpos).getBlock() == Blocks.CHEST) {
						chests.add(bpos);
					}
				}
			}
		}
		return chests;
	}
	@Subscribe
	private void onRender3D(EventRender3D event) {
		final IRenderer3D renderer = event.getRenderer();

		final int colorSpawner = ColorUtils.transparency(this.spawnerColor.getValueRGB(), 0.5f); //fill colors look better when the alpha is not 100%
		final int colorChest = ColorUtils.transparency(this.chestColor.getValueRGB(), 0.5f); //fill colors look better when the alpha is not 100%

		//begin renderer
		renderer.begin(event.getMatrixStack());
		synchronized (processedChunks) { // Ensure thread-safety while accessing the map
			for (Map.Entry<LevelChunk, ChunkDetectorGroup> entry : processedChunks.entrySet()) {
				ChunkDetectorGroup chunkGroup = entry.getValue();

				// Iterate over all spawnerPositions in the current ChunkDetectorGroup
				synchronized (chunkGroup.spawnerPositions) { // Ensure thread-safety for spawnerPositions set
					for (BlockPos spawner : chunkGroup.spawnerPositions) {
						if (spawnerTracers.getValue())
							drawTracers(event, spawner.getCenter(), colorSpawner);
						if (blockRender.getValue() && !chestsOnly.getValue())
							renderer.drawBox(spawner, true, true, colorSpawner);
					}
				}
				synchronized (chunkGroup.chestPositions) { // Ensure thread-safety for spawnerPositions set
					for (BlockPos chest : chunkGroup.chestPositions) {
						if (chestTracers.getValue())
							drawTracers(event, chest.getCenter(), colorChest);
						if (blockRender.getValue())
							renderer.drawBox(chest, true, true, colorChest);
					}
				}

			}
		}

		//end renderer
		renderer.end();
	}
	public boolean isBlockPosOutsideRenderDistance(BlockPos blockPos) {
        assert mc.player != null;
        Vec3 playerPos = mc.player.getEyePosition();
		int renderDistance = mc.options.renderDistance().get();
		int maxDistance = renderDistance * 16;  // Each chunk is 16x16 blocks
		double distance = playerPos.distanceToSqr(blockPos.getCenter());
		return distance > maxDistance * maxDistance;
	}
	private void drawTracers(EventRender3D event, Vec3 pos, int color) {
		final IRenderer3D renderer = event.getRenderer();
		double distance = 75;
		renderer.setLineWidth((int)distance);
		Vec3 cameraPos = EntityUtils.interpolateEntityVec(event.getCamera().getEntity(), event.getPartialTicks());
		Vec3 crosshairPosition = cameraPos.add(
				event.getCamera().getLookVector().x * distance,
				event.getCamera().getLookVector().y * distance,
				event.getCamera().getLookVector().z * distance);
		renderer.drawLine(pos, crosshairPosition, color);
		renderer.setLineWidth(1);
	}
	@Override
	public void onEnable() {
		super.onEnable();
		processedChunks.clear();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		processedChunks.clear();
	}


}


