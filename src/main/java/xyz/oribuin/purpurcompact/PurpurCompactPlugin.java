package xyz.oribuin.purpurcompact;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class PurpurCompactPlugin extends JavaPlugin implements Listener {

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        if (this.getConfig().getBoolean("entity-portal-teleport")) {
            this.registerEvent(EntityPortalEvent.class, event -> {
                if (event.getFrom().getBlock().getType() == Material.END_PORTAL && event.getFrom().getWorld().getEnvironment() == World.Environment.THE_END) {
                    event.setCancelled(true);
                }
            });
        }

        if (this.getConfig().getBoolean("anvil-color")) {
            this.registerEvent(PrepareAnvilEvent.class, event -> {
                ItemStack result = event.getResult();
                if (result == null) return;

                if (event.getViewers().stream().noneMatch(viewer -> viewer.hasPermission("purpurcompact.anvil-color")))
                    return;

                // Use the legacy serializer to force the whole string to be italic
                result.editMeta(itemMeta -> itemMeta.displayName(serializer
                        .deserialize(itemMeta.getDisplayName())
                        .decorate(TextDecoration.ITALIC))
                );

                event.setResult(result);
            });
        }

        if (this.getConfig().getBoolean("anti-explode")) {

            // Stop the ender crystal from breaking blocks
            this.registerEvent(EntityExplodeEvent.class, event -> {
                if (event.getEntity().getType() == EntityType.ENDER_CRYSTAL)
                    event.setCancelled(true);
            });

            // Stop the ender crystal from damaging entities
            this.registerEvent(EntityDamageByEntityEvent.class, event -> {
                if (event.getDamager().getType() == EntityType.ENDER_CRYSTAL)
                    event.setCancelled(true);
            });

            // Stop the respawn anchor from damaging entities
            this.registerEvent(EntityDamageByBlockEvent.class, event -> {
                Block damager = event.getDamager();
                if (damager == null || damager.getType() != Material.RESPAWN_ANCHOR)
                    return;

                event.setCancelled(true);
            });

            // Stop the respawn anchor from exploding (just remove the block)
            this.registerEvent(PlayerInteractEvent.class, event -> {
                if (event.getClickedBlock() == null) return;
                if (event.getClickedBlock().getType() != Material.RESPAWN_ANCHOR) return;

                RespawnAnchor anchor = (RespawnAnchor) event.getClickedBlock().getBlockData();
                if (event.getClickedBlock().getWorld().getEnvironment() == World.Environment.NETHER)
                    return;

                if (anchor.getCharges() > 0) {

                    if (event.getItem() != null
                            && event.getItem().getType() == Material.GLOWSTONE
                            && anchor.getCharges() < anchor.getMaximumCharges()
                    ) return;

                    event.setCancelled(true);
                    event.getClickedBlock().setType(Material.AIR); // Remove the block
                }
            });
        }

    }

    /**
     * Register an event with a consumer
     *
     * @param eventClass The event class
     * @param consumer   The consumer
     * @param <T>        The event class
     */
    private <T extends Event> void registerEvent(Class<T> eventClass, Consumer<T> consumer) {
        Bukkit.getPluginManager().registerEvent(
                eventClass,
                this,
                EventPriority.NORMAL,
                (listener, event) -> {
                    if (eventClass.isAssignableFrom(event.getClass()))
                        consumer.accept(eventClass.cast(event));
                },
                this
        );
    }

}
