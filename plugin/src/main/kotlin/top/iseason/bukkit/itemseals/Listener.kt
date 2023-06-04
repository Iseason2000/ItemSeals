package top.iseason.bukkit.itemseals

import com.google.common.cache.CacheBuilder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkit.itemseals.config.Events
import top.iseason.bukkit.itemseals.config.Lang
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.submit
import java.util.concurrent.TimeUnit

object Listener : org.bukkit.event.Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        if (!Events.on_world_change) return
        if (!Config.async_check_on_world_change) {
            ItemSeals.checkPlayerBags(event.player)
            return
        }
        submit(async = true) {
            ItemSeals.checkPlayerBags(event.player)
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onLogin(event: PlayerLoginEvent) {
        if (!Events.on_login || Config.login_check_delay < 0 || Config.hooks__player_data_sql) return
        val player = event.player
        submit(async = true, delay = Config.login_check_delay) {
            if (player.isOnline) ItemSeals.checkPlayerBags(player)
        }
    }

    private val cache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(5, TimeUnit.SECONDS)
        .build<ClickCache, Boolean>()

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onInvClick(event: InventoryClickEvent) {
        if (!Events.on_inventory_click) return
        val whoClicked = event.whoClicked as Player
        val checkWorldSeal = ItemSeals.checkWorldSeal(whoClicked) ?: return
        val item = event.currentItem
        if (item.checkAir()) return
        cache.get(ClickCache(whoClicked, item!!)) {
            if (checkWorldSeal) {
                val sealItem = ItemSeals.sealItem(item, whoClicked) ?: return@get false
                item.type = sealItem.first.type
                item.itemMeta = sealItem.first.itemMeta
                item.amount = 1
                whoClicked.sendColorMessage(Lang.on_click_seal_msg)
                event.isCancelled = true
                return@get true
            } else {
                val sealItem = ItemSeals.unSealItem(item) ?: return@get false
                item.type = sealItem.first.type
                item.itemMeta = sealItem.first.itemMeta
                item.amount = 1
                whoClicked.sendColorMessage(Lang.on_click_un_seal_msg)
                event.isCancelled = true
                return@get true
            }
        }
    }

    private class ClickCache(val player: Player, val itemStack: ItemStack) {
        override fun hashCode(): Int {
            var hash = 1
            hash = hash * 31 + player.hashCode()
            hash = hash * 31 + itemStack.hashCode()
            return hash
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is ClickCache) return false
            return other.player == player && other.itemStack == itemStack
        }
    }
}