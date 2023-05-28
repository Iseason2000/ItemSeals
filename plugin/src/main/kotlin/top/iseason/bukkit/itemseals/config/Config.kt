package top.iseason.bukkit.itemseals.config

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.ItemSeals
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.warn
import java.util.*

@Key
@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Comment("", "黑名单，在此世界中将封印起来")
    var black_list = hashSetOf<String>()

    @Comment(
        "", "白名单，在此世界中将解除封印",
        "all 表示 除了 家园世界(如果有的话) 和 黑名单中的世界 之外的所有世界"
    )
    var white_list = hashSetOf("all")

    @Comment("", "默认是在玩家切换世界时检查，此处为是否是异步检查, 如果存在问题请设置为false")
    var async_check_on_world_change = true

    @Comment("", "玩家进入服务端之后会检查所处的世界进行封印、解封操作，此处为检查的延迟, 单位tick，-1不检查")
    var login_check_delay = 40L

    @Comment(
        "", "被封印的物品将会变成这个材质, 注意不要与需要封印的物品一致",
        "支持材质名:子id 例子：",
        "paper、PAPER、DIAMOND_SWORD、PAPER:2"
    )
    var material = "paper"

    @Comment("", "被封印的物品将会变成这个名字,{0}是原物品的自定义名字，没有会是材质名")
    var seal_name = "&6[已封印] &f{0}"

    @Comment("", "被封印的物品将会插入这个lore")
    var seal_lore = listOf("&c由于世界限制，此物品已被封印", "&c前往不限制的世界将自动解封")

    @Comment("", "插入lore的位置，0表示最上面，一个足够大的值(999)可以插到最后面，-1直接覆盖原lore")
    var seal_lore_index = 0

    @Comment("", "给被封印的物品加上附魔光效")
    var highlight_sealed_item = true

    @Comment("", "原物品会存放在封印物品的nbt中，此为nbt名")
    var seal_item_nbt = "item_seals"

    @Comment("", "插件兼容")
    var hooks: MemorySection? = null

    @Comment("", "兼容pwp，在玩家有权限的家园解除封印，其他家园封印")
    var hooks__player_worlds_pro = false

    @Comment("", "兼容SakuraBind，绑定被封印的物品")
    var hooks__sakura_bind = false

    @Comment("", "SakuraBind绑定的配置，如果不存在则是按默认规则匹配, 如果留空且原物品是绑定物品可以继承它的配置")
    var hooks__sakura_bind_setting = ""

    @Comment("", "在玩家数据同步完之后才进行世界检查")
    var hooks__player_data_sql = true

    @Comment(
        "", "是否允许权限检查:",
        "itemseals.bypass 什么都不做",
        "itemseals.seal 封印物品",
        "itemseals.unseal 解封物品",
    )
    var permission_check = true

    @Comment("", "需要封印的物品材质")
    var materials: EnumSet<Material> = EnumSet.noneOf(Material::class.java)

    override fun onLoaded(section: ConfigurationSection) {
        val split = material.split(':')
        val mat = split[0]
        val matchMaterial = Material.matchMaterial(mat)
        if (matchMaterial == null) {
            warn("无效的材质: $mat")
            return
        }
        val itemStack = ItemStack(matchMaterial)
        if (split.size > 1) {
            val toByte = runCatching { split[1].toByte() }.getOrElse {
                warn("无法设置子ID:${split[1]}")
                return
            }
            val data = itemStack.data ?: return
            data.data = toByte
        }
        ItemSeals.pattern = itemStack
    }

}