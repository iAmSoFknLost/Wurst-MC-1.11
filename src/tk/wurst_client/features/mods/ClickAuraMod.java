/*
 * Copyright � 2014 - 2017 | Wurst-Imperium | All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.features.mods;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import tk.wurst_client.events.listeners.UpdateListener;
import tk.wurst_client.features.Feature;
import tk.wurst_client.features.special_features.YesCheatSpf.BypassLevel;
import tk.wurst_client.settings.CheckboxSetting;
import tk.wurst_client.settings.SliderSetting;
import tk.wurst_client.settings.SliderSetting.ValueDisplay;
import tk.wurst_client.utils.EntityUtils;
import tk.wurst_client.utils.EntityUtils.TargetSettings;
import tk.wurst_client.utils.RotationUtils;

@Mod.Info(
	description = "Automatically attacks the closest valid entity whenever you click.\n"
		+ "�lWarning:�r ClickAuras generally look more suspicious than Killauras\n"
		+ "and are easier to detect. It is recommended to use Killaura or\n"
		+ "TriggerBot instead.",
	name = "ClickAura",
	tags = "Click Aura,ClickAimbot,Click Aimbot",
	help = "Mods/ClickAura")
@Mod.Bypasses(ghostMode = false)
public class ClickAuraMod extends Mod implements UpdateListener
{
	public final CheckboxSetting useKillaura =
		new CheckboxSetting("Use Killaura settings", true)
		{
			@Override
			public void update()
			{
				if(isChecked())
				{
					KillauraMod killaura = wurst.mods.killauraMod;
					useCooldown.lock(killaura.useCooldown.isChecked());
					speed.lockToValue(killaura.speed.getValue());
					range.lockToValue(killaura.range.getValue());
					fov.lockToValue(killaura.fov.getValue());
					hitThroughWalls.lock(killaura.hitThroughWalls.isChecked());
				}else
				{
					useCooldown.unlock();
					speed.unlock();
					range.unlock();
					fov.unlock();
					hitThroughWalls.unlock();
				}
			}
		};
	public final CheckboxSetting useCooldown =
		new CheckboxSetting("Use Attack Cooldown as Speed", true)
		{
			@Override
			public void update()
			{
				speed.setDisabled(isChecked());
			}
		};
	public final SliderSetting speed =
		new SliderSetting("Speed", 20, 0.1, 20, 0.1, ValueDisplay.DECIMAL);
	public final SliderSetting range =
		new SliderSetting("Range", 6, 1, 6, 0.05, ValueDisplay.DECIMAL);
	public final SliderSetting fov =
		new SliderSetting("FOV", 360, 30, 360, 10, ValueDisplay.DEGREES);
	public final CheckboxSetting hitThroughWalls =
		new CheckboxSetting("Hit through walls", false);
	
	private TargetSettings targetSettings = new TargetSettings()
	{
		@Override
		public boolean targetBehindWalls()
		{
			return hitThroughWalls.isChecked();
		}
		
		@Override
		public float getRange()
		{
			return range.getValueF();
		}
		
		@Override
		public float getFOV()
		{
			return fov.getValueF();
		}
	};
	
	@Override
	public void initSettings()
	{
		settings.add(useKillaura);
		settings.add(useCooldown);
		settings.add(speed);
		settings.add(range);
		settings.add(fov);
		settings.add(hitThroughWalls);
	}
	
	@Override
	public Feature[] getSeeAlso()
	{
		return new Feature[]{wurst.special.targetSpf, wurst.mods.killauraMod,
			wurst.mods.killauraLegitMod, wurst.mods.multiAuraMod,
			wurst.mods.triggerBotMod};
	}
	
	@Override
	public void onEnable()
	{
		// disable other killauras
		wurst.mods.killauraMod.setEnabled(false);
		wurst.mods.killauraLegitMod.setEnabled(false);
		wurst.mods.multiAuraMod.setEnabled(false);
		wurst.mods.triggerBotMod.setEnabled(false);
		
		// add listener
		wurst.events.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		// remove listener
		wurst.events.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// update timer
		updateMS();
		
		// check if clicking
		if(!mc.gameSettings.keyBindAttack.pressed)
			return;
		
		// check timer / cooldown
		if(useCooldown.isChecked() ? mc.player.getCooledAttackStrength(0F) < 1F
			: !hasTimePassedS(speed.getValueF()))
			return;
		
		// set entity
		Entity entity = EntityUtils.getBestEntityToAttack(targetSettings);
		if(entity == null)
			return;
		
		// AutoSword
		wurst.mods.autoSwordMod.setSlot();
		
		// Criticals
		wurst.mods.criticalsMod.doCritical();
		
		// face entity
		if(!RotationUtils.faceEntityPacket(entity))
			return;
		
		// attack entity
		mc.playerController.attackEntity(mc.player, entity);
		mc.player.swingArm(EnumHand.MAIN_HAND);
		
		// reset timer
		updateLastMS();
	}
	
	@Override
	public void onYesCheatUpdate(BypassLevel bypassLevel)
	{
		switch(bypassLevel)
		{
			default:
			case OFF:
			case MINEPLEX:
				speed.unlock();
				range.unlock();
				hitThroughWalls.unlock();
				break;
			case ANTICHEAT:
			case OLDER_NCP:
			case LATEST_NCP:
				speed.lockToMax(12);
				range.lockToMax(4.25);
				hitThroughWalls.unlock();
				break;
			case GHOST_MODE:
				speed.lockToMax(12);
				range.lockToMax(4.25);
				hitThroughWalls.lock(false);
				break;
		}
	}
}
