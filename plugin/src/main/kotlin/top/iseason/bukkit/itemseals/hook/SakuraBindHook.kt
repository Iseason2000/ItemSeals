package top.iseason.bukkit.itemseals.hook

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkittemplate.hook.BaseHook

object SakuraBindHook : BaseHook("SakuraBind") {

    fun bind(player: Player, item: ItemStack, raw: ItemStack) {
        if (!hasHooked) return
        val setting = Config.getSetting(item)
        if (!Config.getConfigOr(setting, "hooks.sakura-bind") { Config.hooks__sakura_bind }) return

        val bindSetting = Config.getConfigOr(setting, "hooks.sakura-bind-setting") { Config.hooks__sakura_bind_setting }
        val hasBind = SakuraBindAPI.hasBind(raw)
        val replace = Config.getConfigOr(setting, "seal-lore-index") { Config.seal_lore_index } < 0
        if (bindSetting.isBlank()) {
            SakuraBindAPI.bind(
                item,
                player,
                setting = SakuraBindAPI.getItemSetting(raw),
                showLore = replace || !hasBind
            )
        } else {
            SakuraBindAPI.bind(
                item, player,
                setting = SakuraBindAPI.getSetting(bindSetting),
                showLore = replace || !hasBind
            )
        }
    }

}