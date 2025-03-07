@file:Suppress("PackageDirectoryMismatch")

package xyz.xenondevs.invui.item.builder

import net.kyori.adventure.text.Component
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper
import xyz.xenondevs.invui.item.ItemBuilder

/**
 * Sets the lore the item stack.
 */
fun ItemBuilder.setLore(lore: List<Component>): ItemBuilder = setLore(lore)