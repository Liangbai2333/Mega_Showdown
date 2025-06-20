package com.cobblemon.yajatkaul.mega_showdown.datapack.showdown;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.data.DataRegistry;
import com.cobblemon.mod.common.api.reactive.SimpleObservable;
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.Value;
import com.cobblemon.yajatkaul.mega_showdown.MegaShowdown;
import kotlin.Unit;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HeldItems implements DataRegistry {
    private static final Identifier ID = Identifier.of(MegaShowdown.MOD_ID, "showdown/held_items");
    private static final SimpleObservable<HeldItems> OBSERVABLE = new SimpleObservable<>();
    public static final HeldItems INSTANCE = new HeldItems();
    private final Map<String, String> heldItemsScripts = new HashMap<>();

    private HeldItems() {
        OBSERVABLE.subscribe(Priority.NORMAL, this::heldItemsLoad);
    }

    private Unit heldItemsLoad(HeldItems heldItem) {
        registerItems();
        return Unit.INSTANCE;
    }

    public void registerItems() {
        Cobblemon.INSTANCE.getShowdownThread().queue(showdownService -> {
            if (showdownService instanceof GraalShowdownService service) {
                Value receiveHeldItemDataFn = service.context.getBindings("js").getMember("receiveHeldItemData");
                for (Map.Entry<String, String> entry : HeldItems.INSTANCE.getHeldItemsScripts().entrySet()) {
                    String itemId = entry.getKey();
                    String js = entry.getValue().replace("\n", " ");
                    receiveHeldItemDataFn.execute(itemId, js);
                }
            }
            return Unit.INSTANCE;
        });
    }

    public Map<String, String> getHeldItemsScripts() {
        return heldItemsScripts;
    }

    @NotNull
    @Override
    public Identifier getId() {
        return ID;
    }


    @NotNull
    @Override
    public SimpleObservable<? extends DataRegistry> getObservable() {
        return OBSERVABLE;
    }

    @Override
    public void reload(@NotNull ResourceManager resourceManager) {
        heldItemsScripts.clear();
        resourceManager.findAllResources("showdown/held_items", path -> path.getPath().endsWith(".js")).forEach((id, resources) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resources.getLast().getInputStream(), StandardCharsets.UTF_8))) {
                String js = reader.lines().collect(Collectors.joining("\n"));
                String itemId = new File(id.getPath()).getName().replace(".js", "");
                heldItemsScripts.put(itemId, js);
            } catch (IOException e) {
                MegaShowdown.LOGGER.error("Failed to load held item script: {} {}", id, e);
            }
        });
        OBSERVABLE.emit(this);
    }

    @NotNull
    @Override
    public ResourceType getType() {
        return ResourceType.SERVER_DATA;
    }

    @Override
    public void sync(@NotNull ServerPlayerEntity serverPlayerEntity) {

    }
}
