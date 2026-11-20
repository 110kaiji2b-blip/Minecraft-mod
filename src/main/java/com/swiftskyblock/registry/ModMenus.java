package com.swiftskyblock.registry;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.menu.OmniSynthesizerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SwiftSkyblockMod.MOD_ID);

    public static final RegistryObject<MenuType<OmniSynthesizerMenu>> OMNI_SYNTHESIZER =
            MENUS.register("omni_synthesizer", () -> IForgeMenuType.create(OmniSynthesizerMenu::new));
}
