package top.iseason.bukkit.itemseals.config

import com.google.common.cache.CacheBuilder
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.itemseals.config.matcher.BaseMatcher
import top.iseason.bukkit.itemseals.config.matcher.MatcherManager
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.item
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toSection
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
        "", "被封印的物品将会变成这个物品（全局）占位符 {0} 会被替换成原物品的名字",
        "格式: https://github.com/Iseason2000/BukkitTemplate/wiki/%E7%89%A9%E5%93%81%E5%BA%8F%E5%88%97%E5%8C%96",
    )
    var sealed_item: MemorySection = Material.PAPER.item.applyMeta {
        setDisplayName("&6[已封印] &f{0}")
        lore = listOf("&c由于世界限制，此物品已被封印", "&c前往不限制的世界将自动解封")
    }.toSection()

    private var globalItem = ItemStack(Material.PAPER)

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
    @Comment("", "兼容萌芽槽扫描")
    var hooks__germ = false

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
        "需要封印的物品材质匹配器, match 选项格式如下 ",
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
        "example 是一个例子，example作为该匹配器的组名，可用于权限控制",
        "在匹配器里可以覆盖全局设置, 支持的键如下",
        "black-list、white-list、sealed-item、seal-lore-index、highlight-sealed-item",
        "hooks.player-worlds-pro、hooks.sakura-bind、hooks.sakura-bind-setting、hooks.germ"
    )
    var item_matchers: MemorySection = YamlConfiguration().apply {
        createSection("example").apply {
            set("match.name", "^这是.*的物品$")
            set("match.material", "BOW|BOOKSHELF")
            set("match.materials", listOf("DIAMOND_SWORD"))
            set("match.materialId", listOf("SPECIAL:2"))
            set("match.ids", listOf("6578", "2233:2"))
            set("match.lore", listOf("绑定物品", "属于"))
            set("match.nbt.tag.testnbt", ".*")
            set("sealed-item", ItemStack(Material.PAPER).toSection())
        }
    }
    private var matchers = LinkedHashMap<String, List<BaseMatcher>>()
    private var matcherItems = LinkedHashMap<String, ItemStack>()
    var matcherWorldsBlack = LinkedHashMap<String, Set<String>>()
        private set
    var matcherWorldsWhite = LinkedHashMap<String, Set<String>>()
        private set
    var matcherSections = LinkedHashMap<String, ConfigurationSection>()
        private set

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

    fun getSealedItemPattern(itemStack: ItemStack): ItemStack {
        val key = getSetting(itemStack) ?: return globalItem
        return matcherItems[key] ?: globalItem
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

    fun <T> getConfigOr(itemStack: ItemStack, key: String, default: () -> T): T {
        val setting = getSetting(itemStack) ?: return default()
        val section = matcherSections[setting] ?: return default()
        if (!section.contains(key)) {
            return default()
        }
        return section.get(key) as T
    }


    override fun onLoaded(section: ConfigurationSection) {
        val fromSection = ItemUtils.fromSection(sealed_item)
        if (fromSection == null) {
            warn("无效的材质")
        } else {
            globalItem = fromSection
            info("&a新的封印材质为 ${globalItem.type}")
        }
        matcherItems.clear()
        matcherWorldsBlack.clear()
        matcherWorldsWhite.clear()
        matcherSections.clear()
        matchers.clear()
        cache.cleanUp()
        item_matchers.getKeys(false).forEach {
            try {
                val parseSection = MatcherManager
                    .parseSection(item_matchers.getConfigurationSection("${it}.match")!!)
                matchers[it] = parseSection
                matcherSections[it] = item_matchers.getConfigurationSection(it)!!
                val black = item_matchers.getStringList("${it}.black-list")
                if (black.isNotEmpty()) matcherWorldsBlack[it] = black.toHashSet()
                val white = item_matchers.getStringList("${it}.white-list")
                if (white.isNotEmpty()) matcherWorldsWhite[it] = white.toHashSet()
                val itemSection = item_matchers.getConfigurationSection("${it}.sealed-item")
                if (itemSection != null) matcherItems[it] = ItemUtils.fromSection(itemSection) ?: return
            } catch (e: Exception) {
                warn("匹配器配置错误：$it")
            }
        }
    }

}