package top.iseason.bukkit.itemseals.hook

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.ReflectionUtil
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

object PWPHook : BaseHook("PlayerWorldsPro") {

    private val apiClazz: Class<*>? =
        runCatching { Class.forName("cz._heropwp.playerworldspro.api.API") }
            .recoverCatching { Class.forName("cz.heroify.playerworldspro.api.API") }
            .getOrNull()

    private val getWorldOwnerMethod: MethodHandle? = runCatching {
        ReflectionUtil.getStaticMethod(
            apiClazz,
            "getUUIDOfPlayerWorldOwner",
            MethodType.methodType(String::class.java, String::class.java)
        )
    }.getOrNull()

    private val isMemberMethod: MethodHandle? = runCatching {
        ReflectionUtil.getStaticMethod(
            apiClazz,
            "isMember",
            MethodType.methodType(Boolean::class.java, Player::class.java, String::class.java)
        )
    }.getOrNull()

    fun getOwner(worldName: String): String? = try {
        getWorldOwnerMethod?.invoke(worldName) as? String
    } catch (_: Exception) {
        null
    }

    fun isMember(player: Player, worldOwner: String): Boolean? =
        isMemberMethod?.invoke(player, worldOwner) as? Boolean

    /**
     * @return true 需要封印 false 需要解封 null不操作
     */
    fun checkPlayerWorld(player: Player): Boolean? {
        if (!hasHooked) return null
        if (!Config.hooks__player_worlds_pro) return null
        val name = player.world.name
        val worldOwner = getOwner(name) ?: return null
        val member = isMember(player, worldOwner) ?: return null
        return !member
    }

    fun checkPlayerWorld(player: Player, item: ItemStack): Boolean? {
        if (!hasHooked) return null
        if (!Config.getConfigOr(item, "hooks.player-worlds-pro") { Config.hooks__player_worlds_pro }) return null
        val name = player.world.name
        val worldOwner = getOwner(name) ?: return null
        val member = isMember(player, worldOwner) ?: return null
        return !member
    }

}