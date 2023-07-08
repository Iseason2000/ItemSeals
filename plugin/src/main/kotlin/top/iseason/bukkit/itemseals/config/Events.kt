package top.iseason.bukkit.itemseals.config

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.scheduler.BukkitTask
import top.iseason.bukkit.itemseals.ItemSeals
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.utils.bukkit.SchedulerUtils.submit

@FilePath("events.yml")
object Events : SimpleYAMLConfig() {
    @Key
    @Comment("", "本配置为封印触发事件", "可以在此决定什么时候进行封印/解封检查")
    var readme = ""

    @Key
    @Comment("", "在玩家切换世界时检查")
    var on_world_change = true

    @Key
    @Comment("", "在进入服务器时检查, 在config中配置延迟")
    var on_login = true

    @Key
    @Comment("", "在玩家点击物品时检查")
    var on_inventory_click = false

    @Key
    @Comment("", "定时扫描背包，单位tick, -1关闭")
    var on_scanner = -1L
    private var scanner: BukkitTask? = null

    override fun onLoaded(section: ConfigurationSection) {
        scanner?.cancel()
        scanner = null
        if (on_scanner > 0) {
            scanner = submit(async = true, delay = on_scanner, period = on_scanner) {
                for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                    ItemSeals.checkPlayerBags(onlinePlayer)
                }
            }
            info("&a定时扫描已开启，间隔 $on_scanner")
        }
    }
}