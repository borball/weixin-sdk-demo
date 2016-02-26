# weixin-sdk-demo

使用spring boot，基于weixin-sdk开发的web应用。

##如何使用？

1. 企业号：
    - 修改 wx-qy-settings-test.xml 文件, 根据您的情况修改corpId, corpSecret相关信息。
    - 修改application.properties 中默认的agent配置
	    - agent.default.token=xxxxxxxxx
	    - agent.default.aesKey=xxxxxxxxxxxxxxxxx

2. 公众号：
	- 修改wx-mp-settings-test.xml 文件, 根据您的情况修改appId, appSecret相关信息。

    
3. 如果需要修改应用服务器端口:
	- 修改文件：application.properties


4. 启动： Run:  com.riversoft.weixin.demo.Application



