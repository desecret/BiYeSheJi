# WebController API 文档

本文档描述了自动化测试系统的 RESTful API 接口。所有接口均以 `/api` 为基础路径。

## 测试用例管理

### 获取所有测试用例列表

- **URL**: `/api/testcases`
- **方法**: GET
- **描述**: 扫描测试用例目录下的所有.txt文件，返回测试用例ID和名称
- **响应示例**:
```json
[
  {
    "id": "login.txt",
    "name": "login"
  },
  {
    "id": "search.txt",
    "name": "search"
  }
]
```

### 获取测试用例详细内容

- **URL**: `/api/testcases/{id}`
- **方法**: GET
- **参数**:
  - `id`: 测试用例ID（文件名）
- **描述**: 读取指定测试用例文件并解析其中的命令步骤，返回结构化的步骤列表
- **响应示例**:
```json
{
  "steps": [
    {
      "type": "点击",
      "content": "登录按钮"
    },
    {
      "type": "在",
      "content": "用户名输入框中输入admin"
    }
  ],
  "success": true
}
```

### 保存测试用例

- **URL**: `/api/testcases`
- **方法**: POST
- **请求体**:
```json
{
  "name": "login",
  "steps": [
    {
      "type": "点击",
      "content": "登录按钮"
    },
    {
      "type": "在",
      "content": "用户名输入框中输入admin"
    }
  ]
}
```
- **描述**: 将前端提交的测试步骤保存为标准格式的测试用例文件

## 元素库管理

### 获取所有UI元素

- **URL**: `/api/elements`
- **方法**: GET
- **描述**: 从YAML配置文件中读取元素映射配置，返回所有UI元素的列表
- **响应示例**:
```json
[
  {
    "context": "login",
    "name": "username",
    "cssSelector": "#username",
    "xpath": "//input[@id='username']",
    "locatorType": "css",
    "imagePath": "images/login/username.jpg"
  }
]
```

### 按分组获取UI元素

- **URL**: `/api/elements/grouped`
- **方法**: GET
- **描述**: 获取按context分组的元素列表
- **响应示例**:
```json
{
  "login": [
    {
      "context": "login",
      "name": "username",
      "cssSelector": "#username",
      "xpath": "//input[@id='username']",
      "locatorType": "css",
      "imagePath": "images/login/username.jpg"
    }
  ],
  "dashboard": [
    {
      "context": "dashboard",
      "name": "logout",
      "cssSelector": ".logout-btn",
      "xpath": "//button[@class='logout-btn']",
      "locatorType": "css",
      "imagePath": "images/dashboard/logout.jpg"
    }
  ]
}
```

### 获取元素图片映射

- **URL**: `/api/elements/images`
- **方法**: GET
- **描述**: 返回每个元素ID和其对应的图片URL或文件路径的映射关系
- **响应示例**:
```json
{
  "login/username": "images/login/username.jpg",
  "dashboard/logout": "images/dashboard/logout.jpg"
}
```

### 更新元素信息

- **URL**: `/api/elements/update`
- **方法**: POST
- **请求体**:
```json
{
  "originalContext": "login",
  "originalName": "username",
  "context": "login",
  "name": "username_field",
  "cssSelector": "#login-username",
  "xpath": "//input[@id='login-username']"
}
```
- **描述**: 接收前端提交的元素更新信息，更新YAML配置文件中的元素定义
- **响应示例**:
```json
{
  "success": true
}
```

### 删除元素

- **URL**: `/api/elements/delete`
- **方法**: POST
- **请求体**:
```json
{
  "context": "login",
  "name": "username"
}
```
- **描述**: 从YAML配置文件中删除指定元素
- **响应示例**:
```json
{
  "success": true
}
```

### 获取元素图片

- **URL**: `/api/element/image`
- **方法**: GET
- **参数**:
  - `path`: 图片路径
- **描述**: 根据请求参数返回指定元素的图片资源
- **响应**: 图片资源文件

### 更新元素图片

- **URL**: `/api/elements/update-image`
- **方法**: POST
- **参数**:
  - `image`: 上传的图片文件
  - `context`: 元素上下文
  - `name`: 元素名称
- **描述**: 接收上传的图片文件，更新指定元素的图片路径
- **响应示例**:
```json
{
  "success": true
}
```

### 创建新元素

- **URL**: `/api/elements/create`
- **方法**: POST
- **请求体**:
```json
{
  "context": "login",
  "name": "password",
  "cssSelector": "#password",
  "xpath": "//input[@id='password']",
  "locatorType": "css"
}
```
- **描述**: 将新元素添加到YAML配置文件中
- **响应示例**:
```json
{
  "success": true
}
```

## 测试执行和图像处理

### 获取可用动作类型

- **URL**: `/api/actions`
- **方法**: GET
- **描述**: 获取所有可用的动作类型列表
- **响应示例**:
```json
[
  "点击",
  "右键点击",
  "双击",
  "鼠标移动到",
  "在当前鼠标位置，按下鼠标左键",
  "在当前鼠标位置，弹起鼠标左键",
  "等待",
  "设置自动等待",
  "在",
  "访问",
  "出现",
  "存在",
  "输出",
  "如果"
]
```

### 执行测试用例

- **URL**: `/api/run`
- **方法**: POST
- **请求体**:
```json
{
  "steps": [
    {
      "type": "点击",
      "content": "登录按钮"
    },
    {
      "type": "在",
      "content": "用户名输入框中输入admin"
    }
  ]
}
```
- **描述**: 接收前端提交的测试步骤，将其转换为标准命令格式并执行，返回执行结果日志
- **响应示例**:
```json
{
  "logs": [
    "执行命令: [点击登录按钮, 在用户名输入框中输入admin]"
  ]
}
```

### 上传图片处理

- **URL**: `/api/upload/image`
- **方法**: POST
- **参数**:
  - `image`: 上传的图片文件
- **描述**: 接收上传的图片，调用图像处理服务识别图片中的UI元素，更新元素库配置文件，并返回处理结果
- **响应示例**:
```json
{
  "success": true
}
```
```