package cn.blockforge.generated.ysmv.client;

import cn.blockforge.generated.ysmv.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public final class VillagerScreen extends Screen {
    private final LivingEntity villager;
    private final ModNetwork.Open session;
    private List<YsmClient.Model> models = List.of();
    private List<YsmClient.Model> filtered = List.of();
    private Appearance selected;
    private EditBox search;
    private Button apply, previous, next, skin;
    private final List<Button> entries = new ArrayList<>();
    private int page, rows, left, top, panelWidth, panelHeight, listWidth, previewX, previewWidth;
    private String query = "", status = "选择模型后，点击“应用外观”保存。";
    private boolean waiting;
    private int ticks;

    public VillagerScreen(LivingEntity villager, ModNetwork.Open session) {
        super(Component.literal(WardrobeTarget.displayName(villager) + "换装 · YSM"));
        this.villager = villager;
        this.session = session;
        this.selected = session.appearance();
        this.models = YsmClient.models(session.allowed());
    }

    @Override
    protected void init() {
        panelWidth = Math.min(600, width - 16);
        panelHeight = Math.min(350, height - 12);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        listWidth = Math.max(110, panelWidth / 2 - 14);
        previewX = left + listWidth + 22;
        previewWidth = panelWidth - listWidth - 32;
        rows = Math.max(1, (panelHeight - 142) / 24);
        entries.clear();

        search = new EditBox(font, left + 10, top + 32, listWidth, 20, Component.literal("搜索模型名称或 ID"));
        search.setMaxLength(100);
        search.setHint(Component.literal("搜索模型名称或 ID"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            page = 0;
            filter();
        });
        addRenderableWidget(search);

        for (int i = 0; i < rows; i++) {
            final int slot = i;
            entries.add(addRenderableWidget(Button.builder(Component.empty(), button -> choose(slot))
                    .bounds(left + 10, top + 59 + i * 24, listWidth, 21).build()));
        }

        int navY = top + panelHeight - 77;
        previous = addRenderableWidget(Button.builder(Component.literal("上一页"), button -> {
            page--;
            updateEntries();
        }).bounds(left + 10, navY, 54, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal("下一页"), button -> {
            page++;
            updateEntries();
        }).bounds(left + 10 + listWidth - 54, navY, 54, 20).build());
        skin = addRenderableWidget(Button.builder(Component.literal("皮肤：默认"), button -> cycleSkin())
                .bounds(previewX, navY, previewWidth, 20).build());

        int bottom = top + panelHeight - 27;
        int buttonWidth = Math.max(48, (panelWidth - 38) / 4);

        addRenderableWidget(Button.builder(Component.literal("恢复原版"), button -> {
            if (!waiting) {
                selected = Appearance.DEFAULT;
                status = "已预览原版外观，点击应用后保存。";
                updateEntries();
            }
        }).bounds(left + 10, bottom, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("刷新列表"), button -> {
            models = YsmClient.models(session.allowed());
            filter();
            status = "已刷新；新导入的模型请先在 YSM 重载，再重新打开本页。";
        }).bounds(left + 16 + buttonWidth, bottom, buttonWidth, 20).build());

        apply = addRenderableWidget(Button.builder(Component.literal("应用外观"), button -> {
            if (!waiting) {
                waiting = true;
                status = "正在保存……";
                updateEntries();
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.Apply(session.token(), selected));
            }
        }).bounds(left + 22 + buttonWidth * 2, bottom, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("取消"), button -> onClose())
                .bounds(left + 28 + buttonWidth * 3, bottom, buttonWidth, 20).build());

        filter();
    }

    private void filter() {
        String text = query.toLowerCase(Locale.ROOT).trim();
        filtered = models.stream()
                .filter(model -> model.id().toLowerCase(Locale.ROOT).contains(text)
                        || model.name().toLowerCase(Locale.ROOT).contains(text))
                .toList();
        page = Math.max(0, Math.min(page, Math.max(0, (filtered.size() - 1) / rows)));
        updateEntries();
    }

    private void updateEntries() {
        for (int i = 0; i < entries.size(); i++) {
            Button button = entries.get(i);
            int index = page * rows + i;
            button.visible = index < filtered.size();
            button.active = !waiting;
            if (button.visible) {
                YsmClient.Model model = filtered.get(index);
                String marker = selected.model().equals(model.id()) ? "✓ " : "";
                button.setMessage(Component.literal(font.plainSubstrByWidth(marker + model.name(), listWidth - 14)));
                button.setTooltip(Tooltip.create(Component.literal(model.name() + "\n" + model.id())));
            }
        }
        previous.active = !waiting && page > 0;
        next.active = !waiting && (page + 1) * rows < filtered.size();
        skin.active = !waiting && !selected.isDefault() && !textures().isEmpty();
        skin.setMessage(Component.literal(font.plainSubstrByWidth(
                "皮肤：" + (selected.texture().isEmpty() ? "默认" : selected.texture()), previewWidth - 10)));
        skin.setTooltip(Tooltip.create(Component.literal("点击切换这个模型的皮肤")));
        if (apply != null) {
            apply.active = !waiting && (selected.isDefault()
                    || models.stream().anyMatch(m -> m.id().equals(selected.model())));
        }
    }

    private List<String> textures() {
        return models.stream()
                .filter(model -> model.id().equals(selected.model()))
                .findFirst()
                .map(YsmClient.Model::textures)
                .orElse(List.of());
    }

    private void choose(int slot) {
        int index = page * rows + slot;
        if (index >= filtered.size() || waiting) return;
        selected = new Appearance(filtered.get(index).id(), "");
        status = "仅预览，点击“应用外观”才会修改村民。";
        updateEntries();
    }

    private void cycleSkin() {
        List<String> textures = textures();
        if (textures.isEmpty()) return;
        int index = textures.indexOf(selected.texture());
        selected = new Appearance(selected.model(), index + 1 >= textures.size() ? "" : textures.get(index + 1));
        updateEntries();
    }

    public void result(ModNetwork.Result packet) {
        if (!packet.token().equals(session.token())) return;
        waiting = false;
        status = packet.message();
        if (packet.success()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal(status), true);
            }
            onClose();
        } else {
            updateEntries();
        }
    }

    @Override
    public void tick() {
        search.tick();
        if (minecraft == null || minecraft.player == null || !villager.isAlive()
                || villager.level() != minecraft.level || minecraft.player.distanceToSqr(villager) > 100) {
            onClose();
            return;
        }
        if (++ticks % 40 == 0 && !waiting) {
            models = YsmClient.models(session.allowed());
            filter();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF018202B);
        graphics.fill(left, top, left + panelWidth, top + 3, 0xFF65D6B4);
        graphics.drawString(font, title, left + 10, top + 12, 0xFFFFFF);
        graphics.drawString(font, "仅改变外观", left + panelWidth - 78, top + 12, 0xA7B9CA);

        int previewTop = top + 32;
        int previewBottom = top + panelHeight - 84;
        graphics.fill(previewX, previewTop, previewX + previewWidth, previewBottom, 0xFF111821);
        graphics.drawCenteredString(font, "外观预览", previewX + previewWidth / 2, previewTop + 7, 0xA7B9CA);

        int scale = Math.max(12, Math.min(65, (previewBottom - previewTop - 31) / 2));
        graphics.enableScissor(previewX + 1, previewTop + 20, previewX + previewWidth - 1, previewBottom - 1);
        ClientEvents.previewEntity = villager;
        ClientEvents.previewAppearance = selected;
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, previewX + previewWidth / 2,
                    previewBottom - 6, scale, previewX + previewWidth / 2F - mouseX,
                    previewBottom - scale - mouseY, villager);
        } finally {
            ClientEvents.previewEntity = null;
            graphics.disableScissor();
        }

        if (filtered.isEmpty()) {
            graphics.drawWordWrap(font, Component.literal(models.isEmpty()
                    ? "暂无可选模型。请等待 YSM 同步完成，并确认模型已获得授权。"
                    : "没有找到匹配的模型。"), left + 14, top + 64, listWidth - 8, 0xA7B9CA);
        }

        int pages = Math.max(1, (filtered.size() + rows - 1) / rows);
        graphics.drawCenteredString(font, (page + 1) + " / " + pages, left + 10 + listWidth / 2,
                top + panelHeight - 70, 0xFFFFFF);
        String message = YsmClient.error().isEmpty() ? status : YsmClient.error();
        graphics.drawString(font, font.plainSubstrByWidth(message, panelWidth - 20), left + 10,
                top + panelHeight - 45, 0xBDDCCD);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= left + 10 && mouseX <= left + 10 + listWidth && !waiting) {
            page = Math.max(0, Math.min(Math.max(0, (filtered.size() - 1) / rows), page + (delta < 0 ? 1 : -1)));
            updateEntries();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        YsmClient.clearPreview(villager);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}