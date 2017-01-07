layout: post
title: '[最新版]Cmder中文环境使用小结'
date: 2017-01-07 11:47:37
categories: tools
---
### Cmder 优势

* 支持 Ctrl-C Ctrl-V 复制粘贴
* 支持部分 Linux 命令
* 支持从 console 打开 Explorer 进入当前文件路径
* 自带 vim 工具

以上对于用惯了 Linux 命令行的人来说，使用 Windows 会变得顺手许多。

### 下载及安装
**下载地址**
使用 full version 还是 mini version 看个人需求，我个人建议 mini 就够了。
[Github 传送门](https://github.com/cmderdev/cmder/releases/)

**安装**
Cmder 不需要 wizard 安装，配置环境变量就可以。
具体步骤：
* 解压 e.g. C:\ProgramData\cmder_mini
* 设置环境变量
> 我的电脑 -> 属性 -> 高级系统设置 -> 环境变量
> 在[系统变量]下新建：变量名-CmderPath; 变量值-C:\ProgramData\cmder_mini
> 在[系统变量]下找到 Path 变量，在末尾添加 ";%CmderPath%"
> 保存
* 测试 Path 是否生效
> 快捷键 Win-R 运行 cmder，弹出 Cmder 对话框视为生效

### 配置自定义环境
Cmder 原生配置对中文的支持不好，中文显示会出现重叠或乱码。
* 在 Cmder 中点击 Settings
* [Main] 设置中不勾选 Monospace
* [Startup-Tasks] 设置中可以选择其他命令行工具 [Specified named Tasks]，例如 Windows 自带 Powershell
* 如果使用 Powershell 不存在中文编码问题
* 使用 Cmder 自带工具，则需要在 [Startup-Environment] 设置中，添加 set LANG=zh_CN.UTF-8 解决中文乱码

*以下是 Bonus*
告诉我不是我一人觉得 Cmder 的 Lambda λ 巨丑...
* 打开 vendor\clink.lua 文件
* 找到以下代码并将对应 lambda 默认符号改成 $。
```
local cmder_prompt = "\x1b[1;32;40m{cwd} {git}{hg} \n\x1b[1;30;40m{lamb} \x1b[0m"
cmder_prompt = string.gsub(cmder_prompt, "{cwd}", cwd)
if env == nil then
lambda = "$"
else
lambda = "("..env..") $"
end
clink.prompt.value = string.gsub(cmder_prompt, "{lamb}", lambda)
```


愿世界和平。