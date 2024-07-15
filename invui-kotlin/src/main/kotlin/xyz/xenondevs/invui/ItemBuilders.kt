@file:Suppress("PackageDirectoryMismatch")

package xyz.xenondevs.invui.item.builder

import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.inventoryaccess.component.BungeeComponentWrapper
import xyz.xenondevs.invui.item.ItemBuilder

/**
 * Sets the lore of the item stack.
 */
fun ItemBuilder.setLore(lore: List<Array<BaseComponent>>): ItemBuilder = setLore(lore.map { BungeeComponentWrapper(it) })