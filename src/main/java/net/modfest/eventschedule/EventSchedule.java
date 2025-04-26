package net.modfest.eventschedule;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;

public class EventSchedule {
    public static EventSchedule instance = new EventSchedule();

    private static final TypeToken<List<EventInfo>> TOKEN = new TypeToken<>() {};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().disableHtmlEscaping()
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) -> Instant.parse(json.getAsString()))
            .create();
    @SerializedName("current")
    public EventInfo currentEvent = null;
    public List<EventInfo> events = new ArrayList<>();


    @Nullable
    public EventInfo get(int i) {
        return i < events.size() && i >= 0 ? events.get(i) : null;
    }

    public void update() {
        try {
            var time = Instant.now();

            events.removeIf(x -> x.end.isBefore(time));

            if (currentEvent != null && currentEvent.end.isBefore(time)) {
                currentEvent = null;
            }

            if (currentEvent == null && !events.isEmpty()) {
                var event = events.getFirst();
                if (event.start.isBefore(time)) {
                    currentEvent = event;
                    events.removeFirst();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void finishCurrent() {
        currentEvent = null;
        update();
    }


    public static void load(MinecraftServer server) {
        var file = FabricLoader.getInstance().getConfigDir().resolve("event_schedule.json");
        if (Files.exists(file)) {
            try {
                var x = GSON.<List<EventInfo>>fromJson(Files.readString(file), TOKEN.getType());
                if (x != null) {
                    x.sort(Comparator.comparing(e -> e.start));
                    instance.currentEvent = null;
                    instance.events.clear();
                    instance.events.addAll(x);
                    instance.update();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            CompletableFuture.runAsync(() -> {
                try (HttpClient client = HttpClient.newHttpClient()) {
                    var x = client.send(HttpRequest.newBuilder().uri(URI.create("https://platform.modfest.net/event/bc25/schedule")).GET().build(), HttpResponse.BodyHandlers.ofString());
                    var y = GSON.<List<EventInfo>>fromJson(x.body(), TOKEN.getType());
                    if (y != null) {
                        y.sort(Comparator.comparing(e -> e.start));
                        instance.currentEvent = null;
                        instance.events.clear();
                        instance.events.addAll(y);
                        instance.update();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void save() {
        var file = FabricLoader.getInstance().getConfigDir().resolve("event_schedule.json");
        try {
            Files.writeString(file, GSON.toJson(instance));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void createCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
            literal("event_schedule")
                .requires(x -> x.hasPermissionLevel(3))
                .then(literal("skip").executes(ctx -> {
                    instance.finishCurrent();
                    return 0;
                }))
                    .then(literal("reload").executes(ctx -> {
                        load(ctx.getSource().getServer());
                        return 0;
                    }))
                    .then(literal("preview").executes(ctx -> {
                        if (instance.currentEvent != null) {
                            ctx.getSource().sendMessage(Text.literal("Current: " + instance.currentEvent));
                        }

                        for (var event : instance.events) {
                            ctx.getSource().sendMessage(Text.literal("Next: " + event));
                        }
                        return 0;
                    }))
        );
    }
}