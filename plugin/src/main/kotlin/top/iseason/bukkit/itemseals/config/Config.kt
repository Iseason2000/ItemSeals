package top.iseason.bukkit.itemseals.config

import com.google.common.cache.CacheBuilder
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.ItemSeals
import top.iseason.bukkit.itemseals.config.matcher.BaseMatcher
import top.iseason.bukkit.itemseals.config.matcher.MatcherManager
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.warn
import java.util.concurrent.TimeUnit


@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Key
    @Comment("", "黑名单，在此世界中将封印起来")
    var black_list = hashSetOf<String>()

    @Key
    @Comment(
        "", "白名单，在此世界中将解除封印",
        "all 表示 除了 家园世界(如果有的话) 和 黑名单中的世界 之外的所有世界"
    )
    var white_list = hashSetOf("all")

    @Key
    @Comment("", "默认是在玩家切换世界时检查，此处为是否是异步检查, 如果存在问题请设置为false")
    var async_check_on_world_change = true

    @Key
    @Comment("", "玩家进入服务端之后会检查所处的世界进行封印、解封操作，此处为检查的延迟, 单位tick，-1不检查")
    var login_check_delay = 40L

    @Key
    @Comment(
        "", "被封印的物品将会变成这个材质, 注意不要与需要封印的物品一致",
        "支持材质名:子id 例子：",
        "paper、PAPER、DIAMOND_SWORD、PAPER:2"
    )
    var material = "paper"

    @Key
    @Comment("", "被封印的物品将会变成这个名字,{0}是原物品的自定义名字，没有会是材质名")
    var seal_name = "&6[已封印] &f{0}"

    @Key
    @Comment("", "被封印的物品将会插入这个lore")
    var seal_lore = listOf("&c由于世界限制，此物品已被封印", "&c前往不限制的世界将自动解封")

    @Key
    @Comment("", "插入lore的位置，0表示最上面，一个足够大的值(999)可以插到最后面，-1直接覆盖原lore")
    var seal_lore_index = 0

    @Key
    @Comment("", "给被封印的物品加上附魔光效")
    var highlight_sealed_item = true

    @Key
    @Comment("", "原物品会存放在封印物品的nbt中，此为nbt名")
    var seal_item_nbt = "item_seals"

    @Key
    @Comment("", "插件兼容")
    var hooks: MemorySection? = null

    @Key
    @Comment("", "兼容pwp，在玩家有权限的家园解除封印，其他家园封印")
    var hooks__player_worlds_pro = false

    @Key
    @Comment("", "兼容SakuraBind，绑定被封印的物品")
    var hooks__sakura_bind = false

    @Key
    @Comment("", "SakuraBind绑定的配置，如果不存在则是按默认规则匹配, 如果留空且原物品是绑定物品可以继承它的配置")
    var hooks__sakura_bind_setting = ""

    @Key
    @Comment("", "在玩家数据同步完之后才进行世界检查")
    var hooks__player_data_sql = true

    @Key
    @Comment(
        "", "是否允许权限检查:",
        "itemseals.bypass 什么都不做",
        "itemseals.seal 封印物品",
        "itemseals.unseal 解封物品",
        "itemseals.[matcher].bypass 不封印特定matcher的物品",
    )
    var permission_check = true

    @Key
    @Comment(
        "",
        "需要封印的物品材质匹配器, 格式如下",
        "name 为 物品名字, 必须为非原版翻译名(也就是从创造物品栏拿出来的'圆石'的name为空)",
        "name-without-color 为 除去颜色代码的物品名字, 必须为非原版翻译名(也就是从创造物品栏拿出来的'圆石'的name为空)",
        "material 为 物品材质,使用正则匹配",
        "materials 为 物品材质,使用全名匹配 https://bukkit.windit.net/javadoc/org/bukkit/Material.html",
        "ids 为 物品id:子id 匹配方式 如 6578 或 6578:2",
        "materialIds 为 物品材质:子ID 匹配方式 如 STONE 或 STONE:2 ; 如果只需要匹配材质请使用效率更高的 materials 方式",
        "lore 为 物品lore正则匹配 如有多行则需全匹配, lore! 表示删除匹配到的lore",
        "lore-without-color 为 物品lore除去颜色代码正则匹配 如有多行则需全匹配 与 lore 互斥, lore-without-color! 表示删除匹配到的lore",
        "nbt 为 物品NBT，注意：常规的nbt储存在tag下",
        "注：以上的 name 和 name-without-color 互斥，material、materials、ids、materialIds 互斥，",
        "注：lore、lore-without-color 及其!后缀互斥。 互斥就是只能同时存在其中一个",
        "所有条件取交集",
        "example 是一个例子，example作为该匹配器的组名，可用于权限控制"
    )
    var item_matchers: MemorySection = YamlConfiguration().apply {
        createSection("example").apply {
            set("name", "^这是.*的物品$")
            set("material", "BOW|BOOKSHELF")
            set("materials", listOf("DIAMOND_SWORD"))
            set("materialId", listOf("SPECIAL:2"))
            set("ids", listOf("6578", "2233:2"))
            set("lore", listOf("绑定物品", "属于"))
            set("nbt.tag.testnbt", ".*")
        }
    }
    private var matchers = LinkedHashMap<String, List<BaseMatcher>>()

    private val cache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(10, TimeUnit.SECONDS)
        .build<ItemStack, String>()

    private const val NOT_MATCH = "z6R08ED6hvMeEnTQUvnYxQO5FQI81ucd6HA7xAj6B1yX8c7yQjRyok2xWKSlJLL1"

    fun getSetting(itemStack: ItemStack): String? {
        val key = cache.get(itemStack) {
            matchers.entries.find { m -> m.value.all { it.tryMatch(itemStack) } }?.key ?: NOT_MATCH
        }
        return if (key == NOT_MATCH) null
        else key
    }

    /**
     * true 是需要封印的物品
     * false 不是需要封印的物品
     */
    fun isMatch(itemStack: ItemStack, player: Player): Boolean {
        val setting = getSetting(itemStack) ?: return false
        if (permission_check) {
            if (player.hasPermission("itemseals.$setting.bypass")) return false
        }
        return true
    }

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
        matchers.clear()
        cache.cleanUp()
        item_matchers.getKeys(false).forEach {
            try {
                val parseSection = MatcherManager.parseSection(item_matchers.getConfigurationSection(it)!!)
                matchers[it] = parseSection
            } catch (e: Exception) {
                warn("匹配器配置错误：$it")
            }
        }
    }

}