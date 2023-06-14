package top.iseason.bukkit.itemseals

import cc.bukkitPlugin.banitem.api.invGettor.CCInventory
import io.github.bananapuncher714.nbteditor.NBTEditor
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
import top.iseason.bukkit.itemseals.hook.BanItemHook
import top.iseason.bukkit.itemseals.hook.PWPHook
import top.iseason.bukkit.itemseals.hook.PlayerDataSQLHook
import top.iseason.bukkit.itemseals.hook.SakuraBindHook
import top.iseason.bukkittemplate.BukkitPlugin
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.command.CommandHandler
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.debug.info
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
import kotlin.math.min

@Suppress("UNUSED")
object ItemSeals : BukkitPlugin {

    override fun onAsyncEnable() {
        SimpleYAMLConfig.notifyMessage = "&a配置文件 &7%s &a已重载"
        PWPHook.checkHooked()
        SakuraBindHook.checkHooked()
        PlayerDataSQLHook.checkHooked()
        BanItemHook.checkHooked()
        if (PlayerDataSQLHook.hasHooked) {
            PlayerDataSQLHook.registerListener()
        }
        Config.load(false)
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
            val itemMeta = item.itemMeta as? BlockStateMeta ?: return null
            val blockState = itemMeta.blockState as? InventoryHolder ?: return null
            val c = sealInv(blockState.inventory, player)
            if (c > 0) {
                itemMeta.blockState = blockState as BlockState
                val clone = item.clone()
                clone.itemMeta = itemMeta
                return clone to c
            }
            return null
        }
        val base64 = item.toBase64()
        val itemMeta = item.itemMeta!!
        val itemStack = Config.getSealedItemPattern(item).clone().applyMeta {
            val name = item.getDisplayName() ?: item.type.name
            if (hasDisplayName())
                setDisplayName(displayName.formatBy(name))
            if (itemMeta.hasLore() && Config.seal_lore_index >= 0) {
                val lore = itemMeta.lore!!
                lore.addAll(min(Config.seal_lore_index, lore.size - 1), this.lore!!.map { it.formatBy(name) })
                this.lore = lore
            }
            if (Config.highlight_sealed_item) {
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, true)
            }
        }.toColorPapi(player)
        SakuraBindHook.bind(player, itemStack, item)
        var set = NBTEditor.set(itemStack, base64, Config.seal_item_nbt)
        set = NBTEditor.set(set, UUID.randomUUID().toString(), "itemseals_unique_id")
        return set to 1
    }

    /**
     *
     * 尝试解封印物品
     * @return 被解封之后的物品，null 不解封
     */
    fun unSealItem(item: ItemStack): Pair<ItemStack, Int>? {
        val base64 = NBTEditor.getString(item, Config.seal_item_nbt)
        if (base64 == null) {
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
            var im = itm.first
            inv.setItem(i, im)
        }
        return count
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

    /**
     * 封印或解封玩家背包物品
     */
    fun checkPlayerBags(player: Player) {
        val checkWorldSeal = checkWorldSeal(player)
        debug("开始检查玩家 ${player.name} 的世界: ${checkWorldSeal}")
        checkWorldSeal ?: return
        val inventory = player.inventory
        var count = if (checkWorldSeal)
            sealInv(inventory, player)
        else unSealInv(inventory)
        if (BanItemHook.hasHooked) {
            count += BanItemHook.getModInventories(player).sumOf {
                val num = if (checkWorldSeal) sealInv(it, player)
                else unSealInv(it)
                if (it is CCInventory) it.onOpEnd()
                num
            }
        }
        if (count != 0) {
            if (checkWorldSeal) {
                player.sendColorMessage(Lang.seal_msg)
                debug("已为玩家 ${player.name} 封印 $count 个物品")
            } else {
                player.sendColorMessage(Lang.un_seal_msg)
                debug("已为玩家 ${player.name} 解封 $count 物品")
            }
        }
    }


    fun isSealedItem(item: ItemStack) = NBTEditor.contains(item, Config.seal_item_nbt)
}