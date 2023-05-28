package top.iseason.bukkit.itemseals.config

import top.iseason.bukkittemplate.config.Lang
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key

@Key
@FilePath("lang.yml")
object Lang : Lang() {
    var seal_msg = "&6由于世界限制，你的某些物品已被封印"
    var un_seal_msg = "&a你被封印的物品已解除封印"
    var on_click_seal_msg = "&6由于世界限制，该物品已被封印"
    var on_click_un_seal_msg = "&a该物品已解除封印"
}