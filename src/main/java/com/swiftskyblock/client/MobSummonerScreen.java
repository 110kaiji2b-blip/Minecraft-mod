package com.swiftskyblock.client;

import com.swiftskyblock.mob.MobSummonRegistry;
import com.swiftskyblock.network.ModNetworking;
import com.swiftskyblock.network.SelectSummonMobPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Searchable Mob Selection screen for the Mob Summoning Staff.
 * Displays spawn egg icons (or fitting emblems for Golems/Wither), durability cost,
 * and supports Japanese & English live search.
 */
public class MobSummonerScreen extends Screen {
    private static final int PANEL_W = 246;
    private static final int PANEL_H = 196;
    private static final int COLS = 2;
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = COLS * ROWS;

    private final InteractionHand hand;
    @Nullable
    private ResourceLocation selectedMobId;

    private EditBox searchBox;
    private List<EntityType<?>> filteredMobs = List.of();
    private int currentPage = 0;

    public MobSummonerScreen(InteractionHand hand, @Nullable ResourceLocation selectedMobId) {
        super(Component.translatable("gui.swiftskyblock.mob_summoner.title"));
        this.hand = hand;
        this.selectedMobId = selectedMobId;
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        this.searchBox = new EditBox(this.font, left + 12, top + 24, 166, 16, Component.translatable("gui.swiftskyblock.search"));
        this.searchBox.setMaxLength(40);
        this.searchBox.setResponder(text -> {
            currentPage = 0;
            refreshFilter(text);
        });
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (currentPage > 0) currentPage--;
        }).bounds(left + 184, top + 24, 22, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if ((currentPage + 1) * PAGE_SIZE < filteredMobs.size()) currentPage++;
        }).bounds(left + 210, top + 24, 22, 16).build());

        refreshFilter("");
    }

    private void refreshFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<EntityType<?>> all = MobSummonRegistry.getSummonableMobs();
        if (q.isEmpty()) {
            filteredMobs = all;
            return;
        }
        List<EntityType<?>> out = new ArrayList<>();
        for (EntityType<?> type : all) {
            String name = type.getDescription().getString().toLowerCase(Locale.ROOT);
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            String idStr = id != null ? id.toString().toLowerCase(Locale.ROOT) : "";
            if (name.contains(q) || idStr.contains(q)) {
                out.add(type);
            }
        }
        filteredMobs = out;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta < 0 && (currentPage + 1) * PAGE_SIZE < filteredMobs.size()) {
            currentPage++;
            return true;
        } else if (delta > 0 && currentPage > 0) {
            currentPage--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            EntityType<?> clicked = getHoveredMob(mouseX, mouseY);
            if (clicked != null) {
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(clicked);
                if (id != null) {
                    this.selectedMobId = id;
                    ModNetworking.CHANNEL.sendToServer(new SelectSummonMobPacket(this.hand, id));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nullable
    private EntityType<?> getHoveredMob(double mouseX, double mouseY) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int startX = left + 12;
        int startY = top + 46;
        int cellW = 108;
        int cellH = 22;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int rx = startX + col * (cellW + 6);
                int ry = startY + row * cellH;
                if (mouseX >= rx && mouseX < rx + cellW && mouseY >= ry && mouseY < ry + cellH - 2) {
                    int idx = currentPage * PAGE_SIZE + row * COLS + col;
                    if (idx >= 0 && idx < filteredMobs.size()) {
                        return filteredMobs.get(idx);
                    }
                }
            }
        }
        return null;
    }

    private ItemStack getIconFor(EntityType<?> type) {
        Item egg = SpawnEggItem.byId(type);
        if (egg != null) return new ItemStack(egg);
        if (type == EntityType.WITHER) return new ItemStack(Items.NETHER_STAR);
        if (type == EntityType.IRON_GOLEM) return new ItemStack(Items.IRON_BLOCK);
        if (type == EntityType.SNOW_GOLEM) return new ItemStack(Items.CARVED_PUMPKIN);
        return new ItemStack(Items.ENDER_PEARL);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Dark purple/gold mystic panel
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF161321);
        g.fill(left + 1, top + 1, left + PANEL_W - 1, top + PANEL_H - 1, 0xFFB87BFF);
        g.fill(left + 2, top + 2, left + PANEL_W - 2, top + PANEL_H - 2, 0xFF211C30);

        g.drawString(this.font, this.title, left + 12, top + 9, 0xFFF2DEFF, false);

        int totalPages = Math.max(1, (filteredMobs.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        String pageStr = (currentPage + 1) + "/" + totalPages;
        g.drawString(this.font, pageStr, left + PANEL_W - this.font.width(pageStr) - 12, top + 9, 0xFFBFB5D9, false);

        int startX = left + 12;
        int startY = top + 46;
        int cellW = 108;
        int cellH = 22;

        EntityType<?> hovered = getHoveredMob(mouseX, mouseY);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = currentPage * PAGE_SIZE + row * COLS + col;
                if (idx >= filteredMobs.size()) continue;

                EntityType<?> type = filteredMobs.get(idx);
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
                boolean isSelected = id != null && id.equals(selectedMobId);
                boolean isHovered = type == hovered;

                int rx = startX + col * (cellW + 6);
                int ry = startY + row * cellH;

                int border = isSelected ? 0xFFFFD74A : (isHovered ? 0xFF7BE8FF : 0xFF473E63);
                int bg = isHovered ? 0xFF352C4F : 0xFF272138;
                if (MobSummonRegistry.isBoss(type)) {
                    bg = isHovered ? 0xFF4A2234 : 0xFF381A28;
                }

                g.fill(rx, ry, rx + cellW, ry + cellH - 2, border);
                g.fill(rx + 1, ry + 1, rx + cellW - 1, ry + cellH - 3, bg);

                g.renderItem(getIconFor(type), rx + 2, ry + 2);

                String label = type.getDescription().getString();
                if (this.font.width(label) > cellW - 24) {
                    label = this.font.plainSubstrByWidth(label, cellW - 28) + "..";
                }
                int textColor = MobSummonRegistry.isBoss(type) ? 0xFFFF8888 : 0xFFEAE6F5;
                g.drawString(this.font, label, rx + 21, ry + 6, textColor, false);
            }
        }

        g.drawString(this.font, Component.translatable("gui.swiftskyblock.mob_summoner.hint"),
                left + 12, top + PANEL_H - 14, 0xFFABA3C4, false);

        super.render(g, mouseX, mouseY, partialTick);

        if (hovered != null) {
            int cost = MobSummonRegistry.getDurabilityCost(hovered);
            List<Component> tip = new ArrayList<>();
            tip.add(hovered.getDescription());
            tip.add(Component.translatable("tooltip.swiftskyblock.staff.cost", cost).withStyle(ChatFormatting.GOLD));
            if (MobSummonRegistry.isBoss(hovered)) {
                tip.add(Component.translatable("gui.swiftskyblock.mob_summoner.boss_warn").withStyle(ChatFormatting.RED));
            }
            g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
