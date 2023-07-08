package top.iseason.bukkit.itemseals.hook

import cz._heropwp.playerworldspro.Main
import cz._heropwp.playerworldspro.api.API
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkittemplate.hook.BaseHook

object PWPHook : BaseHook("PlayerWorldsPro") {
    private val main = API::class.java.getDeclaredField("main").apply {
        isAccessible = true
    }.get(null) as Main

    // pwp自己有bug读取不到uuid
    private val isSameJar = try {
        main.B().a("")
        true
    } catch (e: Throwable) {
        false
    }

    /**
     * @return true 需要封印 false 需要解封 null不操作
     */
    fun checkPlayerWorld(player: Player): Boolean? {
        if (!hasHooked) return null
        if (!Config.hooks__player_worlds_pro) return null
        val name = player.world.name
        val worldOwner = if (isSameJar)
            main.B().a(name) ?: return null
        else
            runCatching { API.getUUIDOfPlayerWorldOwner(name) }.getOrNull() ?: return null
        return !API.isMember(player, worldOwner)
    }

    fun checkPlayerWorld(player: Player, item: ItemStack): Boolean? {
        if (!hasHooked) return null
        if (!Config.getConfigOr(item, "hooks.player-worlds-pro") { Config.hooks__player_worlds_pro }) return null
        val name = player.world.name
        val worldOwner = if (isSameJar)
            main.B().a(name) ?: return null
        else
            runCatching { API.getUUIDOfPlayerWorldOwner(name) }.getOrNull() ?: return null
        return !API.isMember(player, worldOwner)
    }

}