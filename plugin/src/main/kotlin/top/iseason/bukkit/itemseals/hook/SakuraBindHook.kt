package top.iseason.bukkit.itemseals.hook

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkittemplate.hook.BaseHook

object SakuraBindHook : BaseHook("SakuraBind") {

    fun bind(player: Player, item: ItemStack, raw: ItemStack) {
        if (!hasHooked || !Config.getConfigOr(item, "hooks.sakura-bind") { Config.hooks__sakura_bind }) return
        val setting = Config.getConfigOr(item, "hooks.sakura-bind-setting") { Config.hooks__sakura_bind_setting }
        if (setting.isBlank() && SakuraBindAPI.hasBind(raw)) {
            val itemSetting = SakuraBindAPI.getItemSetting(raw)
            SakuraBindAPI.bind(
                item,
                player,
                setting = itemSetting,
                showLore = Config.getConfigOr(item, "seal-lore-index") { Config.seal_lore_index } < 0)
        } else {
            SakuraBindAPI.bind(item, player, setting = SakuraBindAPI.getSetting(Config.hooks__sakura_bind_setting))
        }
    }

}