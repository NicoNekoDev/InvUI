@file:Suppress("PackageDirectoryMismatch")

package xyz.xenondevs.invui.item.builder

import net.kyori.adventure.text.Component
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper
import xyz.xenondevs.invui.item.ItemBuilder

/**
 * Sets the display name of the item stack.
 */
fun ItemBuilder.setDisplayName(displayName: Component): ItemBuilder = setDisplayName(AdventureComponentWrapper(displayName))

/**
 * Sets the lore the item stack.
 */
fun ItemBuilder.setLore(lore: List<Component>): ItemBuilder = setLore(lore.map { AdventureComponentWrapper(it) })

/**
 * Adds lore lines to the item stack.
 */
fun ItemBuilder.addLoreLines(vararg components: Component): ItemBuilder = addLoreLines(*components.map { AdventureComponentWrapper(it) }.toTypedArray())