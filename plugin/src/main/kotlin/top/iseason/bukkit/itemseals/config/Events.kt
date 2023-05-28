package top.iseason.bukkit.itemseals.config

import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key

@Key
@FilePath("events.yml")
object Events : SimpleYAMLConfig() {
    @Comment("", "本配置为封印触发事件", "可以在此决定什么时候进行封印/解封检查")
    var readme = ""

    @Comment("", "在玩家切换世界时检查")
    var on_world_change = true

    @Comment("", "在进入服务器时检查, 在config中配置延迟")
    var on_login = true

    @Comment("", "在玩家点击物品时检查")
    var on_inventory_click = false
}