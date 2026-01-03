// 重新导出一个简化的 Select 实现，因为完全复刻 Radix UI 比较复杂
// 这里我们提供一个 SimpleSelect 作为替代，或者使用上面的组件进行模拟
// 为了让代码跑起来，我们先提供上面那套模拟组件，但在实际交互中，SelectContent 可能需要通过 v-if 控制显示
// 这里我们修改一下 Select.vue 让它能够工作

import Select from './Select.vue'
import SelectTrigger from './SelectTrigger.vue'
import SelectValue from './SelectValue.vue'
import SelectContent from './SelectContent.vue'
import SelectItem from './SelectItem.vue'

export { Select, SelectTrigger, SelectValue, SelectContent, SelectItem }
