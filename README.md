# ItemSeals

> 很多mod物品都能够绕过插件的限制，那么只要把物品变成什么功能都没有的原版物品不就可以“封印”物品了。

## 插件特色

* 兼容 1.8+ 的任何服务端类型
* 根据配置按世界\权限等条件封印特定物品
* 自动重载配置
* 丰富的语言消息功能
* 在玩家切换世界时检查并封印/解封物品
* 在玩家点击物品时检查并封印/解封物品
* 在玩家登录之后延迟一定时间检查并封印/解封物品
* 世界名黑白名单
* 兼容 PlayerDataSQL 在玩家数据同步时检查并封印/解封物品
* 兼容 PlayerWorldsPro 在玩家有权限的家园解封物品，在没有权限的家园封印物品
* 兼容 SakuraBind 2.2.7+，绑定封印物品，基础被封印物品的绑定设置
* ....更多功能等你来提

## 插件动图

## 插件命令

主命令 itemseals 别名 is、iss ,全部为管理员命令

~~~ tex
 - seal <player>  封印玩家手上的物品
 - sealAll <player>  封印玩家背包的所有物品
 - checkSealAll <player>  封印玩家背包的所有符合规则的物品
 - unseal <player>  解封玩家手上的物品
 - unsealAll <player>  解封玩家背包的所有物品
 - reload  重载配置
 - debug  重载配置
~~~

## 插件配置

**config.yml**

~~~ yaml

# 黑名单，在此世界中将封印起来
black-list: []

# 白名单，在此世界中将解除封印
# all 表示 除了 家园世界(如果有的话) 和 黑名单中的世界 之外的所有世界
white-list:
- all

# 默认是在玩家切换世界时检查，此处为是否是异步检查, 如果存在问题请设置为false
async-check-on-world-change: true

# 玩家进入服务端之后会检查所处的世界进行封印、解封操作，此处为检查的延迟, 单位tick，-1不检查
login-check-delay: 40

# 被封印的物品将会变成这个材质, 注意不要与需要封印的物品一致
# 支持材质名:子id 例子：
# paper、PAPER、DIAMOND_SWORD、PAPER:2
material: paper

# 被封印的物品将会变成这个名字,{0}是原物品的自定义名字，没有会是材质名
seal-name: '&6[已封印] &f{0}'

# 被封印的物品将会插入这个lore
seal-lore:
- '&c由于世界限制，此物品已被封印'
- '&c前往不限制的世界将自动解封'

# 插入lore的位置，0表示最上面，一个足够大的值(999)可以插到最后面，-1直接覆盖原lore
seal-lore-index: 0

# 给被封印的物品加上附魔光效
highlight-sealed-item: true

# 原物品会存放在封印物品的nbt中，此为nbt名
seal-item-nbt: item_seals

# 插件兼容
hooks:
  
  # 兼容pwp，在玩家有权限的家园解除封印，其他家园封印
  player-worlds-pro: false
  
  # 兼容SakuraBind，绑定被封印的物品
  sakura-bind: false
  
  # SakuraBind绑定的配置，如果不存在则是按默认规则匹配, 如果留空且原物品是绑定物品可以继承它的配置
  sakura-bind-setting: ''
  
  # 在玩家数据同步完之后才进行世界检查
  player-data-sql: true

# 是否允许权限检查:
# itemseals.bypass 什么都不做
# itemseals.seal 封印物品
# itemseals.unseal 解封物品
permission-check: true

# 需要封印的物品材质
materials: []

~~~

**events.yml**

~~~ yaml

# 本配置为封印触发事件
# 可以在此决定什么时候进行封印/解封检查
readme: ''

# 在玩家切换世界时检查
on-world-change: true

# 在进入服务器时检查, 在config中配置延迟
on-login: true

# 在玩家点击物品时检查
on-inventory-click: false

~~~

**lang.yml**

~~~ yaml

# 消息留空将不会显示，使用 换行符 可以换行
# 支持 & 颜色符号，1.17以上支持16进制颜色代码，如 #66ccff
# {0}、{1}、{2}、{3} 等格式为该消息独有的变量占位符
# 所有消息支持PlaceHolderAPI
# 以下是一些特殊消息, 大小写不敏感，可以通过 多行 自由组合
# 以 [Broadcast] 开头将以广播的形式发送，支持BungeeCord
# 以 [Actionbar] 开头将发送ActionBar消息
# 以 [Main-Title] 开头将发送大标题消息
# 以 [Sub-Title] 开头将发送小标题消息
# 以 [Command] 开头将以消息接收者的身份运行命令
# 以 [Console] 开头将以控制台的身份运行命令
# 以 [OP-Command] 开头将赋予消息接收者临时op运行命令 (慎用)
readme: ''

# 系统消息设置
system:
  
  # 消息前缀
  msg-prefix: '&a[&6ItemSeals&a] &f'
  
  # 控制台消息前缀
  log-prefix: '&a[&6ItemSeals&a] &f'
  
  # 是否使用 MiniMessage 模式, 同时不支持&符号, 第一次开启将会自动下载依赖
  # 格式: https://docs.advntr.dev/minimessage/format.html
  # 网页可视化: https://webui.advntr.dev/
  mini-message: false
seal-msg: '&6由于世界限制，你的某些物品已被封印'
un-seal-msg: '&a你被封印的物品已解除封印'
on-click-seal-msg: '&6由于世界限制，该物品已被封印'
on-click-un-seal-msg: '&a该物品已解除封印'

~~~

