package xyz.xenondevs.invui.item.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.AbstractWindow;
import xyz.xenondevs.invui.window.Window;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract implementation of the {@link Item} interface.
 */
public abstract class AbstractItem implements Item {
    
    private final Set<AbstractWindow> windows = new HashSet<>();
    
    @Override
    public void addWindow(AbstractWindow window) {
        windows.add(window);
    }
    
    @Override
    public void removeWindow(AbstractWindow window) {
        windows.remove(window);
    }
    
    @Override
    public Set<Window> getWindows() {
        return Collections.unmodifiableSet(windows);
    }
    
    @Override
    public void notifyWindows() {
        windows.forEach(w -> w.handleItemProviderUpdate(this));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {}
}
