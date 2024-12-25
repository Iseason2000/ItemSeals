package top.iseason.bukkit.itemseals

import cc.bukkitPlugin.banitem.api.invGettor.CCInventory
import de.tr7zw.nbtapi.NBT

import org.bstats.bukkit.Metrics
import org.bukkit.block.BlockState
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import top.iseason.bukkit.itemseals.command.commands
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkit.itemseals.config.Events
import top.iseason.bukkit.itemseals.config.Lang
import top.iseason.bukkit.itemseals.hook.*
import top.iseason.bukkittemplate.BukkitPlugin
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.command.CommandHandler
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.hook.ItemsAdderHook
import top.iseason.bukkittemplate.hook.MMOItemsHook
import top.iseason.bukkittemplate.hook.OraxenHook
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.registerListener
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.getDisplayName
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toBase64
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toColorPapi
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min

@Suppress("UNUSED")
object ItemSeals : BukkitPlugin {

    override fun onEnable() {
        Config.load(false)
        SimpleYAMLConfig.notifyMessage = "&a配置文件 &7%s &a已重载"
        PWPHook.checkHooked()
        SakuraBindHook.checkHooked()
        PlayerDataSQLHook.checkHooked()
        BanItemHook.checkHooked()
        GermHook.checkHooked()
        ItemsAdderHook
        OraxenHook
        MMOItemsHook
        if (PlayerDataSQLHook.hasHooked) {
            PlayerDataSQLHook.registerListener()
        }

        Lang.load(false)
        Events.load(false)
        commands()
        CommandHandler.updateCommands()
        Listener.registerListener()
        Metrics(BukkitTemplate.getPlugin(), 18594)
        info("&a插件已启用! 作者: Iseason")
    }

    override fun onDisable() {
        info("&6插件已卸载!")
    }

    /**
     * 尝试封印物品
     * @return 被封印之后的物品，null 不封印
     */
    fun sealItem(item: ItemStack, player: Player, force: Boolean = false): Pair<ItemStack, Int>? {
        if (!force && !Config.isMatch(item, player)) {
            if (!Config.check_container_item) return null
            try {
                val itemMeta = item.itemMeta as? BlockStateMeta ?: return null
                val blockState = itemMeta.blockState as? InventoryHolder ?: return null
                val c = checkInv(blockState.inventory, player).first
                if (c > 0) {
                    itemMeta.blockState = blockState as BlockState
                    val clone = item.clone()
                    clone.itemMeta = itemMeta
                    return clone to c
                }
                return null
            } catch (_: RuntimeException) {
                return null
            }
        }
        val base64 = item.toBase64()
        val itemMeta = item.itemMeta!!
        val setting = Config.getSetting(item)
        val itemClone = Config.getSealedItemPattern(item).clone()
        SakuraBindHook.bind(player, itemClone, item)
        val itemStack = itemClone.applyMeta {
            val name = item.getDisplayName() ?: item.type.name
            if (hasDisplayName())
                setDisplayName(displayName.formatBy(name))
            val loreIndex = Config.getConfigOr(item, "seal-lore-index") { Config.seal_lore_index }
            if (itemMeta.hasLore() && loreIndex >= 0) {
                val lore = itemMeta.lore!!
                if (this.hasLore())
                    lore.addAll(min(loreIndex, lore.size - 1), this.lore!!.map { it.formatBy(name) })
                this.lore = lore
            }
            if (Config.getConfigOr(item, "highlight-sealed-item") { Config.highlight_sealed_item }) {
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, true)
            }
        }.toColorPapi(player)
        NBT.modify(itemStack) {
            it.setString(Config.seal_item_nbt, base64)
            it.setString("item_seals_unique_id", UUID.randomUUID().toString())
            if (setting != null)
                it.setString("item_seals_setting", setting)
        }
        return itemStack to 1
    }

    /**
     *
     * 尝试解封印物品
     * @return 被解封之后的物品，null 不解封
     */
    fun unSealItem(item: ItemStack): Pair<ItemStack, Int>? {
        val base64 = NBT.get<String>(item) {
            it.getString(Config.seal_item_nbt)
        }
        if (base64.isNullOrEmpty()) {
            if (!Config.check_container_item) return null
            try {
                val itemMeta = item.itemMeta as? BlockStateMeta ?: return null
                val blockState = itemMeta.blockState as? InventoryHolder ?: return null
                val c = unSealInv(blockState.inventory)
                if (c > 0) {
                    itemMeta.blockState = blockState as BlockState
                    val clone = item.clone()
                    clone.itemMeta = itemMeta
                    return clone to c
                }
                return null
            } catch (_: RuntimeException) {
                return null
            }
        }
        return ItemUtils.fromBase64ToItemStack(base64) to 1
    }

    /**
     * 封印背包, 返回封印的个数
     */
    fun sealInv(inv: Inventory, player: Player, force: Boolean = false): Int {
        var count = 0
        for (i in 0 until inv.size) {
            val item = inv.getItem(i)
            if (item.checkAir()) {
                continue
            }
            val itm = sealItem(item!!, player, force) ?: continue
            count += itm.second
            val im = itm.first
            debug("sealed item in ${inv.javaClass} for ${item.type}")
            inv.setItem(i, im)
        }
        return count
    }

    /**
     * 解封背包, 返回解封的个数
     */
    fun unSealInv(inv: Inventory): Int {
        var count = 0
        for (i in 0 until inv.size) {
            val item = inv.getItem(i)
            if (item.checkAir()) {
                continue
            }
            val itm = unSealItem(item!!) ?: continue
            count += itm.second
            inv.setItem(i, itm.first)
        }
        return count
    }

    fun checkInv(inv: Inventory, player: Player): Pair<Int, Int> {
        var scount = 0
        var ucount = 0
        for (i in 0 until inv.size) {
            val item = inv.getItem(i)
            if (item.checkAir()) {
                continue
            }
            val checkWorldSeal = checkWorldSeal(player, item!!) ?: continue
            if (checkWorldSeal && isSealedItem(item)) continue
            debug("物品：${item.type} 检查是否封印: $checkWorldSeal")
            val itm = if (checkWorldSeal) sealItem(item, player) else unSealItem(item)
            itm ?: continue
            if (checkWorldSeal) scount += itm.second else ucount += itm.second
            val im = itm.first
//            if (inv.getItem(i) === item) // 校验
            inv.setItem(i, im)
        }
        return scount to ucount
    }

    /**
     * @return true 需要封印 false 需要解封 null不操作
     */
    fun checkWorldSeal(player: Player): Boolean? {
        if (Config.permission_check) {
            if (player.hasPermission("itemseals.bypass")) return null
            if (player.hasPermission("itemseals.seal")) return true
            if (player.hasPermission("itemseals.unseal")) return false
        }
        val pwp = PWPHook.checkPlayerWorld(player)
        if (pwp != null) return pwp
        val name = player.world.name
        if (Config.black_list.contains(name)) return true
        if (Config.white_list.contains("all")) return false
        if (Config.white_list.contains(name)) return false
        return null
    }

    fun checkWorldSeal(player: Player, item: ItemStack): Boolean? {
        val setting = Config.getSetting(item)
        if (Config.permission_check) {
            if (player.hasPermission("itemseals.bypass")) return null
            if (player.hasPermission("itemseals.seal")) return true
            if (player.hasPermission("itemseals.unseal")) return false
            if (setting != null) {
                if (player.hasPermission("itemseals.$setting.bypass")) return null
                if (player.hasPermission("itemseals.$setting.seal")) return true
                if (player.hasPermission("itemseals.$setting.unseal")) return false
            }
        }
        val pwp = PWPHook.checkPlayerWorld(player, item)
        if (pwp != null) return pwp
        val name = player.world.name
        val reverse = Config.getConfigOr(item, "reverse-order") { Config.reverse_order }
        val pattern = Config.getConfigOr(item, "enable-world-name-pattern") { Config.enable_world_name_pattern }
        val black = Config.matcherWorldsBlack.getOrDefault(setting, Config.black_list)
        val white = Config.matcherWorldsWhite.getOrDefault(setting, Config.white_list)
        val first = if (reverse) white else black
        val second = if (reverse) black else white
        if (pattern) {
            if (first.any { Pattern.compile(it).matcher(name).find() }) {
                return !reverse
            }
            if (second.any { Pattern.compile(it).matcher(name).find() }) {
                return reverse
            }
            return null
        }
        if (first.contains(name) || first.contains("all")) {
            return !reverse
        }
        if (second.contains(name) || second.contains("all")) {
            return reverse
        }
        return null
    }

    /**
     * 封印或解封玩家背包物品
     */
    fun checkPlayerBags(player: Player) {
        val inventory = player.inventory
        var (scount, ucount) = checkInv(inventory, player)
        if (BanItemHook.hasHooked) {
            BanItemHook.getModInventories(player).forEach {
                val (s, u) = checkInv(it, player)
                if (it is CCInventory) it.onOpEnd()
                scount += s
                ucount += u
            }
        }
        if (GermHook.hasHooked) {
            val (s, u) = GermHook.checkInv(player)
            scount += s
            ucount += u
        }
        if (scount > 0) {
            player.sendColorMessage(Lang.seal_msg)
            debug("已为玩家 ${player.name} 封印 $scount 个物品")
        }
        if (ucount > 0) {
            player.sendColorMessage(Lang.un_seal_msg)
            debug("已为玩家 ${player.name} 解封 $ucount 物品")
        }
    }

    fun isSealedItem(item: ItemStack) = NBT.get<Boolean>(item) { it.hasTag(Config.seal_item_nbt) }
}