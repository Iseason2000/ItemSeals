package top.iseason.bukkit.itemseals.command

import cc.bukkitPlugin.banitem.api.invGettor.CCInventory
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.itemseals.ItemSeals
import top.iseason.bukkit.itemseals.config.Config
import top.iseason.bukkit.itemseals.config.Lang
import top.iseason.bukkit.itemseals.hook.BanItemHook
import top.iseason.bukkit.itemseals.hook.GermHook
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

internal fun commands() {
    command("itemseals") {
        alias = arrayOf("is", "iss")
        default = PermissionDefault.OP
        node("seal") {
            description = "封印玩家手上的物品"
            default = PermissionDefault.OP
            param("<player>", suggestRuntime = ParamSuggestCache.playerParam)
            executor { params, sender ->
                val player = params.next<Player>()
                val heldItem = player.getHeldItem() ?: throw ParmaException("该玩家手上没有物品")
                val sealItem = ItemSeals.sealItem(heldItem, player, true)!!.first
                heldItem.type = sealItem.type
                heldItem.itemMeta = sealItem.itemMeta
                heldItem.amount = 1
                sender.sendColorMessage("&a已封印玩家 &6${player.name} &a手上的物品")
            }
        }
        node("sealAll") {
            description = "封印玩家背包的所有物品"
            default = PermissionDefault.OP
            param("<player>", suggestRuntime = ParamSuggestCache.playerParam)
            executor { params, sender ->
                val player = params.next<Player>()
                var count = ItemSeals.sealInv(player.inventory, player, true)
                if (BanItemHook.hasHooked)
                    count += BanItemHook.getModInventories(player).sumOf {
                        val num = ItemSeals.sealInv(it, player, true)
                        if (it is CCInventory) it.onOpEnd()
                        num
                    }
                if (GermHook.hasHooked) count += GermHook.checkInv(player, true).first
                if (count > 0) sender.sendColorMessage("&a已封印玩家 &6${player.name} &a背包里的 &7${count} &a个物品")
                else sender.sendColorMessage("&6玩家背包是空的")
            }
        }
        node("checkSealAll") {
            description = "封印玩家背包的所有符合规则的物品"
            default = PermissionDefault.OP
            param("<player>", suggestRuntime = ParamSuggestCache.playerParam)
            executor { params, sender ->
                val player = params.next<Player>()
                var count = ItemSeals.sealInv(player.inventory, player)
                if (BanItemHook.hasHooked)
                    count += BanItemHook.getModInventories(player).sumOf {
                        val num = ItemSeals.sealInv(it, player)
                        if (it is CCInventory) it.onOpEnd()
                        num
                    }
                if (GermHook.hasHooked) count += GermHook.checkInv(player).first
                if (count > 0) sender.sendColorMessage("&a已封印玩家 &6${player.name} &a背包里的 &7${count} &a个物品")
                else sender.sendColorMessage("&6玩家背包是空的")
            }
        }
        node("unseal") {
            description = "解封玩家手上的物品"
            default = PermissionDefault.OP
            param("<player>", suggestRuntime = ParamSuggestCache.playerParam)
            executor { params, sender ->
                val player = params.next<Player>()
                val heldItem = player.getHeldItem() ?: throw ParmaException("该玩家手上没有物品")
                val sealItem = ItemSeals.unSealItem(heldItem)?.first ?: throw ParmaException("玩家手上的物品没有封印!")
                heldItem.type = sealItem.type
                heldItem.itemMeta = sealItem.itemMeta
                heldItem.amount = 1
                sender.sendColorMessage("已解封玩家 ${player.name} 手上的物品")
            }
        }

        node("unsealAll") {
            description = "解封玩家背包的所有物品"
            default = PermissionDefault.OP
            param("<player>", suggestRuntime = ParamSuggestCache.playerParam)
            executor { params, sender ->
                val player = params.next<Player>()
                var count = ItemSeals.unSealInv(player.inventory)
                if (BanItemHook.hasHooked)
                    count += BanItemHook.getModInventories(player).sumOf {
                        val num = ItemSeals.unSealInv(it)
                        if (it is CCInventory) it.onOpEnd()
                        num
                    }
                if (GermHook.hasHooked) count += GermHook.checkInv(player, false).second
                if (count > 0) sender.sendColorMessage("&a已解封玩家 &6${player.name} &a背包里的 &7${count} &a个物品")
                else sender.sendColorMessage("&6没有封印的物品")
            }
        }
        node("reload") {
            description = "重载配置"
            default = PermissionDefault.OP
            executor { _, _ ->
                Config.load()
                Lang.load()
            }
        }
        node("debug") {
            description = "重载配置"
            default = PermissionDefault.OP
            executor { _, sender ->
                SimpleLogger.isDebug = !SimpleLogger.isDebug
                sender.sendColorMessage("Debug模式: ${SimpleLogger.isDebug}")
            }
        }
    }
}