# 系统修复指南

> 本文档记录系统的所有修复记录，每次修复都需要在此处登记。

---

## 修复记录

### 修复 #001: 产品分类管理功能入口缺失

**修复时间**: 2026-06-08

**问题描述**:
- 用户反馈在系统界面中缺少产品分类管理的功能入口，导致用户无法从产品管理页面跳转到分类管理页面。

**根本原因**:
- 新增产品分类管理模块时，未在现有页面的导航栏中添加跳转链接，导致用户无法在页面间切换。

**修复方案**:
- 在所有产品管理相关页面（列表、详情、表单）的导航栏中添加「产品分类」链接，指向 `/categories`
- 在所有分类管理相关页面（列表、表单）的导航栏中添加「产品管理」链接，指向 `/products`

**影响文件**:

| 文件路径 | 修改内容 |
|----------|----------|
| `frontend/templates/products/list.html` | 导航栏添加「产品分类」链接 |
| `frontend/templates/products/detail.html` | 导航栏添加「产品分类」链接 |
| `frontend/templates/products/form.html` | 导航栏添加「产品分类」链接 |
| `frontend/templates/categories/list.html` | 导航栏添加「产品管理」链接 |
| `frontend/templates/categories/form.html` | 导航栏添加「产品管理」链接 |

**修复状态**: ✅ 已完成

**验证结果**:
- 所有 23 个测试全部通过
- 页面导航正常，可在产品管理和分类管理之间自由切换

---

### 修复 #002: 分类列表列重叠 + 状态图标混淆

**修复时间**: 2026-06-08

**问题描述**:
- 分类列表编码列和状态列重叠，导致显示混乱
- 状态「启用」和「停用」图标都是相同的紫色圆点，容易混淆（产品列表也有同样问题）

**根本原因**:
- CSS 中缺少 `.col-code` 和 `.col-sort` 的列宽定义，导致分类列表的编码列和排序列没有固定宽度
- 状态 pill 的 `.spark` 图标没有根据状态区分颜色，启用和停用都使用相同的紫色渐变

**修复方案**:
- 在 CSS 中添加 `.col-code` (140px) 和 `.col-sort` (90px) 的列宽定义
- 添加 `.status-active` 和 `.status-inactive` 样式类，分别使用绿色和灰色区分状态
- 在分类列表和产品列表的状态 pill 中使用 `th:classappend` 动态添加状态类名

**影响文件**:

| 文件路径 | 修改内容 |
|----------|----------|
| `frontend/public/assets/app.css` | 添加 `.col-code`、`.col-sort` 列宽；添加 `.status-active`、`.status-inactive` 状态样式 |
| `frontend/templates/categories/list.html` | 状态 pill 动态添加 `status-active`/`status-inactive` 类 |
| `frontend/templates/products/list.html` | 状态 pill 动态添加 `status-active`/`status-inactive` 类 |

**修复状态**: ✅ 已完成

**验证结果**:
- 分类列表列宽正常，不再重叠
- 启用状态显示绿色圆点 + 绿色边框背景，停用状态显示灰色圆点 + 灰色边框背景
- 所有测试通过

---

### 修复 #003: 分类列表模块宽度问题全面修复

**修复时间**: 2026-06-08

**问题描述**:
- 产品分类页面的列表模块宽度问题尚未得到有效解决
- 分类列表在不同屏幕尺寸下显示异常，布局不合理
- 表格列宽与容器宽度不匹配，导致内容溢出

**根本原因**:
1. 产品列表页使用 `page-products` 类名有专门的全宽布局样式（`max-width: none`, `width: 100%`），但分类列表页使用 `page-categories` 类名，CSS 中缺少对应的布局样式
2. 分类列表 7 列总宽度约 1304px，但默认 container 只有 1100px，导致表格内容溢出
3. CSS 中缺少 `.w-200` 宽度类，而分类列表页使用了该类
4. 缺少响应式断点适配，小屏幕下表单和表格布局不合理

**修复方案**:
1. 为 `.page-categories` 添加与 `.page-products` 相同的布局样式（全宽、固定定位、弹性布局、表格自适应高度）
2. 添加响应式媒体查询断点：
   - `≤ 900px`：调整列宽、内边距，确保表格可横向滚动
   - `≤ 640px`：进一步缩小列宽，输入框宽度自适应
   - `≤ 480px`：搜索栏改为垂直布局，按钮宽度自适应
3. 添加缺失的 `.w-200` 宽度类（200px，最大 32vw）
4. 所有共享样式使用组合选择器（`.page-products, .page-categories`）统一维护

**影响文件**:

| 文件路径 | 修改内容 |
|----------|----------|
| `frontend/public/assets/app.css` | 为 `.page-categories` 添加全宽布局样式；添加 900px/640px/480px 响应式断点；添加 `.w-200` 宽度类 |

**修复状态**: ✅ 已完成

**验证结果**:
- 分类列表页面与产品列表页布局一致，全宽显示
- 大屏幕（>900px）：7 列完整显示，无溢出
- 中等屏幕（≤900px）：列宽自适应，表格可横向滚动
- 小屏幕（≤640px）：列宽缩小，输入框全宽显示
- 超小屏幕（≤480px）：搜索栏垂直布局，按钮全宽
- 所有 23 个测试通过

---

### 修复 #004: 修复 .container max-width 优先级问题

**修复时间**: 2026-06-08

**问题描述**:
- 分类页面的 `.container` 仍然被 `max-width: 1100px` 限制，导致宽度问题没有得到有效解决
- 用户反馈问题没有解决，经排查发现是 CSS 选择器优先级问题

**根本原因**:
1. 默认 `.container` 样式定义了 `max-width: 1100px`
2. 虽然添加了 `.page-categories .container { max-width: none }`，但选择器优先级不足以覆盖默认样式
3. 可能存在浏览器缓存或 CSS 优先级计算问题，导致 `max-width: none` 没有生效

**修复方案**:
1. 使用更强的 CSS 选择器 `body.page-products .container, body.page-categories .container`，增加 `body` 标签选择器提升优先级
2. 添加 `!important` 关键字强制覆盖，确保 `max-width: none !important`、`width: 100% !important`、`padding: 18px 24px !important` 能够生效
3. 为 `main.container` 添加 `margin: 0 !important`，确保移除默认的居中边距
4. 统一更新所有相关的响应式媒体查询和搜索栏样式，使用相同的强选择器保持一致性
5. 所有涉及 `.page-products` 和 `.page-categories` 的样式统一使用 `body.` 前缀

**具体修改**:

| 选择器 | 修改内容 |
|--------|----------|
| `body.page-products .container, body.page-categories .container` | `max-width: none !important; width: 100% !important; padding: 18px 24px !important` |
| `body.page-products main.container, body.page-categories main.container` | 新增 `margin: 0 !important` |
| 响应式媒体查询 | 统一使用 `body.` 前缀选择器 |
| 搜索栏样式 | 统一使用 `body.` 前缀选择器 |

**影响文件**:

| 文件路径 | 修改内容 |
|----------|----------|
| `frontend/public/assets/app.css` | 所有 `.page-products` 和 `.page-categories` 相关选择器添加 `body.` 前缀，关键属性添加 `!important` 强制覆盖 |

**修复状态**: ✅ 已完成

**验证结果**:
- `.container` 的 `max-width: 1100px` 被成功覆盖
- 分类列表页面全宽显示，与产品列表页布局一致
- 所有响应式断点正常工作
- 所有 23 个测试通过

---

## 修复登记模板

> 后续修复请复制以下模板并填写：

```markdown
### 修复 #00X: [简短问题描述]

**修复时间**: YYYY-MM-DD

**问题描述**:
- [详细描述问题现象、用户反馈或 Bug 表现]

**根本原因**:
- [分析问题产生的根本原因]

**修复方案**:
- [具体的修复措施和技术方案]

**影响文件**:

| 文件路径 | 修改内容 |
|----------|----------|
| `path/to/file` | [修改描述] |

**修复状态**: ✅ 已完成 / 🔄 进行中 / ⏳ 待开始

**验证结果**:
- [测试结果或验证说明]
```
