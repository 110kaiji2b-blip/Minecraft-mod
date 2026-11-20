package com.swiftskyblock.client;

import com.swiftskyblock.menu.OmniSynthesizerMenu;
import com.swiftskyblock.network.CraftOmniItemPacket;
import com.swiftskyblock.network.ModNetworking;
import com.swiftskyblock.omni.OmniValueRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Searchable catalog screen for the Omni Synthesizer.
 * Supports Japanese & English name search, mouse-wheel paging, instant 1x / Shift-click stack synthesis.
 */
public class OmniSynthesizerScreen extends AbstractContainerScreen<OmniSynthesizerMenu> {
    private static final int COLS = 9;
    private static final int ROWS = 4;
    private static final int PAGE_SIZE = COLS * ROWS;
    private static final int GRID_X = 16;
    private static final int GRID_Y = 44;

    private EditBox searchBox;
    private Button prevButton;
    private Button nextButton;
    private List<Item> filteredItems = List.of();
    private int currentPage = 0;

    public OmniSynthesizerScreen(OmniSynthesizerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 194;
        this.imageHeight = 234;
    }

    @Override
    protected void init() {
        super.init();

        int x = this.leftPos;
        int y = this.topPos;

        this.searchBox = new EditBox(this.font, x + 16, y + 22, 114, 16, Component.translatable("gui.swiftskyblock.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(text -> {
            currentPage = 0;
            refreshFilter(text);
        });
        this.addRenderableWidget(this.searchBox);

        this.prevButton = Button.builder(Component.literal("<"), b -> {
            if (currentPage > 0) currentPage--;
        }).bounds(x + 134, y + 22, 18, 16).build();

        this.nextButton = Button.builder(Component.literal(">"), b -> {
            if ((currentPage + 1) * PAGE_SIZE < filteredItems.size()) currentPage++;
        }).bounds(x + 158, y + 22, 18, 16).build();

        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);

        refreshFilter("");
    }

    private void refreshFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Item> all = OmniValueRegistry.getCatalogItems();
        if (q.isEmpty()) {
            filteredItems = all;
            return;
        }
        List<Item> result = new ArrayList<>();
        for (Item item : all) {
            String displayName = item.getDescription().getString().toLowerCase(Locale.ROOT);
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            String idStr = id != null ? id.toString().toLowerCase(Locale.ROOT) : "";
            if (displayName.contains(q) || idStr.contains(q)) {
                result.add(item);
            }
        }
        filteredItems = result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused()) {
            if (keyCode == 256) { // Escape unfocuses search box first
                this.searchBox.setFocused(false);
                return true;
            }
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta < 0 && (currentPage + 1) * PAGE_SIZE < filteredItems.size()) {
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
            Item clicked = getHoveredCatalogItem(mouseX, mouseY);
            if (clicked != null) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(clicked);
                if (id != null) {
                    int count = hasShiftDown() ? Math.max(1, clicked.getMaxStackSize()) : 1;
                    ModNetworking.CHANNEL.sendToServer(new CraftOmniItemPacket(this.menu.getBlockPos(), id, count));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Item getHoveredCatalogItem(double mouseX, double mouseY) {
        int gx = this.leftPos + GRID_X;
        int gy = this.topPos + GRID_Y;
        if (mouseX < gx || mouseY < gy || mouseX >= gx + COLS * 18 || mouseY >= gy + ROWS * 18) {
            return null;
        }
        int col = (int) ((mouseX - gx) / 18);
        int row = (int) ((mouseY - gy) / 18);
        int idx = currentPage * PAGE_SIZE + row * COLS + col;
        if (idx >= 0 && idx < filteredItems.size()) {
            return filteredItems.get(idx);
        }
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Outer golden-trimmed dark panel
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A1E29);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFD4AF37);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF232938);

        int vp = this.menu.getVoidPoints();
        int startIdx = currentPage * PAGE_SIZE;

        // Render 9x4 catalog slots
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int sx = x + GRID_X + col * 18;
                int sy = y + GRID_Y + row * 18;
                int idx = startIdx + row * COLS + col;

                boolean hasItem = idx < filteredItems.size();
                int bg = 0xFF171B24;
                if (hasItem) {
                    int cost = OmniValueRegistry.getBuyCost(filteredItems.get(idx));
                    bg = (vp >= cost) ? 0xFF23383B : 0xFF261E24;
                }
                g.fill(sx, sy, sx + 17, sy + 17, 0xFF3E4659);
                g.fill(sx + 1, sy + 1, sx + 16, sy + 16, bg);

                if (hasItem) {
                    ItemStack stack = new ItemStack(filteredItems.get(idx));
                    g.renderItem(stack, sx + 1, sy + 1);
                }
            }
        }

        // Charge slot highlight (gold border)
        int cx = x + OmniSynthesizerMenu.CHARGE_SLOT_X - 1;
        int cy = y + OmniSynthesizerMenu.CHARGE_SLOT_Y - 1;
        g.fill(cx, cy, cx + 18, cy + 18, 0xFFD4AF37);
        g.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0xFF171B24);

        // Player inventory & hotbar slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + OmniSynthesizerMenu.INV_START_X + col * 18 - 1;
                int sy = y + OmniSynthesizerMenu.INV_START_Y + row * 18 - 1;
                g.fill(sx, sy, sx + 18, sy + 18, 0xFF3E4659);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1E29);
            }
        }
        for (int col = 0; col < 9; col++) {
            int sx = x + OmniSynthesizerMenu.INV_START_X + col * 18 - 1;
            int sy = y + OmniSynthesizerMenu.HOTBAR_Y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF3E4659);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1E29);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 10, 8, 0xFFF3E2A9, false);

        String vpText = "VP: " + NumberFormat.getNumberInstance(Locale.US).format(this.menu.getVoidPoints());
        int vpWidth = this.font.width(vpText);
        g.drawString(this.font, vpText, imageWidth - vpWidth - 10, 8, 0xFF66F0FF, false);

        int totalPages = Math.max(1, (filteredItems.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        String pageStr = (currentPage + 1) + "/" + totalPages;
        g.drawString(this.font, pageStr, 136, 118, 0xFFBCC4D6, false);

        g.drawString(this.font, Component.translatable("gui.swiftskyblock.omni_charge_hint"), 38, 130, 0xFFE5C878, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        Item hovered = getHoveredCatalogItem(mouseX, mouseY);
        if (hovered != null) {
            ItemStack sample = new ItemStack(hovered);
            int cost = OmniValueRegistry.getBuyCost(hovered);
            int vp = this.menu.getVoidPoints();
            boolean affordable = vp >= cost;

            List<Component> lines = new ArrayList<>();
            lines.add(sample.getHoverName());
            lines.add(Component.translatable("gui.swiftskyblock.omni_cost",
                    NumberFormat.getNumberInstance(Locale.US).format(cost))
                    .withStyle(affordable ? ChatFormatting.GREEN : ChatFormatting.RED));
            lines.add(Component.translatable("gui.swiftskyblock.omni_click_hint").withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else {
            this.renderTooltip(g, mouseX, mouseY);
        }
    }
}
