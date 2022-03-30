package com.teamabnormals.pet_cemetery.common.item;

import com.google.common.collect.Maps;
import com.teamabnormals.pet_cemetery.common.block.CompanionCoilBlock;
import com.teamabnormals.pet_cemetery.common.block.state.properties.CoilType;
import com.teamabnormals.pet_cemetery.core.PetCemetery;
import com.teamabnormals.pet_cemetery.core.other.tags.PCEntityTypeTags;
import com.teamabnormals.pet_cemetery.core.registry.PCBlocks;
import com.teamabnormals.pet_cemetery.core.registry.PCEntityTypes;
import com.teamabnormals.pet_cemetery.core.registry.PCItems;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.text.WordUtils;

import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber(modid = PetCemetery.MOD_ID)
public class ForgottenCollarItem extends Item {
	public static final String PET_ID = "PetID";
	public static final String PET_NAME = "PetName";
	public static final String PET_VARIANT = "PetVariant";

	public static final String COLLAR_COLOR = "CollarColor";
	public static final String IS_CHILD = "IsChild";
	public static final String OWNER_ID = "OwnerID";

	public static final String HORSE_STRENGTH = "HorseStrength";
	public static final String HORSE_SPEED = "HorseSpeed";
	public static final String HORSE_HEALTH = "HorseHealth";

	public ForgottenCollarItem(Properties properties) {
		super(properties);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntityLiving();
		EntityType<?> type = entity.getType();

		if (type.is(PCEntityTypeTags.DROPS_FORGOTTEN_COLLAR)) {
			ItemStack collar = new ItemStack(PCItems.FORGOTTEN_COLLAR.get());
			CompoundTag tag = collar.getOrCreateTag();

			tag.putString(ForgottenCollarItem.PET_ID, type.getRegistryName().toString());
			tag.putBoolean(ForgottenCollarItem.IS_CHILD, entity.isBaby());
			if (entity.hasCustomName()) {
				tag.putString(ForgottenCollarItem.PET_NAME, entity.getCustomName().getString());
			}

			if (entity instanceof TamableAnimal pet) {
				if (pet.isTame()) {
					tag.putString(ForgottenCollarItem.OWNER_ID, pet.getOwnerUUID().toString());

					if (entity instanceof Wolf wolf) {
						tag.putInt(ForgottenCollarItem.COLLAR_COLOR, wolf.getCollarColor().getId());
					}

					if (entity instanceof Cat cat) {
						tag.putInt(ForgottenCollarItem.PET_VARIANT, cat.getCatType());
						tag.putInt(ForgottenCollarItem.COLLAR_COLOR, cat.getCollarColor().getId());
					}

					if (entity instanceof Parrot parrot) {
						tag.putInt(ForgottenCollarItem.PET_VARIANT, parrot.getVariant());
					}

					entity.spawnAtLocation(collar);
				}
			} else if (entity instanceof AbstractHorse horse) {
				if (horse.isTamed()) {
					tag.putString(ForgottenCollarItem.OWNER_ID, horse.getOwnerUUID().toString());
					tag.putDouble(ForgottenCollarItem.HORSE_SPEED, horse.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
					tag.putDouble(ForgottenCollarItem.HORSE_HEALTH, horse.getAttributeBaseValue(Attributes.MAX_HEALTH));
					tag.putDouble(ForgottenCollarItem.HORSE_STRENGTH, horse.getAttributeBaseValue(Attributes.JUMP_STRENGTH));
					if (entity instanceof Horse)
						tag.putInt(ForgottenCollarItem.PET_VARIANT, ((Horse) entity).getTypeVariant());

					entity.spawnAtLocation(collar);
				}
			}
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = world.getBlockState(pos);
		ItemStack stack = context.getItemInHand();

		BlockPos offsetPos = pos.above(3);
		if (state.is(PCBlocks.COMPANION_COIL.get()) && state.getValue(CompanionCoilBlock.COIL_TYPE) == CoilType.BOTTOM && state.getValue(CompanionCoilBlock.CHARGE) == 10 && world.canSeeSky(offsetPos) && world.isNight()) {
			if (!world.getBlockState(offsetPos).getCollisionShape(world, offsetPos).isEmpty())
				return InteractionResult.FAIL;

			Player player = context.getPlayer();
			CompoundTag tag = stack.getOrCreateTag();

			if (tag.contains(PET_ID)) {
				Map<EntityType<?>, EntityType<?>> UNDEAD_MAP = Util.make(Maps.newHashMap(), (map) -> {
					map.put(EntityType.WOLF, PCEntityTypes.ZOMBIE_WOLF.get());
					map.put(EntityType.CAT, PCEntityTypes.ZOMBIE_CAT.get());
					map.put(EntityType.HORSE, EntityType.ZOMBIE_HORSE);
					map.put(EntityType.PARROT, PCEntityTypes.ZOMBIE_PARROT.get());
					map.put(PCEntityTypes.ZOMBIE_WOLF.get(), PCEntityTypes.SKELETON_WOLF.get());
					map.put(PCEntityTypes.ZOMBIE_CAT.get(), PCEntityTypes.SKELETON_CAT.get());
					map.put(EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE);
					map.put(PCEntityTypes.ZOMBIE_PARROT.get(), PCEntityTypes.SKELETON_PARROT.get());
				});

				EntityType<?> entityType = UNDEAD_MAP.get(ForgeRegistries.ENTITIES.getValue(new ResourceLocation(tag.getString(PET_ID))));
				System.out.println(entityType);

				if (entityType != null) {
					Animal entity = (Animal) entityType.create(world);
					UUID owner = tag.contains(OWNER_ID) ? UUID.fromString(tag.getString(OWNER_ID)) : player.getUUID();
					DyeColor collarColor = DyeColor.byId(tag.getInt(COLLAR_COLOR));
					int variant = tag.getInt(PET_VARIANT);
					LivingEntity returnEntity = null;

					entity.setBaby(tag.getBoolean(IS_CHILD));
					entity.setPos(offsetPos.getX() + 0.5F, offsetPos.getY(), offsetPos.getZ() + 0.5F);
					if (tag.contains(PET_NAME))
						entity.setCustomName(new TextComponent(tag.getString(PET_NAME)));

					if (entity instanceof TamableAnimal tameableEntity) {
						tameableEntity.setTame(true);
						tameableEntity.setOwnerUUID(owner);

						if (tameableEntity instanceof Wolf wolf) {
							wolf.setCollarColor(collarColor);
							returnEntity = wolf;
						}

						if (tameableEntity instanceof Cat cat) {
							cat.setCatType(variant);
							cat.setCollarColor(collarColor);
							returnEntity = cat;
						}

						if (tameableEntity instanceof Parrot parrot) {
							parrot.setVariant(variant);
							returnEntity = parrot;
						}

					} else if (entity instanceof AbstractHorse horseEntity) {
						horseEntity.setTamed(true);
						horseEntity.setOwnerUUID(owner);

						horseEntity.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(tag.getDouble(HORSE_STRENGTH));
						horseEntity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tag.getDouble(HORSE_SPEED));
						horseEntity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(tag.getDouble(HORSE_HEALTH));

						returnEntity = horseEntity;
					}

					if (returnEntity != null) {
						world.setBlockAndUpdate(pos, state.setValue(CompanionCoilBlock.CHARGE, 0));

						LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
						bolt.moveTo(Vec3.atBottomCenterOf(entity.blockPosition()));
						bolt.setCause(player instanceof ServerPlayer ? (ServerPlayer) player : null);
						bolt.setVisualOnly(true);
						world.addFreshEntity(bolt);

						world.playSound(player, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
						world.addFreshEntity(returnEntity);
						if (!player.getAbilities().instabuild)
							stack.shrink(1);
					}
				}
			}

			return InteractionResult.sidedSuccess(world.isClientSide);
		}
		return InteractionResult.PASS;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		CompoundTag tag = stack.getOrCreateTag();

		if (tag.contains(PET_NAME)) {
			String name = tag.getString(PET_NAME);
			tooltip.add(new TextComponent(name).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		}

		if (tag.contains(PET_ID)) {
			String petID = tag.getString(PET_ID);
			EntityType<?> pet = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(petID));

			Component petType = new TranslatableComponent(pet.getDescriptionId()).withStyle(ChatFormatting.GRAY);
			if (tag.contains(PET_VARIANT)) {
				int type = tag.getInt(PET_VARIANT);
				String texture = "";

				if (pet == EntityType.CAT || pet == PCEntityTypes.ZOMBIE_CAT.get()) {
					texture = Cat.TEXTURE_BY_TYPE.get(type).toString().replace("minecraft:textures/entity/cat/", "");
					texture = texture.replace(".png", "").replace("all_", "").replace("_", " ").concat(" ");
				}

				if (pet == EntityType.PARROT || pet == PCEntityTypes.ZOMBIE_PARROT.get()) {
					texture = ParrotRenderer.PARROT_LOCATIONS[type].toString().replace("minecraft:textures/entity/parrot/parrot_", "");
					texture = texture.replace(".png", "").replace("_", "-").concat(" ");
				}

				petType = new TextComponent(WordUtils.capitalize(texture)).withStyle(ChatFormatting.GRAY).append(petType);
			}

			if (tag.getBoolean(IS_CHILD))
				petType = new TranslatableComponent("tooltip." + PetCemetery.MOD_ID + ".baby").withStyle(ChatFormatting.GRAY).append(" ").append(petType);

			tooltip.add(petType);
		}

		super.appendHoverText(stack, worldIn, tooltip, flagIn);
	}

	public int getColor(ItemStack stack) {
		CompoundTag tag = stack.getOrCreateTag();
		return tag.contains(COLLAR_COLOR) ? DyeColor.byId(tag.getInt(COLLAR_COLOR)).getTextColor() : 10511680;
	}
}
