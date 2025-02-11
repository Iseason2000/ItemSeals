package top.iseason.bukkit.itemseals.hook

import com.germ.germplugin.api.GermSlotAPI
import org.bukkit.entity.Player
import top.iseason.bukkit.itemseals.ItemSeals
import top.iseason.bukkit.itemseals.ItemSeals.isSealedItem
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir

object GermHook : BaseHook("GermPlugin") {

    fun checkInv(player: Player, force: Boolean? = null): Pair<Int, Int> {
        if (hasHooked) return 0 to 0
        val allGermSlotIdentity = GermSlotAPI.getAllGermSlotIdentity()
        var scount = 0
        var ucount = 0
        for (s in allGermSlotIdentity) {
            val itemStack = GermSlotAPI.getItemStackFromIdentity(player, s)
            if (itemStack.checkAir()) continue
            if (!Config.getConfigOr(itemStack, "hooks.germ") { Config.hooks__germ }) {
                continue
            }
            val checkWorldSeal = force ?: ItemSeals.checkWorldSeal(player, itemStack) ?: continue
            val sealedItem = isSealedItem(itemStack)
            debug { "物品：${itemStack.type} 检查是否封印: $checkWorldSeal 是否已经封印$sealedItem" }
            if (checkWorldSeal && sealedItem) continue
            if (!checkWorldSeal && !sealedItem) continue
            val (itm, sc) = if (checkWorldSeal) {
                ItemSeals.sealItem(itemStack, player) ?: continue
            } else {
                ItemSeals.unSealItem(itemStack) ?: continue
            }
            if (checkWorldSeal) scount += sc
            else ucount += sc
            GermSlotAPI.saveItemStackToIdentity(player, s, itm)
        }
        return scount to ucount
    }
}