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
//import org.rusherhack.client.api.events.render.EventRender2D;
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

	private final Set<BlockPos> spawnerPositions = Collections.synchronizedSet(new HashSet<>());
	private final Set<BlockPos> chestPositions = Collections.synchronizedSet(new HashSet<>());
	Set<LevelChunk> processedChunks =  Collections.synchronizedSet(new HashSet<>());
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
		Set<LevelChunk> currentChunks = new HashSet<>();
		List<LevelChunk> chunks = WorldUtils.getChunks();
		for (LevelChunk chunk : chunks) {
			currentChunks.add(chunk);
			if (processedChunks.contains(chunk)) continue;
			chunk.getBlockEntities().values().stream()
					.filter(be -> be.getBlockState().getBlock() == Blocks.SPAWNER)
					.forEach(blockEntity -> {

					SpawnerBlockEntity spawner = (SpawnerBlockEntity) blockEntity;
					int spawnDelay = getSpawnerDelay(spawner);
					BlockPos pos = spawner.getBlockPos();

					if (!spawnerPositions.contains(pos) && spawnDelay != 20) {
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

						spawnerPositions.add(pos);
						BlockPos chestPos = getChestPos(pos);
						if (chestPos != null) {
							chestPositions.add(chestPos);
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
							if (chatNotify.getValue()) {
								synchronized (ChatUtils.class) {
									ChatUtils.print(String.format(
											"There is a chest nearby an activated spawner! Block Position: x:%d, y:%d, z:%d",
											(int) chestPos.getCenter().x, (int) chestPos.getCenter().y, (int) chestPos.getCenter().z
									));
								}
							}

						}
					}
			});
			processedChunks.add(chunk);
		}
		processedChunks.retainAll(currentChunks);
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

	public BlockPos getChestPos(BlockPos pos) {
		for (int x = -16; x < 17; x++) {
			for (int y = -16; y < 17; y++) {
				for (int z = -16; z < 17; z++) {
					BlockPos bpos = new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    assert mc.level != null;
                    if (mc.level.getBlockState(bpos).getBlock() == Blocks.CHEST) {
						return bpos;
					}
				}
			}
		}
		return null;
	}
	@Subscribe
	private void onRender3D(EventRender3D event) {
		final IRenderer3D renderer = event.getRenderer();

		final int colorSpawner = ColorUtils.transparency(this.spawnerColor.getValueRGB(), 0.5f); //fill colors look better when the alpha is not 100%
		final int colorChest = ColorUtils.transparency(this.chestColor.getValueRGB(), 0.5f); //fill colors look better when the alpha is not 100%

		//begin renderer
		renderer.begin(event.getMatrixStack());

		double distance = 75;
		renderer.setLineWidth((int)distance);
		if (chestTracers.getValue()) {
			synchronized (chestPositions) {
				for (BlockPos chest : chestPositions) {
					Vec3 cameraPos = EntityUtils.interpolateEntityVec(event.getCamera().getEntity(), event.getPartialTicks());
					Vec3 crosshairPosition = cameraPos.add(
							event.getCamera().getLookVector().x * distance,
							event.getCamera().getLookVector().y * distance,
							event.getCamera().getLookVector().z * distance);
					renderer.drawLine(chest.getCenter(), crosshairPosition, colorChest);
				}
			}
		}

		if (spawnerTracers.getValue()) {
			synchronized (spawnerPositions) {
				for (BlockPos spawner : spawnerPositions) {
					Vec3 cameraPos = EntityUtils.interpolateEntityVec(event.getCamera().getEntity(), event.getPartialTicks());
					Vec3 crosshairPosition = cameraPos.add(
							event.getCamera().getLookVector().x * distance,
							event.getCamera().getLookVector().y * distance,
							event.getCamera().getLookVector().z * distance);
					renderer.drawLine(spawner.getCenter(), crosshairPosition, colorSpawner);
				}
			}
		}
		renderer.setLineWidth(1);

		if (blockRender.getValue()) {
			if (!chestsOnly.getValue()) {
				synchronized (spawnerPositions) {
					for (BlockPos spawner : spawnerPositions) {
						renderer.drawBox(spawner, true, true, colorSpawner);
					}
				}
			}
			synchronized (chestPositions) {
				for (BlockPos chest : chestPositions) {
					renderer.drawBox(chest, true, true, colorChest);
				}
			}
		}

		//end renderer
		renderer.end();
	}
	@Override
	public void onEnable() {
		super.onEnable();
		spawnerPositions.clear();
		chestPositions.clear();
		processedChunks.clear();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		spawnerPositions.clear();
		chestPositions.clear();
		processedChunks.clear();
	}


}


