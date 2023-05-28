package top.iseason.bukkit.itemseals.hook

import cz._heropwp.playerworldspro.api.API
import org.bukkit.entity.Player
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkittemplate.hook.BaseHook

object PWPHook : BaseHook("PlayerWorldsPro") {

    /**
     * @return true 需要封印 false 需要解封 null不操作
     */
    fun checkPlayerWorld(player: Player): Boolean? {
        if (!hasHooked) return null
        if (!Config.hooks__player_worlds_pro) return null
        val name = player.world.name
        val worldOwner = runCatching { API.getUUIDOfPlayerWorldOwner(name) }.getOrNull() ?: return null
        return !API.isMember(player, worldOwner)
    }

}