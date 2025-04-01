https://baidu.com
click images/百度/登录_按钮_1.png
dclick images/百度/登录_按钮_1.png
rclick images/百度/登录_按钮_1.png
hover images/百度/登录_按钮_1.png
mouse down
mouse up
wait 4
timeout 5
if present("images/百度/用户名_输入框_1.png")
    type images/百度/用户名_输入框_1.png as admin
type images/百度/用户名_输入框_1.png as admin
if present("images/百度/登录_按钮_1.png")
    echo 登录按钮已出现
https://txhy.qq.com
click images/腾讯会议/启动会议_按钮_2.jpg
if present("images/腾讯会议/下载腾讯会议_按钮_1.jpg")
    click images/腾讯会议/下载腾讯会议_按钮_1.jpg
wait 2
if present("images/腾讯会议/使用非中国大陆地区手机号码请前往下载 V_标签_3.jpg")
    echo 提示信息已出现
if present("images/腾讯会议/用户名_输入框_1.jpg")
    type images/百度/用户名_输入框_1.png as adminadmin
