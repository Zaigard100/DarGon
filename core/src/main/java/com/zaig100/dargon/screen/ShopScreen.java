package com.zaig100.dargon.screen;

import static com.zaig100.dargon.ui.Ui.button;
import static com.zaig100.dargon.ui.Ui.column;
import static com.zaig100.dargon.ui.Ui.image;
import static com.zaig100.dargon.ui.Ui.layers;
import static com.zaig100.dargon.ui.Ui.paragraph;
import static com.zaig100.dargon.ui.Ui.row;
import static com.zaig100.dargon.ui.Ui.text;

import com.zaig100.dargon.ecs.Entity;
import com.zaig100.dargon.game.component.ShopStock;
import com.zaig100.dargon.game.item.ItemStack;
import com.zaig100.dargon.ui.UiNode;
import com.zaig100.dargon.ui.signal.Signal;
import java.util.ArrayList;
import java.util.List;

/** Магазин: товары с ценами и остатком, монеты игрока, причина отказа. */
final class ShopScreen extends UiScreen {

    private static final float DESCRIPTION_WIDTH = 96;

    private final int shop;
    private final Signal<String> status = Signal.of("");

    /** Растёт после каждой покупки — остатки на товарах пересчитываются. */
    private final Signal<Integer> sold = Signal.of(0);

    ShopScreen(int shop) {
        this.shop = shop;
    }

    @Override
    public Area area() {
        return Area.GAME;
    }

    @Override
    protected UiNode build() {
        var texts = app.texts();
        var items = app.data().items();
        HudModel hud = app.gameplay().hud();
        ShopStock stock = app.gameplay().level().world().get(shop, ShopStock.class);
        List<UiNode> offers = new ArrayList<>();
        for (int i = 0; i < stock.offers.size(); i++) {
            ShopStock.Offer offer = stock.offers.get(i);
            int index = i;
            var b =
                    button(
                                    column(
                                            1,
                                            Widgets.itemSlot(items, offer.item()),
                                            row(
                                                    1,
                                                    image("money").size(5, 5),
                                                    text(String.valueOf(offer.price()))
                                                            .style("tiny")),
                                            text(sold.combine(
                                                            texts.localeSignal(),
                                                            (v, l) ->
                                                                    offer.stock() > 0
                                                                            ? texts.ui(
                                                                                    "shop.stock",
                                                                                    offer.stock())
                                                                            : texts.ui(
                                                                                    "shop.soldOut")))
                                                    .style("tiny")),
                                    () -> buy(index, offer.item()))
                            .enabled(sold.map(v -> offer.stock() > 0))
                            .onFocus(() -> status.set(texts.ui("item." + offer.item().id())))
                            .pad(1);
            offers.add(i == 0 ? b.focused() : b);
        }
        return layers(
                        Widgets.panel(
                                column(
                                        2,
                                        text(texts.tr("shop.title")).style("button"),
                                        row(2, offers.toArray(new UiNode[0])),
                                        row(
                                                1,
                                                image("money").size(6, 6),
                                                text(hud.coins.map(String::valueOf))
                                                        .style("small")),
                                        paragraph(status).style("hint").size(DESCRIPTION_WIDTH, -1),
                                        button(texts.tr("shop.close"), this::back))))
                .background("dim")
                .expand();
    }

    private void buy(int index, ItemStack item) {
        var player = app.gameplay().level().state().player();
        int price =
                app.gameplay().level().world().get(shop, ShopStock.class).offers.get(index).price();
        if (app.gameplay().buy(shop, index)) {
            status.set(app.texts().ui("shop.bought", app.texts().ui("item." + item.id())));
            sold.set(sold.get() + 1);
        } else if (player.coins < price) {
            status.set(app.texts().ui("shop.noMoney"));
        } else {
            status.set(app.texts().ui("shop.noRoom"));
        }
    }

    @Override
    protected void closed() {
        if (app.gameplay() != null) {
            app.gameplay().level().state().setOpenShop(Entity.NONE);
        }
    }
}
