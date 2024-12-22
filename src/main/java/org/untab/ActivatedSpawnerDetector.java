package org.untab;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockMath;
import net.minecraft.client.renderer.LevelRenderer;

import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;

import java.lang.reflect.Field;
import java.util.*;


/**
 * ActivatedSpawnerDetector
 *
 * @author untab
 * @credits etianl
 */

public class ActivatedSpawnerDetector extends ToggleableModule {
	private final BooleanSetting chestsOnly = new BooleanSetting("Log Chests Only", "Only sends a message if a chest is found within a 16 block radius", false);
	private final Set<BlockPos> spawnerPositions = Collections.synchronizedSet(new HashSet<>());
    /**
	 * Constructor
	 */
	public ActivatedSpawnerDetector() {
		super("ActivatedSpawnerDetector", "", ModuleCategory.CLIENT);
		this.registerSettings(this.chestsOnly);
	}

	@Subscribe
	public void onUpdate(EventUpdate event) {
		// check enabled
		if (!this.isToggled()) {
			return;
		}
		int renderDistance = mc.options.renderDistance().get();
        assert mc.player != null;
        ChunkPos playerChunkPos = new ChunkPos(mc.player.blockPosition());
		for (int chunkX = playerChunkPos.x - renderDistance; chunkX <= playerChunkPos.x + renderDistance; chunkX++) {
			for (int chunkZ = playerChunkPos.z - renderDistance; chunkZ <= playerChunkPos.z + renderDistance; chunkZ++) {
				assert mc.level != null;
				LevelChunk chunk = mc.level.getChunk(chunkX, chunkZ);
				chunk.getBlockEntities().values().parallelStream()
						.filter(be -> be.getBlockState().getBlock() == Blocks.SPAWNER)
						.forEach(blockEntity -> {

						SpawnerBlockEntity spawner = (SpawnerBlockEntity) blockEntity;
						int spawnDelay = getSpawnerDelay(spawner);
						BlockPos pos = spawner.getBlockPos();

						if (!spawnerPositions.contains(pos) && spawnDelay != 20) {
							if (mc.level.dimension().registryKey() == Level.NETHER.registryKey() && spawnDelay == 0)
								return;
							if (!chestsOnly.getValue())
								ChatUtils.print(String.format("Detected Activated Spawner! Block Position: x:%d, y:%d, z:%d", (int) pos.getCenter().x, (int) pos.getCenter().y, (int) pos.getCenter().z));
							spawnerPositions.add(pos);
							BlockPos chestPos = getChestPos(pos);
							if (chestPos != null)
								ChatUtils.print(String.format("There is a chest nearby an activated spawner! Block Position: x:%d, y:%d, z:%d", (int) chestPos.getCenter().x, (int) chestPos.getCenter().y, (int) chestPos.getCenter().z));
						}
				});
			}
		}

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
	@Override
	public void onEnable() {
		super.onEnable();
		spawnerPositions.clear();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		spawnerPositions.clear();
	}

}

