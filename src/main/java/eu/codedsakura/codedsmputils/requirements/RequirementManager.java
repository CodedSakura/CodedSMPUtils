package eu.codedsakura.codedsmputils.requirements;

import eu.codedsakura.codedsmputils.config.requirements.Items;
import eu.codedsakura.codedsmputils.config.requirements.Requirements;
import eu.codedsakura.codedsmputils.requirements.fulfillables.FAdvancement;
import eu.codedsakura.codedsmputils.requirements.fulfillables.Fulfillable;
import eu.codedsakura.codedsmputils.requirements.fulfillables.StaticItem;
import eu.codedsakura.codedsmputils.requirements.fulfillables.StaticXP;
import eu.codedsakura.common.ExperienceUtils;
import net.minecraft.advancement.Advancement;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.L;
import static eu.codedsakura.codedsmputils.CodedSMPUtils.logger;

public class RequirementManager {
    private final ServerPlayerEntity player;
    private final Relation relation;

    public final List<StaticItem> haveItems;
    public final List<StaticItem> consumeItems;
    public final StaticXP xp;
    public final List<FAdvancement> advancements;

    public RequirementManager(Requirements req, ServerPlayerEntity player, Relation relation, Map<String, ?> variables) {
        this.player = player;
        this.relation = relation;

        ArrayList<StaticItem> items = new ArrayList<>();
        for (Items item : req.items) {
            items.add(new StaticItem(item.consume.getValue(variables), item.name, item.count.getValue(variables)));
        }

        haveItems = items.stream()
                .filter(i -> !i.consume)
                .collect(Collectors.groupingBy(i -> i.item))
                .entrySet().stream()
                .map(entry ->
                        new StaticItem(false, entry.getKey(),
                                entry.getValue().stream().mapToInt(si -> si.count).sum()))
                .collect(Collectors.toList());
        consumeItems = items.stream()
                .filter(i -> i.consume)
                .collect(Collectors.groupingBy(i -> i.item))
                .entrySet().stream()
                .map(entry ->
                        new StaticItem(true, entry.getKey(),
                                entry.getValue().stream().mapToInt(si -> si.count).sum()))
                .collect(Collectors.toList());

        xp = new StaticXP(req.experience.consume.getValue(variables), req.experience.getData(variables));

        Map<Identifier, Advancement> advancementList = player.server.getAdvancementLoader()
                .getAdvancements().stream()
                .collect(Collectors.toMap(Advancement::getId, Function.identity()));

        advancements = req.advancements.stream()
                .map(advancement -> new Identifier(advancement.name))
                .map(identifier -> new FAdvancement(advancementList.get(identifier)))
                .collect(Collectors.toList());
    }

    /**
     *
     * @return true if requirements can be satisfied, false otherwise
     * @throws RequirementException in extremely unlikely chances
     */
    public boolean hasEnough() {
        HashMap<Item, Integer> itemCounts = new HashMap<>();
        Stream.of(player.getInventory().main, player.getInventory().armor, player.getInventory().offHand)
                .flatMap(List::stream)
                .forEach(itemStack -> itemCounts.put(
                        itemStack.getItem(),
                        itemCounts.getOrDefault(itemStack.getItem(), 0) + itemStack.getCount()));

        haveItems.forEach(item -> {
            item.checked = true;
            item.fulfilled = itemCounts.containsKey(item.item) && itemCounts.get(item.item) >= item.count;
        });
        consumeItems.forEach(item -> {
            item.checked = true;
            item.fulfilled = itemCounts.containsKey(item.item) && itemCounts.get(item.item) >= item.count;
        });


        xp.checked = true;
        switch (xp.type) {
            case LEVELS:
                xp.fulfilled = player.experienceLevel >= xp.value;
                break;
            case POINTS:
                xp.fulfilled = ExperienceUtils.levelToTotalPoints(player.experienceLevel) +
                        (long) (player.experienceProgress * player.getNextLevelExperience()) >= xp.value;
                break;
        }

        advancements.forEach(fAdvancement -> {
            fAdvancement.checked = true;
            fAdvancement.fulfilled = player.getAdvancementTracker().getProgress(fAdvancement.advancement).isDone();
        });


        List<Fulfillable> fulfillables = getAll();
        if (fulfillables.stream().anyMatch(v -> !v.checked)) throw new RequirementException("Something not checked!");

        switch (relation) {
            case AND:
                return fulfillables.stream().allMatch(v -> v.fulfilled);
            case OR:
                return fulfillables.stream().anyMatch(v -> v.fulfilled);
        }
        throw new RequirementException("Unknown relation!");
    }

    /**
     *
     * @return false if there's ambiguity between item and XP removal, true otherwise
     */
    public boolean consume() {
        if (relation == Relation.OR) {
            if (Stream.of(advancements, haveItems)
                    .flatMap(Collection::stream)
                    .anyMatch(fulfillable -> fulfillable.fulfilled)) {
                return true;
            }

            long possible = consumeItems.stream().filter(f -> f.fulfilled).count() +
                    (xp.fulfilled && xp.consume ? 1 : 0);
            if (possible > 1)
                return false;
        }

        consumeItems.forEach(staticItem -> {
            player.getInventory().remove(
                    itemStack -> itemStack.getItem().equals(staticItem.item),
                    staticItem.count, player.playerScreenHandler.getCraftingInput());
            staticItem.consumed = true;
        });

        if (xp.consume) {
            xp.removeFromPlayer(player);
            xp.consumed = true;
        }
        return true;
    }

    // record of sanity checks: 5
    public boolean consumeSpecific(String type, String value) {
        if (consume()) return true;

        switch (type) {
            case "item":
                List<StaticItem> options = consumeItems.stream().filter(item -> item.getOriginalValue().equals(value))
                        .collect(Collectors.toList());
                if (options.size() > 1) {
                    player.sendMessage(L.get("base.error"), false);
                    logger.error("[CSMPU] [/back | step 2] repeated item '{}'", value);
                    return false;
                }

                if (options.size() < 1) {
                    player.sendMessage(L.get("base.error"), false);
                    logger.error("[CSMPU] [/back | step 2] missing item definition '{}'", value);
                    return false;
                }

                StaticItem item = options.get(0);
                player.getInventory().remove(
                        itemStack -> itemStack.getItem().equals(item.item),
                        item.count, player.playerScreenHandler.getCraftingInput());
                item.consumed = true;
                break;

            case "xp":
                if (!xp.consume) {
                    player.sendMessage(L.get("base.error"), false);
                    logger.error("[CSMPU] [/back | step 2] xp not supposed to be consumed");
                    return false;
                }

                if (!value.equalsIgnoreCase(xp.getOriginalValue())) {
                    player.sendMessage(L.get("base.error"), false);
                    logger.error("[CSMPU] [/back | step 2] mismatched xp values '{}' and '{}'", value, xp.getOriginalValue());
                    return false;
                }

                xp.removeFromPlayer(player);
                xp.consumed = true;
                break;

            default:
                player.sendMessage(L.get("base.error"), false);
                logger.error("[CSMPU] [/back | step 2] unknown type '{}'", type);
                return false;
        }
        return true;
    }

    private Stream<Fulfillable> getAllStream() {
        return Stream.concat(
                Stream.of(haveItems, consumeItems, advancements)
                        .flatMap(Collection::stream),
                Stream.of(xp)
        );
    }

    public List<Fulfillable> getAll() {
        return getAllStream().collect(Collectors.toList());
    }

    public List<Fulfillable> getFulfilled() {
        return getAllStream().filter(f -> f.fulfilled).collect(Collectors.toList());
    }

    public List<Fulfillable> getUnfulfilled() {
        return getAllStream().filter(f -> !f.fulfilled).collect(Collectors.toList());
    }

    public List<Fulfillable> getConsumed() {
        return getAllStream().filter(f -> f.consumed).collect(Collectors.toList());
    }

    public static void verifyRequirements(Requirements req, MinecraftServer server) {
        for (Items item : req.items) {
            Identifier id = Identifier.tryParse(item.name);
            if (id == null)
                throw new RequirementException("'" + item.name + "' is not a valid id!");

            if (!Registry.ITEM.getOrEmpty(id).isPresent())
                throw new RequirementException("Item with id '" + item.name + "' not found!");
        }

        Collection<Advancement> advancementList = server.getAdvancementLoader().getAdvancements();
        for (eu.codedsakura.codedsmputils.config.requirements.Advancement advancement : req.advancements) {
            if (advancementList.stream().noneMatch(a -> a.getId().toString().equals(advancement.name)))
                throw new RequirementException("Advancement with id '" + advancement.name + "' not found!");
        }
    }
}
