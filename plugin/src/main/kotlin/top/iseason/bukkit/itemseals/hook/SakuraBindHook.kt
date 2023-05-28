package top.iseason.bukkit.itemseals.hook

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkit.sakurabind.SakuraBindAPI
import top.iseason.bukkittemplate.hook.BaseHook

object SakuraBindHook : BaseHook("SakuraBind") {

    fun bind(player: Player, item: ItemStack, raw: ItemStack) {
        if (!hasHooked || !Config.hooks__sakura_bind) return
        if (Config.hooks__sakura_bind_setting.isBlank() && SakuraBindAPI.hasBind(raw)) {
            val itemSetting = SakuraBindAPI.getItemSetting(raw)
            SakuraBindAPI.bind(item, player, setting = itemSetting, showLore = Config.seal_lore_index < 0)
        } else {
            SakuraBindAPI.bind(item, player, setting = SakuraBindAPI.getSetting(Config.hooks__sakura_bind_setting))
        }
    }

}