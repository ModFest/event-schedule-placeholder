package net.modfest.eventschedule;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class ModInit implements ModInitializer {
    @Override
    public void onInitialize() {
        Placeholders.register(Identifier.of("modfest", "event"), ModInit::eventInfo);
        Placeholders.register(Identifier.of("modfest", "event_current"), ModInit::eventCurrent);

        CommandRegistrationCallback.EVENT.register(EventSchedule::createCommands);
        ServerLifecycleEvents.SERVER_STARTING.register(EventSchedule::load);
        ServerTickEvents.END_SERVER_TICK.register(x -> EventSchedule.instance.update());
    }

    private static PlaceholderResult eventInfo(PlaceholderContext placeholderContext, String s) {
        int id = 0;
        try {
            id = Integer.parseInt(s);
        } catch (Throwable e) {
        }

        var type = EventSchedule.instance.get(id);

        if (type == null) {
            return PlaceholderResult.value(Text.literal("--- No Events! ---").formatted(Formatting.YELLOW));
        } else {
            return PlaceholderResult.value(Text.empty()
                    .append(Text.literal(type.name).formatted(Formatting.GOLD))
                    .append(Text.literal(" [").formatted(Formatting.GRAY))
                    .append(Text.literal(getDurationText(type.start)).formatted(Formatting.YELLOW))
                    .append(Text.literal("]").formatted(Formatting.GRAY))
            );
        }
    }

    private static PlaceholderResult eventCurrent(PlaceholderContext placeholderContext, String s) {
        var type = EventSchedule.instance.currentEvent;

        if (type == null) {
            return PlaceholderResult.value(Text.literal("--- No Event! ---").formatted(Formatting.YELLOW));
        } else {
            var next = EventSchedule.instance.get(0);
            return PlaceholderResult.value(Text.empty()
                    .append(Text.literal(type.name).formatted(Formatting.BLUE))
                    .append(Text.literal(" [").formatted(Formatting.GRAY))
                    .append(Text.literal(next == null ? " +- Forever -+ " : getDurationText(type.end)).formatted(Formatting.AQUA))
                    .append(Text.literal("]").formatted(Formatting.GRAY))
            );
        }
    }

    public static String getDurationText(Instant instant) {
        long x = Math.abs(System.currentTimeMillis() / 1000 - instant.getEpochSecond());

        long seconds = x % 60;
        long minutes = (x / 60) % 60;
        if (seconds > 5) {
            minutes += 1;
        }
        long hours = (x / (60 * 60));

        if (hours == 1) {
            hours = 0;
            minutes += 60;
        }

        StringBuilder builder = new StringBuilder();

        if (hours > 0) {
            builder.append(hours).append(" hours");
        } else {
            builder.append(minutes).append(" minutes");
        }
        return builder.toString();
    }
}
