package top.iseason.bukkit.itemseals.hook

import cc.bukkitPlugin.pds.events.PlayerDataLoadCompleteEvent
import org.bukkit.event.EventHandler
import top.iseason.bukkit.itemseals.ItemSeals
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.other.submit


object PlayerDataSQLHook : BaseHook("PlayerDataSQL"), org.bukkit.event.Listener {

    @EventHandler
    fun onDataLoad(event: PlayerDataLoadCompleteEvent) {
        if (Config.login_check_delay < 0 || !Config.hooks__player_data_sql) return
        val player = event.player
        submit(async = true, delay = Config.login_check_delay) {
            if (player.isOnline) ItemSeals.checkPlayerBags(player)
        }
    }
}